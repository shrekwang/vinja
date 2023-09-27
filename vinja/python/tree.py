import zipfile, os  
import shutil
import sys
import logging
import traceback
import fnmatch
import json
from common import ZipUtil,FileUtil,VimUtil,PathUtil
from xml.etree.ElementTree import *
from jde import ProjectManager,EditUtil

class TreeNode(object):
    mark_postfix = " [mark]"
    edit_postfix = " [edit]"
    error_postfix = " [error]"

    def __init__(self, name, realpath, isDirectory, isOpen = False, isLoaded = False):
        self.name=name
        self.realpath = realpath
        self.isDirectory = isDirectory
        self.isOpen = isOpen
        self.isLoaded = isLoaded
        self._children = []
        self.parent = None
        self.isMarked = False
        self.isEdited = False
        self.isError = False

    def get_display_str(self):
        dis_str = self.name
        if self.isError :
            dis_str = self.name + TreeNode.error_postfix
        elif self.isMarked :
            dis_str = self.name + TreeNode.mark_postfix
        elif self.isEdited :
            dis_str = self.name + TreeNode.edit_postfix
        return dis_str

    def get_child(self,name):
        for node in self._children :
            if node.name == name :
                return node
        return None

    def get_children(self) :
        return self._children

    def get_next_sibling(self):
        if self.parent  == None :
            return None
        for index,node in  enumerate(self.parent._children) :
            if node ==  self  and index < len(self.parent._children) -1 :
                return self.parent._children[index+1]

    def get_prev_sibling(self):
        if self.parent  == None :
            return None
        for index,node in  enumerate(self.parent._children) :
            if node ==  self  and index > 0: 
                return self.parent._children[index-1]

    def add_child(self,child):
        self._children.append(child)
        child.parent = self

    def insert_child(self,child):
        self._children.insert(0,child)
        child.parent = self

    def remove_child(self, child):
        self._children.remove(child)
        return

    def renderToString(self,depth, drawText, vertMap, isLastChild):
        treeParts = ''
        if depth > 1 :
            for j in vertMap[0:-1] :
                if j == 1 :
                    treeParts = treeParts + "| "
                else :
                    treeParts = treeParts + "  "
        if isLastChild :
            treeParts = treeParts + "`"
        else :
            treeParts = treeParts + "|"

        if self.isDirectory :
            if self.isOpen :
                treeParts = treeParts + "~"
            else :
                treeParts = treeParts + "+"
        else :
            treeParts = treeParts + "-"

        if depth == 0 :
            treeParts = self.name + "/" + "\n"
        else :
            if self.isDirectory :
                mark = ""
                if self.isMarked :
                    mark = TreeNode.mark_postfix
                treeParts = treeParts + self.name + "/" + mark + "\n"
            else :
                treeParts = treeParts + self.get_display_str() + "\n"

        if self.isDirectory and self.isOpen :
            childNodes = self.get_children()
            if len(childNodes) > 0 :
                lastIndex = len(childNodes) -1 
                if lastIndex > 0 :
                    for i in range(0,lastIndex):
                        tmpMap = vertMap[:]
                        tmpMap.append(1)
                        treeParts = treeParts + childNodes[i].renderToString(depth+1, 1, tmpMap, 0)
                tmpMap = vertMap[:]
                tmpMap.append(0)
                treeParts = treeParts + childNodes[lastIndex].renderToString(depth+1, 1, tmpMap, 1)
        return treeParts

    def add_sub_node(self, name):
        print("unsupported operation.")
        return False

    def dispose(self):
        print("unsupported operation.")
        return False

    def refresh(self):
        "there's nothing to do here"
        return

    def paste(self, file_names):
        print("unsupported operation.")
        return False

    def toggle_mark(self):
        if self.isMarked :
            self.isMarked = False
        else :
            self.isMarked = True

    def plainText(self):
        return False

    def set_edit_flag(self, flag) :
        self.isEdited = flag

    def set_error_flag(self, flag):
        self.isError = flag

    def get_content(self,encoding=None):
        return ""

    def get_rel_path(self):
        if self.parent == None :
            return ""
        names = []
        parentNode = self
        while True :
            names.insert(0,parentNode.name)
            parentNode = parentNode.parent
            if parentNode == None :
                break
        return "/".join(names[1:])


class NormalDirNode(TreeNode):

    def __init__(self,dir_name,abpath,projectTree):
        self.projectTree = projectTree
        self.hidden_nodes = set()
        super(NormalDirNode, self).__init__(dir_name,abpath, True)

    def _default_hidden(self,file_name):
        if self.projectTree.hide_dot_files and (file_name.startswith(".") or file_name == "CVS") :
            return True
        return False

    def _load_dir_content(self):

        if not os.path.exists(self.realpath):
            self.isLoaded = True
            return

        old_children = self._children
        self._children = []
        self.hidden_nodes = set()
        files, dirs = [], []
        for file_name in os.listdir(self.realpath) :
            if self._default_hidden(file_name):
                continue
            abpath = os.path.join(self.realpath, file_name)
            if self.get_rel_path() == "" :
                relpath = file_name
            else :
                relpath = self.get_rel_path() + "/" + file_name
            if not self.projectTree.node_visible(relpath):
                self.hidden_nodes.add(file_name)
                continue
            if os.path.isdir(abpath) :
                dirs.append((file_name, abpath))
            else :
                files.append((file_name, abpath))
        dirs.sort()
        files.sort()
        for dir_name, abpath in dirs :
            node = None
            #see if there's old dir node, for keep the node state (open/close etc).
            for old_node in old_children :
                if old_node.name == dir_name and old_node.isDirectory :
                    node = old_node
                    break
            if node == None :
                if os.path.exists(os.path.join(abpath,".classpath")) :
                    node = ProjectRootNode(abpath,self.projectTree)
                else :
                    node = NormalDirNode(dir_name, abpath, self.projectTree)
            self.add_child(node)
        for file_name, abpath in files :
            node = None
            for old_node in old_children :
                if old_node.name == file_name and old_node.isDirectory == False:
                    node = old_node
                    break
            if node == None :
                _ , ext  = os.path.splitext(file_name)
                if ext.lower() in [".zip", ".jar", ".war", ".ear"] :
                    node = ZipRootNode(file_name,abpath, False,False)
                else :
                    node = NormalFileNode(file_name, abpath, isDirectory=False)
            self.add_child(node)
        self.isLoaded = True

    def force_reload(self):
        #this is for change node name, since the path had been
        #changed, the children need to be reload.
        self._children = []
        self._load_dir_content()

    def refresh(self):
        self._load_dir_content()
        
    def get_child(self,name):
        if not self.isLoaded : 
            self.refresh()
 
        for node in self._children :
            if node.name == name :
                return node
        return None

    def get_children(self):
        if not self.isLoaded : 
            self.refresh()
        return self._children

    def add_sub_node(self, name):
        if name.find("/") > 0 :
            if name.endswith("/") :
                os.makedirs(os.path.join(self.realpath, name))
            else :
                dirname = os.path.dirname(name)
                os.makedirs(os.path.join(self.realpath, dirname))
                abspath = os.path.join(self.realpath, name)
                open(abspath, 'a').close()

            name = name[0: name.find("/")]
            subnode_abpath = os.path.join(self.realpath, name)
            node = NormalDirNode(name, subnode_abpath, self.projectTree)
            self.add_child(node)
        else :
            abspath = os.path.join(self.realpath, name)
            open(abspath, 'a').close()
            os.utime(abspath, None)
            node = NormalFileNode(name, abspath, isDirectory=False)
            self.add_child(node)
        self.isOpen = True
        return True

    def dispose(self):
        FileUtil.fileOrDirRm(self.realpath)
        self.parent.remove_child(self)
        return True

    def paste(self, nodes , remove_orignal = False ):
        if not isinstance(nodes[0],str) :
            node_paths = [node.realpath for node in nodes]
        else :
            node_paths = nodes
        commonprefix = os.path.commonprefix(node_paths)
        if not os.path.exists(commonprefix) :
            commonprefix = os.path.dirname(commonprefix)
        #logging.debug("node_paths is %s"  % str(node_paths))
        #logging.debug("commonprefix is %s"  % str(commonprefix))

        added_nodes = []
        for node in nodes :
            if node == "" :
                continue
            if isinstance(node,str) :
                file_abpath = node
                basename = os.path.basename(file_abpath)
                rel_dir = ""
            else :
                file_abpath = node.realpath
                basename = os.path.basename(file_abpath)
                #keep the original direcotry structure
                relpath = os.path.relpath(file_abpath, commonprefix)
                rel_dir = os.path.dirname(relpath)

            dst_dir = os.path.join(self.realpath, rel_dir)
            if not os.path.exists(dst_dir):
                os.makedirs(dst_dir)

            #logging.debug("file_abpath is %s , dst_dir is %s " % (file_abpath , dst_dir))
            if remove_orignal :
                FileUtil.fileOrDirMv(file_abpath,dst_dir)
                if not isinstance(node,str) :
                    node.parent.remove_child(node)
            else :
                if PathUtil.same_path(os.path.dirname(file_abpath), dst_dir) :
                    basename = basename + "_bak"
                    dst_dir = dst_dir + os.path.basename(file_abpath) + "_bak"
                    #logging.debug("file_abpath is %s , dst_dir is %s " % (file_abpath , dst_dir))
                FileUtil.fileOrDirCp(file_abpath,dst_dir)

            if rel_dir == "" :
                new_node_name = basename
            else :
                dir_name = os.path.dirname(rel_dir) 
                new_node_name = dir_name if dir_name !="" else rel_dir

            new_node_path = os.path.join(self.realpath, new_node_name)

            if os.path.isdir(new_node_path): 
                node = NormalDirNode(new_node_name, new_node_path, self.projectTree)
            else :
                node = NormalFileNode(new_node_name, new_node_path, isDirectory=False)

            if self.get_child(new_node_name) == None :
                self.add_child(node)
                added_nodes.append(node)
            else :
                added_nodes.append(self.get_child(new_node_name))

        return added_nodes

class NormalFileNode(TreeNode):

    def get_content(self,file_encoding=None):

        file_object = open(self.realpath,"r")
        try:
            all_the_text = file_object.read()
        finally:
            file_object.close()

        filecontent = all_the_text.split("\n")
        return filecontent

    def plainText(self):
        ext = os.path.splitext(self.realpath)[1]
        if ext in [".jar",".zip",".war",".rar",".jpg",".png",".gif",".class"] :
            return False
        return True

    def get_uri_path(self):
        return self.realpath

    def open_node(self, edit_cmd):
        vim.command("exec 'wincmd w'")
        vim_buffer = vim.current.buffer
        current_file_name = vim_buffer.name
        if self.realpath != current_file_name :
            vim.command("%s %s" %(edit_cmd, self.realpath))
        ProjectTree.set_file_edit(self.realpath,"true");

    def dispose(self):
        FileUtil.fileOrDirRm(self.realpath)
        self.parent.remove_child(self)
        return True

    def add_sub_node(self, name):
        print("selected item is not a dir, can't add node")
        return False

class ZipFileItemNode(TreeNode):

    def plainText(self):
        ext = os.path.splitext(self.realpath)[1]
        if ext in [".jar",".zip",".war",".rar",".jpg",".png",".gif",".class"] :
            return False
        return True

    def set_zip_file(self,zip_file_path) :
        self.zip_file_path = zip_file_path

    def get_content(self,encoding=None):
        scheme_path = "jar://"+self.zip_file_path+"!"+ self.realpath
        content = ZipUtil.read_zip_entry(scheme_path)
        return content

    def get_uri_path(self):
        scheme_path = "jar://"+self.zip_file_path+"!"+ self.realpath
        return scheme_path

    def open_node(self, edit_cmd):
        vim.command("exec 'wincmd w'")
        scheme_path = "jar://"+self.zip_file_path+"!"+ self.realpath
        final_cmd = "%s %s" %(edit_cmd, scheme_path)
        vim.command(final_cmd)
        ProjectTree.set_file_edit(scheme_path,"true");

class ZipRootNode(TreeNode):

    def __init__(self, name, realpath, isDirectory, isOpen = False, isLoaded = False):
        TreeNode.__init__(self,name,realpath, isDirectory,isOpen, isLoaded)
        self.isDirectory = True
        self.isOpen = False
        self.isLoaded = False

    def load_childrend(self):
        #logging.debug("stack:" + ''.join(traceback.format_stack()))
        zipFile = None
        try :
            opened_buf_list = VimUtil.getOpenedBufList(self.realpath)
            zipFile = zipfile.ZipFile(self.realpath)  
            for name in zipFile.namelist() :
                edited = False
                for opened_buf in  opened_buf_list :
                    zip_file_path, inner_path = ZipUtil.split_zip_scheme(opened_buf)
                    if PathUtil.same_path(inner_path,name):
                        edited = True
                        break
                self.add_tree_entry(name,edited)
            zipFile.close()
        except Exception as e :
            logging.debug("load jar childrend faied : %s" % str(e))
            if zipFile != None :
                zipFile.close()
        self.isLoaded = True

    def get_children(self):
        if not self.isLoaded : 
            self.load_childrend()
        return self._children

    def get_child(self,name):
        if not self.isLoaded : 
            self.load_childrend()
 
        return self._get_child(name)
        

    def _get_child(self,name):
 
        for node in self._children :
            if node.name == name :
                return node
        return None


    def dispose(self):
        FileUtil.fileOrDirRm(self.realpath)
        self.parent.remove_child(self)
        return True

    def add_tree_entry(self,line, edited):
        sections = line.strip().split("/")
        parentNode = self

        for index, item in enumerate(sections):
            if item == "" :
                continue
            # invoke _get_child to avoid recursion
            if isinstance(parentNode,ZipRootNode) :
                sameChild = parentNode._get_child(item)
            else :
                sameChild = parentNode.get_child(item)

            if sameChild == None :
                isDirectory = True
                if index == len(sections)-1 and not line.endswith("/"):
                    isDirectory = False
                if isDirectory :
                    node = TreeNode(item,line,isDirectory,isOpen=False,isLoaded=True)
                    parentNode.insert_child(node)
                else :
                    node = ZipFileItemNode(item,line,isDirectory,isOpen=False )
                    if edited :
                        node.set_edit_flag(True)
                    node.set_zip_file(self.realpath)
                    parentNode.add_child(node)
                parentNode = node
            else :
                parentNode = sameChild

class WorkSetNode(NormalDirNode):
    def __init__(self, work_set_name ,work_set, projectTree):
        super(WorkSetNode, self).__init__(work_set_name,"",projectTree)
        self.name = work_set_name
        self.work_set = work_set
        self.projectTree = projectTree
        self._load_workset()
        self.isOpen = True

    def refresh(self):
        self._load_workset()

    def _load_workset(self) :
        for line in self.work_set :
            split_index= line.find ("=")
            if split_index < 0 : continue 
            dir_name = line[0:split_index].strip()
            abpath = line[split_index+1:].strip()
            node = NormalDirNode(dir_name, abpath, self.projectTree)
            self.add_child(node)
        
        self.isLoaded = True

class WorkSetRootNode(NormalDirNode):

    @staticmethod
    def get_root_path():
        workset_root_dir = os.path.join(VinjaConf.getDataHome(), "workset")
        if not os.path.exists(workset_root_dir):
            os.mkdir(workset_root_dir)
        return workset_root_dir


    def __init__(self, projectTree):
        workset_root_dir = WorkSetRootNode.get_root_path()
        super(WorkSetRootNode, self).__init__("workset",workset_root_dir,projectTree)

        self.name="workset"
        self.root_dir = workset_root_dir
        self.projectTree = projectTree

        self.workset_all = {}
        self.workset_cfg_path = os.path.join(VinjaConf.getDataHome(), "workset.cfg")

        self._load_all_workset()
        self.isOpen = True

    def refresh(self):
        self._load_all_workset()

    def _load_all_workset(self) :

        import codecs
        fp = codecs.open(self.workset_cfg_path, "r", "utf-8")
        lines = fp.read().split("\n")
        #lines = open(self.workset_cfg_path,"r").readlines()
        currentSetName = ""
        currentSet = []

        for line in lines:
            if not line.strip() : continue
            if line[0] == "#" : continue
            if line.startswith("  ") :
                line = line.strip()
                currentSet.append(line)
            else :
                if len(currentSet) > 0  :
                    self.workset_all[currentSetName] = currentSet
                currentSetName = line.strip() 
                currentSet = []

        if len(currentSet) > 0  :
            self.workset_all[currentSetName] = currentSet

        for workset_name  in list(self.workset_all.keys()) :
            node = WorkSetNode(workset_name, self.workset_all[workset_name], self.projectTree)
            self.add_child(node)

        self.isLoaded = True
    
class WorkSpaceRootNode(NormalDirNode):

    @staticmethod
    def get_root_path():
        workspace_root = os.path.join(VinjaConf.getDataHome(), "workspace")
        if not os.path.exists(workspace_root):
            os.mkdir(workspace_root)
        return workspace_root

    def __init__(self, projectTree):
        
        workspace_root = WorkSpaceRootNode.get_root_path()
        super(WorkSpaceRootNode, self).__init__("workSpace",workspace_root,projectTree)

        self.name="workSpace"
        self.root_dir = workspace_root
        self.projectTree = projectTree
        self._load_workspace()
        self.isOpen = True

    def refresh(self):
        self._load_workspace()

    def _load_workspace(self) :

        self._children = []
        project_cfg_path = os.path.join(VinjaConf.getDataHome(), "project.cfg")
        lines = open(project_cfg_path).readlines()
        for line in lines:
            path_start = line.find("=")
            project_name = line[0:path_start]
            project_path = line[path_start+1:].strip()
            if not self.projectTree.node_visible(project_name):
                self.hidden_nodes.add(project_name)
                continue
            node = NormalDirNode(project_name, project_path, self.projectTree)
            self.add_child(node)
        
        self.isLoaded = True

class ProjectRootNode(NormalDirNode):

    def __init__(self,root_dir, projectTree):
        super(ProjectRootNode, self).__init__(os.path.basename(root_dir),root_dir, projectTree)
        self.root_dir = root_dir
        self.projectTree = projectTree
        self.is_java_project = True
        self.var_dict = {}
        self.decompiled_jar_dict = {}
        self.lib_srcs = []
        self._load_class_path()
        self._load_dir_content()
        self._build_virtual_noes()
        #self.isOpen = True

    def _load_class_path(self):

        varConfig = os.path.expanduser("~/.vinja/vars.txt")
        if not os.path.exists(varConfig) :
            varConfig = os.path.join(VinjaConf.getShareHome() , "conf/vars.txt")
        if os.path.exists(varConfig):
            for line in open(varConfig).readlines() :
                if line.strip() != "" and not line.startswith("#") :
                    key,value = line.split("=")
                    key,value = key.strip() , value.strip()
                    self.var_dict[key] = value

        decompiledJarConfigPath = os.path.expanduser("~/.vinja/decompile.cache")
        if os.path.exists(decompiledJarConfigPath):
            for line in open(decompiledJarConfigPath).readlines() :
                if line.strip() != "" and not line.startswith("#") :
                    cache_info = json.loads(line)
                    ori_jar_path = cache_info["originalJarPath"]
                    decompiled_jar_path = cache_info["decompiledJarPath"]
                    self.decompiled_jar_dict[ori_jar_path] = decompiled_jar_path

        classpathXml = os.path.join(self.root_dir, ".classpath")
        if not os.path.exists(classpathXml) :
            self.is_java_project = False
            return 

        tree = ElementTree()
        tree.parse(classpathXml)
        entries = tree.findall("classpathentry")
        for entry in  entries :
            kind = entry.get("kind")
            sourcepath = entry.get("sourcepath")
            path = entry.get("path")
            if kind == "var" :
                if sourcepath :
                    if sourcepath.startswith("/"):
                        sourcepath = sourcepath[1:]
                    var_key = sourcepath[0: sourcepath.find("/")]
                    var_path = self.var_dict.get(var_key)
                    if not var_path :
                        logging.debug("var %s not exist in ~/.vinja/vars.txt" % var_key)
                    else :
                        abpath = sourcepath.replace(var_key, self.var_dict.get(var_key))
                        self.lib_srcs.append(abpath)
                else :
                    var_key = path[0: path.find("/")]
                    var_path = self.var_dict.get(var_key)
                    if not var_path :
                        logging.debug("var %s not exist in ~/.vinja/vars.txt" % var_key)
                    else :
                        abpath = path.replace(var_key, self.var_dict.get(var_key))
                        decompiled_jar_path = self.decompiled_jar_dict.get(abpath)
                        if decompiled_jar_path :
                            self.lib_srcs.append(decompiled_jar_path)
                        else :
                            self.lib_srcs.append(abpath)
            elif kind == "lib" :
                if sourcepath :
                    abpath = os.path.normpath(os.path.join(self.root_dir,sourcepath))
                    self.lib_srcs.append(abpath)
                else :
                    abpath = os.path.normpath(os.path.join(self.root_dir,path))
                    decompiled_jar_path = self.decompiled_jar_dict.get(abpath)
                    if decompiled_jar_path :
                        self.lib_srcs.append(decompiled_jar_path)
                    else :
                        self.lib_srcs.append(abpath)

    def _build_virtual_noes(self):
        if not self.is_java_project :
            return 

        abspath = os.path.join(self.root_dir ,"Referenced Libraries")
        lib_src_node = TreeNode("Referenced Libraries",abspath, True,False,True)
        self.add_child(lib_src_node)

        for lib_src in self.lib_srcs :
            if not os.path.exists(lib_src):
                continue
            basename = os.path.basename(lib_src)
            try :
                node = ZipRootNode(basename,lib_src, False,False)
                lib_src_node.add_child(node)
            except Exception as e:
                logging.debug(basename+" not exists or is corrupted")

        jdk_lib_src = os.path.join(os.getenv("JAVA_HOME"),"lib/src.zip")
        if os.path.exists(jdk_lib_src) :
            try : 
                node = ZipRootNode("src.zip",jdk_lib_src, False,False)
                lib_src_node.add_child(node)
            except Exception as e:
                logging.debug(basename+" not exists or is corrupted")

    def refresh(self):
        self._load_dir_content()
        self._build_virtual_noes()


class ProjectTree(object):

    def __init__(self, root_dir, treeType="currentDir"):
        self.remove_orignal = False 
        self.hide_dot_files = True
        self.work_path_set = []
        self.edit_history = []

        if treeType == "workSetTree" :
            root_dir = WorkSetRootNode.get_root_path()
        elif treeType == "workSpaceTree":
            root_dir = WorkSpaceRootNode.get_root_path()
        self.root_dir = root_dir

        self.workset_config_path = os.path.join(self.root_dir, ".jde_work_set")
        self.tree_state_path = os.path.join(self.root_dir, ".jde_tree_state")
        self.prefix_pat = re.compile(r"[^ \-+~`|]")
        self.tree_markup_pat =re.compile(r"^[ `|]*[\-+~]")
        self._load_project_workset()
        self.root_map = {}

        if treeType == "currentDir" :
            self.root = ProjectRootNode(root_dir,self)
            self.root.isOpen = True
        elif treeType == "workSetTree":
            self.root = WorkSetRootNode(self)
        else :
            self.root = WorkSpaceRootNode(self)
        
        self.project_encoding = self._get_project_encoding()

    def _get_project_encoding(self):
        jde_file = os.path.join(self.root_dir, ".jde")
        if not os.path.exists(jde_file) :
            return "utf-8"

        tree = ElementTree()
        tree.parse(jde_file)
        entries = tree.findall("property")
        for entry in  entries :
             prop_name =entry.get("name")
             if prop_name == "encoding" :
                 return entry.get("value")
        return "utf-8"

    def _load_project_workset(self):
        if not os.path.exists(self.workset_config_path):
            return
        self.work_path_set = []
        for line in open(self.workset_config_path):
            line = line.strip()
            self.work_path_set.append(line)

    def _get_tab_id(self):
        cur_tab = vim.eval("tabpagenr()")
        tab_id = vim.eval('gettabvar("%s","tab_id")' % cur_tab)
        if tab_id == None :
            tab_id = str(uuid.uuid4())
            vim.command('call settabvar("%s","tab_id","%s")' %(cur_tab,tab_id))
        return tab_id

    def _set_render_root(self, node):
        tab_id = self._get_tab_id()
        self.root_map[tab_id] = node

    def _get_render_root(self):
        tab_id = self._get_tab_id()
        node = self.root_map.get(tab_id)
        if node == None :
            return self.root
        return node
    
    def node_visible(self, rel_path):
        if len(self.work_path_set) == 0 :
            return True
        curdir_workset =[]
        for work_path in self.work_path_set :
            if os.path.dirname(rel_path) == os.path.dirname(work_path) :
                curdir_workset.append(work_path)
        if len(curdir_workset) == 0 :
            return True
        for work_path in curdir_workset :
            if fnmatch.fnmatch(rel_path, work_path) :
                return True
        return False

    def _get_node_from_path(self, path):
            sections = path.split("/")
            if sections[-1] == "" :
                sections = sections[:-1]
            node = self._get_render_root()
            for section in sections :
                node = node.get_child(section)
            return node

    def _restore_cursor(self, row) :
        vim_buffer = vim.current.buffer
        if len(vim_buffer) < row :
            row = len(vim_buffer)
        vim.current.window.cursor = (row,0)
        vim.command("silent normal ^")


    def preview_selected_node(self, edit_cmd = "edit"):
        self.open_selected_node(edit_cmd)
        tab_id = self._get_tab_id()
        vim.command("call SwitchToVinjaView('ProjectTree_%s')" % tab_id )

    def cmp_selected_node(self):
        node = self.get_selected_node()
        node_path = node.realpath
        vim.command("exec 'wincmd w'")
        vim.command("vertical diffsplit %s" % node_path)
        

    def open_selected_node(self, edit_cmd = "edit"):
        node = self.get_selected_node()
        (row,col) = vim.current.window.cursor
        if node.isDirectory :
            if node.isOpen :
                node.isOpen = False
            else :
                node.isOpen = True
            self.render_tree()
            vim.current.window.cursor = (row,col)
        else :
            node.open_node(edit_cmd)

    def mark_selected_node(self):
        node = self.get_selected_node()
        (row,col) = vim.current.window.cursor
        #if node.isDirectory :
        #    return 
        node.toggle_mark()
        self.render_tree()
        vim.current.window.cursor = (row,col)

    def mark_visual_node(self):

        (row,col) = vim.current.window.cursor
        _,_,startLine,endLine=MiscUtil.getVisualArea()
        for row_num in range(startLine, endLine+1):
            node = self.get_selected_node(row_num)
            node.toggle_mark()
        self.render_tree()
        vim.current.window.cursor = (row,col)
        #vim.command("silent normal gv")

    def yank_visual_node(self, remove_orignal = False):
        _,_,startLine,endLine=MiscUtil.getVisualArea()
        nodes = []
        for row_num in range(startLine, endLine+1):
            node = self.get_selected_node(row_num)
            nodes.append(node)
        node = self.get_selected_node()
        global yank_buffer 
        yank_buffer = nodes
        self.remove_orignal = remove_orignal
        print("visual selected node has been yanked")

    def delete_visual_node(self):
        (row,col) = vim.current.window.cursor
        _,_,startLine,endLine=MiscUtil.getVisualArea()
        nodes = []
        for row_num in range(startLine, endLine+1):
            node = self.get_selected_node(row_num)
            nodes.append(node)
        node_names = ",".join([os.path.basename(node.realpath) for node in nodes])
        prompt = "Are you sure you with to delete \n" + node_names +" (yN):"
        answer = VimUtil.getInput(prompt)
        if not answer or answer != "y" :
            print("delete node abort.")
            return
        parent = nodes[0].parent
        for node in nodes :
            suc = node.dispose()
        self.render_tree()
        #self.select_node(parent)
        self._restore_cursor(row)

    def recursive_open_node(self):
        node = self.get_selected_node()
        opened = False
        while node.isDirectory :
            opened = True
            node.isOpen = True
            children = node.get_children()
            dirs = [item for item in children if item.isDirectory]
            if len(dirs) != 1 :
                break
            node = dirs[0]

        if opened :
            self.render_tree()
            self.select_node(node)

    def recursive_search2(self):
        search_str = vim.eval("@/")
        search_str = search_str.replace("\<", r"\b")
        search_str = search_str.replace("\>", r"\b")
        search_str = "/" + search_str + "/"
        self.recursive_search(search_str)

    def recursive_search(self, default_str = None):
        if default_str != None :
            text = VimUtil.getInput("enter string to be searched: ", default_str)
        else :
            text = VimUtil.getInput("enter string to be searched: ")
        if not text :
            return
        if text.startswith("/"):
            text = re.compile(text[1:-1])
        node = self.get_selected_node()
        if not node.isDirectory :
            return 
        result = []

        re_type = type(re.compile(""))
        def _search_node(node,text) :
            if not node.isDirectory and node.plainText() :
                # logging.debug("search content of %s " % node.realpath)
                try :
                    content = node.get_content(self.project_encoding)
                    file_path = node.get_uri_path()
                    for index,line in enumerate(content) :
                        if (isinstance(text,str) and line.find(text) > -1 ) \
                                or (isinstance(text,re_type) and text.search(line)) :
                                result.append([file_path,str(index+1),line.replace("\n","")])
                except Exception as e :
                    logging.debug("error when reading content of %s " % node.realpath)
            else :
                for subnode in node.get_children():
                    _search_node(subnode, text)
        _search_node(node,text)
        qflist = []
        for filename,lineNum,lineText in result :
            qfitem = dict(filename=self.relpath(filename),lnum=lineNum,text=lineText.strip())
            qflist.append(qfitem)

        if len(qflist) > 0 :
            vim_qflist = vim.eval('[]')
            for d in qflist:
                vim_dict = vim.Dictionary()
                for key, value in d.items():
                    vim_dict[key] = value
                vim_qflist.append(vim_dict)
            vim.vars['tmp_qflist'] = vim_qflist
            vim.command("call setqflist(tmp_qflist)" )
            vim.command("exec 'wincmd w'")
            vim.command("cwindow")
        else :
            print("can't find any reference location.")

    def filter_display_node(self):
        node = self.get_selected_node()
        relpath = node.get_rel_path()
        curdir_workset = []
        if os.path.exists(self.workset_config_path):
            with open(self.workset_config_path) as f:
                lines = [line.strip() for line in f]
            curdir_workset =[os.path.basename(line) for line in lines if os.path.dirname(line) == relpath ]

        if not node.isDirectory :
            return 
        if len(node.hidden_nodes) > 0 :
            hidden_item_str = "hidden items: " + ",".join(node.hidden_nodes) +"\n"
            displayd_items = ",".join(curdir_workset)
        else :
            hidden_item_str = ""
            displayd_items = ""
        inputStr = VimUtil.getInput(hidden_item_str+"enter displayed items:\n", displayd_items)
        if not inputStr :
            return
        file_names = inputStr.split(",")
        self._save_display_info(node, file_names)
        self.refresh_selected_node()
    
    def up_one_level(self):
        node = self.get_selected_node()
        if node.parent != None :
            self.select_node(node.parent)

    def goto_next_sibling(self):
        node = self.get_selected_node()
        sibling = node.get_next_sibling()
        if sibling != None :
            self.select_node(sibling)

    def goto_prev_sibling(self):
        node = self.get_selected_node()
        sibling = node.get_prev_sibling()
        if sibling != None :
            self.select_node(sibling)

    def get_next_marked_node(self):
        (row,col) = vim.current.window.cursor
        vim_buffer = vim.current.buffer
        for row_num in range(row+1,len(vim_buffer)+1):
            node = self.get_selected_node(row_num)
            if node.isMarked:
                vim.current.window.cursor = (row_num,col)
                break

    def get_prev_marked_node(self):
        (row,col) = vim.current.window.cursor
        vim_buffer = vim.current.buffer
        for row_num in range(row-1,0,-1):
            node = self.get_selected_node(row_num)
            if  node.isMarked:
                vim.current.window.cursor = (row_num,col)
                break

    def get_next_open_node(self):
        (row,col) = vim.current.window.cursor
        vim_buffer = vim.current.buffer
        for row_num in range(row+1,len(vim_buffer)+1):
            node = self.get_selected_node(row_num)
            if node.isEdited or node.isError :
                vim.current.window.cursor = (row_num,col)
                break

    def get_prev_open_node(self):
        (row,col) = vim.current.window.cursor
        vim_buffer = vim.current.buffer
        for row_num in range(row-1,0,-1):
            node = self.get_selected_node(row_num)
            if node.isEdited or node.isError :
                vim.current.window.cursor = (row_num,col)
                break

    def close_opened_file(self, discard_change):
        def _close_opened_file(node):
            if not node.isDirectory :
                node.set_error_flag(False)
                if node.isEdited :
                    bufnr = vim.eval("bufnr('%s')" % node.realpath)    
                    bang = "!" if discard_change else ""
                    if node.realpath in self.edit_history :
                        self.edit_history.remove(node.realpath)
                    if bufnr != "-1" :
                        try :
                            vim.command('Bclose%s %s' % (bang,bufnr))
                        except Exception as e :
                            logging.debug("close buffer %s error: %s" % (str(bufnr),str(e)))
                return
            elif node.isLoaded :
                for child in node.get_children() :
                    _close_opened_file(child)

        node = self.get_selected_node()
        _close_opened_file(node)
        self.render_tree()
        self.select_node(node)

    def _save_display_info(self, parent_node, file_names):
        
        workset = []
        node_relpath = parent_node.get_rel_path()
        if os.path.exists(self.workset_config_path):
            with open(self.workset_config_path) as f:
                lines = [line.strip() for line in f]
            workset =[line for line in lines if os.path.dirname(line) != node_relpath ]

        for file_name in file_names :
            file_name = file_name.strip()
            if file_name == "*" or file_name == "" : 
                continue
            if node_relpath != "" :
                relpath = node_relpath + "/" + file_name
            else :
                relpath = file_name
            workset.append(relpath)
        workset_file = open(self.workset_config_path,"w") 
        for item in workset :
            workset_file.write(item)
            workset_file.write("\n")
        workset_file.close()

    def close_parent_node(self):
        node = self.get_selected_node()
        node.parent.isOpen = False
        self.render_tree()
        self.select_node(node.parent)

    def yank_node_path(self):
        node = self.get_selected_node()
        vim.command("let @\" = '%s' " % node.realpath)
        print("node path yanked")

    def yank_node_rel_path(self):
        node = self.get_selected_node()
        vim.command("let @\" = '%s' " % node.get_rel_path())
        print("node relative path yanked")

    def yank_node_name(self):
        node = self.get_selected_node()
        vim.command("let @\" = '%s' " % node.name)
        print("node name yanked")

    def get_selected_node(self, row = None ):
        if row == None :
            (row,col) = vim.current.window.cursor
        path = self._get_path(row)
        if path == "" :
            return self._get_render_root()

        if path.startswith("/") :
            path = path[1:]
        if path.endswith(TreeNode.mark_postfix) :
            path = path[0: len(TreeNode.mark_postfix)]
        if path.endswith(TreeNode.edit_postfix) :
            path = path[0: len(TreeNode.edit_postfix)]
        if path.endswith(TreeNode.error_postfix) :
            path = path[0: len(TreeNode.error_postfix)]
        node = self._get_node_from_path(path)
        return node

    def select_node(self, node):
        node_list =[node.name]
        #render root node
        if node == self._get_render_root() :
            vim.current.window.cursor = (1,0)
            return
        while True :
            node = node.parent
            node_list.insert(0,node.name)
            if node == self._get_render_root() :
                break
        tree_path = "/".join(node_list[1:])
        (row,col) = projectTree.get_path_cursor(tree_path)
        vim.current.window.cursor = (row,col)

    def add_node(self):
        node = self.get_selected_node()
        prompt = "enter file name to be created, dirs endswith / \n" + node.realpath +"/"
        added_file = VimUtil.getInput(prompt)
        if not added_file :
            print("add node aborted.")
            return
        suc = node.add_sub_node(added_file)
        node_name = added_file
        if added_file.find("/") > 0  :
            node_name = added_file[0:added_file.find("/")]
        logging.debug("node_name is %s" % node_name)
        if suc :
            self.render_tree()
            self.select_node(node.get_child(node_name))

    def rename_node(self):
        node = self.get_selected_node()
        prompt = "enter new file name .\n" + os.path.dirname(node.realpath) +"/"
        new_file_name = VimUtil.getInput(prompt,node.name)
        if not new_file_name :
            print("rename node aborted.")
            return
        try :
            new_file_path = os.path.join(os.path.dirname(node.realpath),new_file_name)
            shutil.move(node.realpath, new_file_path)
            node.realpath = new_file_path
            node.name = new_file_name
            if node.isDirectory :
                node.force_reload()
            self.render_tree()
            self.select_node(node)
        except Exception as e:
            print(e)
            print("rename operation failed")

    def delete_node(self):
        (row,col) = vim.current.window.cursor
        node = self.get_selected_node()
        prompt = "Are you sure you with to delete the node \n" + node.realpath +" (yN):"
        answer = VimUtil.getInput(prompt)
        if not answer or answer != "y" :
            print("delete node abort.")
            return
        parent = node.parent
        suc = node.dispose()
        if suc :
            self.render_tree()
            self._restore_cursor(row)
            #self.select_node(parent)

    def yank_selected_node(self, remove_orignal = False):
        node = self.get_selected_node()
        global yank_buffer 
        yank_buffer = [node]
        self.remove_orignal = remove_orignal
        print("selected node has been yanked")

    def yank_marked_node(self, remove_orignal = False):
        nodes = self.get_marked_nodes()
        global yank_buffer 
        yank_buffer = nodes
        self.remove_orignal = remove_orignal
        print("visible marked node has been yanked")

    def delete_marked_node(self) :
        nodes = self.get_marked_nodes()
        prompt = "Are you sure you with to delete marked node (yN):"
        answer = VimUtil.getInput(prompt)
        if not answer or answer != "y" :
            print("delete node abort.")
            return
        for node in nodes :
            parent = node.parent
            node.dispose()
        self.render_tree()
        self.select_node(parent)

    def get_marked_nodes(self):
        nodes = []
        def _get_marked_nodes(parent_node) :
            if parent_node.isMarked :
                nodes.append(parent_node)
            if not parent_node.isOpen :
                return
            for child in parent_node.get_children():
                if child.isDirectory :
                    _get_marked_nodes(child)
                elif child.isMarked :
                    nodes.append(child)
        _get_marked_nodes(self._get_render_root())
        return nodes

    def _do_paste(self, files, remove_orignal):
        node = self.get_selected_node()
        added_nodes = node.paste(files, remove_orignal)
        if added_nodes :
            node.isOpen = True
            self.render_tree()
            last_sub_node = added_nodes[-1]
            self.select_node(last_sub_node)

    def paste(self):
        global yank_buffer 
        self._do_paste(yank_buffer,self.remove_orignal)

    def paste_from_clipBoard(self):
        files = BasicTalker.getClipbordContent().split(";")
        self._do_paste(files,False)

    def copy_to_clipBoard(self):
        file_path = self.get_selected_node().realpath
        files = BasicTalker.setClipbordContent(file_path)
        print("files had been copied to system clipboard. ")

    def open_with_default(self):
        file_path = self.get_selected_node().realpath
        BasicTalker.doTreeCmd(file_path,"openWithDefault")

    def open_in_terminal(self):
        file_path = self.get_selected_node().realpath
        BasicTalker.doTreeCmd(file_path,"openInTerminal")

    def load_java_classpath(self):
        file_path = self.get_selected_node().realpath
        ProjectManager.projectOpen(file_path)

    def print_help(self):
        help_file = os.path.join(VinjaConf.getShareHome(),"doc/tree.help")
        vim.command("exec 'wincmd w'")
        vim.command("%s %s" %("edit", help_file))

    def change_root(self):
        node = self.get_selected_node()
        if node.isDirectory :
            self._set_render_root(node)
            self.render_tree()

    def change_root_upper(self):
        node = self._get_render_root()
        parent_node = node.parent
        if parent_node == None :
            updir_path = os.path.dirname(node.realpath)
            dir_name = os.path.basename(updir_path)
            self.work_path_set = []
            parent_node = NormalDirNode(dir_name, updir_path, self)
            parent_node.add_child(node)
            parent_node.refresh()
            self.root_dir = updir_path
            self.root = parent_node
        parent_node.isOpen = True
        self._set_render_root(parent_node)
        self.render_tree()


    def change_back(self):
        node = self.get_selected_node()
        self._set_render_root(None)
        self.render_tree()
        self.select_node(node)

    def refresh_selected_node(self):
        self._load_project_workset()
        node = self.get_selected_node()
        node.refresh()
        self.render_tree()
        self.select_node(node)

    def render_tree(self):
        vim.command("setlocal modifiable")
        node = self._get_render_root()
        tab_title = os.path.basename(node.realpath)
        vim.command('call setbufvar("%%", "buf_tab_title","%s")' % tab_title)
        result = node.renderToString(0,0, [],0)
        output(result)
        vim.command("setlocal nomodifiable")
            
    def _get_path(self, row = None):
        if row == None :
            (row,col) = vim.current.window.cursor
        vim_buffer = vim.current.buffer
        line = vim_buffer[row-1]
        indent = self._get_indent_level(line)
        if indent == 0 :
            return ""
        curFile = self._strip_markup_from_line(line, False)
        lnum = row - 1
        dir = ""
        while lnum > 0 :
            lnum = lnum - 1
            curLine = vim_buffer[lnum]
            curLineStripped = self._strip_markup_from_line(curLine, True)
            if lnum == 0 :
                break
            lpindent = self._get_indent_level(curLine)
            if lpindent < indent :
                indent = indent - 1
                dir =  curLineStripped + dir
        if not dir.endswith("/") :
            curFile = dir + "/" + curFile
        else :
            curFile = dir + curFile
        return curFile

    def relpath(self, path):
        if path.startswith(os.getcwd()) :
            return os.path.relpath(path)
        else :
            return path

    def toggleHidden(self):
        if self.hide_dot_files :
            self.hide_dot_files = False
        else :
            self.hide_dot_files = True
        self.refresh_selected_node()

    def _get_indent_level(self,line):
        matches = self.prefix_pat.search(line)
        if matches :
            return matches.start() / 2
        return -1

    def _strip_markup_from_line(self,line,remove_leading_spaces):

        #remove the tree parts and the leading space
        line = self.tree_markup_pat.sub("",line)

        #strip off any read only flag
        line = re.sub(' \[RO\]', "", line)
        line = line.replace(TreeNode.mark_postfix, "")
        line = line.replace(TreeNode.edit_postfix, "")
        line = line.replace(TreeNode.error_postfix, "")

        #strip off any bookmark flags
        line = re.sub( ' {[^}]*}', "", line)

        if remove_leading_spaces :
            line = re.sub( '^ *', "", line)

        return line

    def get_path_cursor(self,path):
        (row,col) = vim.current.window.cursor
        vim_buffer = vim.current.buffer
        max_row = len(vim_buffer)
        indent = 0
        lnum = 1
        sections = path.split("/")
        section_idx = 0

        while lnum < max_row :
            curLine = vim_buffer[lnum]
            curLineStripped = self._strip_markup_from_line(curLine, True)
            if curLineStripped.endswith("/"):
                curLineStripped = curLineStripped[:-1]
            lpindent = self._get_indent_level(curLine)
            if curLineStripped == sections[section_idx] and lpindent == indent + 1 :
                section_idx += 1
                indent +=1
                if section_idx >= len(sections) :
                    break
            lnum += 1
        return lnum+1, len(sections)*2

    def open_path(self, path, node = None, abpath = True ):
        if node == None :
            node = self._get_render_root() 
            if isinstance(node,WorkSpaceRootNode) :
                for child in node.get_children():
                    if PathUtil.in_directory(path,child.realpath): 
                        node = child
                        break
            if isinstance(node,WorkSetRootNode) :
                for child in node.get_children():
                    for childs_child in child.get_children():
                        if PathUtil.in_directory(path,childs_child.realpath): 
                            node = childs_child
                            break

        if path.startswith("jar:") :
            zip_file_path, inner_path =ZipUtil.split_zip_scheme(path)
            inner_path = inner_path.replace("\\","/")
            zip_base_name = os.path.basename(zip_file_path)
            zip_inner_node = self.find_node(path)

            if zip_inner_node == None :
                return None
            
            #return self.open_path(inner_path, zip_file_node, False)
            node = zip_inner_node
            tree_path =[node.name]
            while True :
                node = node.parent
                node.isOpen = True
                if node == self._get_render_root() :
                    break
                tree_path.insert(0,node.name)
            return "/".join(tree_path)
        else :
            if abpath :
                path = os.path.relpath(path, node.realpath)
            path = path.replace("\\","/")
            sections = path.split("/")
            if sections[-1] == "" :
                sections = sections[:-1]
            
            found_node = True
            for section in sections :
                node = node.get_child(section)
                if node == None :
                    found_node = False
                    break
                node.isOpen = True

            if not found_node :
                return None

            tree_path =[node.name]
            while True :
                node = node.parent
                node.isOpen = True
                if node == self._get_render_root() :
                    break
                tree_path.insert(0,node.name)
            return "/".join(tree_path)

    def find_node(self,path, node=None):

        if path == None :
            return None
        if node == None :
            node = self.root
            #workspaceRoot is just virtual node, search child nodes
            if isinstance(node,WorkSpaceRootNode) :
                for child in node.get_children():
                    if PathUtil.in_directory(path,child.realpath): 
                        node = child
                        break
                if node.realpath == path :
                    return node

            if isinstance(node,WorkSetRootNode) :
                for child in node.get_children():
                    for childs_child in child.get_children():
                        if PathUtil.in_directory(path,childs_child.realpath): 
                            node = childs_child
                            break
                if node.realpath == path :
                    return node

        def _find_jar_in_lib(node,zip_file_path) :

            if not node.isDirectory :
                return None

            if isinstance(node,ZipRootNode) :
                return None

            lib_node = node.get_child("Referenced Libraries")
            if lib_node != None :
                zip_base_name = os.path.basename(zip_file_path)
                zip_file_node = lib_node.get_child(zip_base_name)
                if zip_file_node != None :
                    return zip_file_node

            for subnode in node.get_children():
                zip_file_node = _find_jar_in_lib(subnode, zip_file_path)
                if zip_file_node != None :
                    return zip_file_node
            return None


        if path.startswith("jar:") :
            zip_file_path, inner_path =ZipUtil.split_zip_scheme(path)
            inner_path = inner_path.replace("\\","/")
            zip_base_name = os.path.basename(zip_file_path)
            path = inner_path
            tmp_node = self.find_node(zip_file_path)
            if tmp_node == None :
                tmp_node = _find_jar_in_lib(node, zip_file_path)
            node = tmp_node
        else :
            try :
                path = os.path.relpath(path, node.realpath)
            except :
                return None
            path = path.replace("\\","/")

        if node == None :
            return  None

        sections = path.split("/")
        if sections[-1] == "" :
            sections = sections[:-1]
        
        found_node = None
        for section in sections :
            node = node.get_child(section)
            if node == None :
                return None
            found_node = node
        return found_node

    def save_status(self, closeFile = True):
        opened_dir_nodes = []
        def _get_opened_nodes(current_node) :
            if current_node.isOpen :
                opened_dir_nodes.append(current_node.realpath)
            if not current_node.isLoaded :
                return
            for child in current_node.get_children():
                if child.isDirectory :
                    _get_opened_nodes(child)
                elif child.isEdited :
                    bufnr = vim.eval("bufnr('%s')" % child.realpath)    
                    if bufnr != "-1" and closeFile:
                        vim.command('Bclose %s' % bufnr)
                    #opened_dir_nodes.append(child.realpath)
        _get_opened_nodes(self._get_render_root())

        tree_state_file = open(self.tree_state_path,"w") 
        for path in  opened_dir_nodes :
            tree_state_file.write(path)
            tree_state_file.write("\n")
        rev_history = self.edit_history[0:10]
        for path in  rev_history :
            tree_state_file.write(path)
            tree_state_file.write("\n")
        tree_state_file.close()
        if not closeFile :
            print("ProjectTree status has been saved.")

    def restore_status(self, node_type = "dir"):
        if not os.path.exists(self.tree_state_path):
            return
        lines = open(self.tree_state_path,"r").readlines()
        edit_count = 0
        for line in lines :
            path = line.strip()
            if node_type == "dir" :
                node = self.find_node(path)
                if node != None :
                    if node.isDirectory :
                        node.isOpen = True
                    else :
                        node.isEdited = True
            elif node_type !="dir" and os.path.isfile(path):
                edit_count = edit_count + 1
                if edit_count < 18 :
                    edit_cmd = "edit"
                    vim.command("%s %s" %(edit_cmd, path))

    @staticmethod
    def locate_buf_in_tree(current_file_name = None):

        if current_file_name == None :
            vim_buffer = vim.current.buffer
            current_file_name = vim_buffer.name

        if current_file_name == None or current_file_name =="" or "ProjectTree" in current_file_name:
            return 

        tab_id = projectTree._get_tab_id()
        vim.command("call SwitchToVinjaView('ProjectTree_%s')" % tab_id )
        tree_path = projectTree.open_path(current_file_name)
        if tree_path == None :
            print("can't find node %s in ProjectTree" % current_file_name)
            return
        projectTree.render_tree()
        (row,col) = projectTree.get_path_cursor(tree_path)
        vim.current.window.cursor = (row,col)

    @staticmethod
    def create_project_tree(projectRoot = None):
        vim_buffer = vim.current.buffer
        current_file_name = vim_buffer.name

        if projectRoot != None :
            tree = ProjectTree(projectRoot)
            return tree
    
        if current_file_name == None or current_file_name.startswith("jar:") :
            fake_file = os.path.join(os.getcwd(),"what_ever_fake_file_name")
            projectRoot = ProjectManager.getProjectRoot(fake_file,False)
        else :
            projectRoot = ProjectManager.getProjectRoot(current_file_name,False)

        if projectRoot == None :
            projectRoot = os.path.abspath(os.getcwd())
        tree = ProjectTree(projectRoot)
        return tree

    @staticmethod
    def create_workspace_tree(projectRoot = None) :
        tree = ProjectTree(projectRoot, "workSpaceTree")
        return tree

    @staticmethod
    def create_workset_tree(projectRoot = None) :
        tree = ProjectTree(projectRoot, "workSetTree")
        return tree

    @staticmethod
    def set_file_edit(path, flag):
        if "projectTree" not in globals() :
            return 

        if flag == "true" :
            flag = True
        else :
            flag = False
        node = projectTree.find_node(path)
        if node != None :
            node.set_edit_flag(flag)
            normed_path = os.path.normpath(path)
            if path in projectTree.edit_history :
                projectTree.edit_history.remove(normed_path)
            if flag :
                projectTree.edit_history.insert(0,normed_path)
        else : 
            return

        render_root = projectTree._get_render_root()
        tmp_node = node.parent
        under_render_root = False
        while True :
            if tmp_node == render_root :
                under_render_root = True
                break
            tmp_node = tmp_node.parent
            if tmp_node == None :
                break
        if not under_render_root :
            return 

        tab_id = projectTree._get_tab_id()
        if not VimUtil.isVinjaBufferVisible('ProjectTree_%s' % tab_id):
            return 
        vim.command("call SwitchToVinjaView('ProjectTree_%s')" % tab_id )
        (row,col) = vim.current.window.cursor
        projectTree.render_tree()
        vim.current.window.cursor = (row,col)
        vim.command("exec 'wincmd w'")

    @staticmethod
    def dispose_tree():
        global projectTree
        tab_id = projectTree._get_tab_id()
        if VimUtil.isVinjaBufferVisible("ProjectTree_%s" % tab_id):
            projectTree.save_status()
            VimUtil.closeVinjaBuffer("ProjectTree_%s" % tab_id)
        projectTree = None
        del globals()["projectTree"]

    @staticmethod
    def runApp():
        if "projectTree" not in globals() :
            global projectTree
            projectTree = None
        if projectTree == None :
            projectTree = ProjectTree.create_project_tree()

        vim_buffer = vim.current.buffer
        current_file_name = vim_buffer.name
        
        tab_id = projectTree._get_tab_id()
        if VimUtil.isVinjaBufferVisible("ProjectTree_%s" % tab_id):
            VimUtil.closeVinjaBuffer("ProjectTree_%s" % tab_id)
        else :
            vim.command("call SplitLeftPanel(30, 'VinjaView_ProjectTree_%s')" % tab_id )
            vim.command("set filetype=ztree")
            vim.command("setlocal statusline=\ ProjectTree")
            vim.command("call SwitchToVinjaView('ProjectTree_%s')" % tab_id )
            projectTree.restore_status()
            projectTree.render_tree()
            if current_file_name != None :
                ProjectTree.locate_buf_in_tree(current_file_name)
            vim.command("exec 'wincmd w'")
            projectTree.restore_status(node_type="file")
            vim.command("exec 'wincmd w'")


    @staticmethod
    def toggleTreeType(treeType):
        global projectTree
        if projectTree == None :
            return 

        projectTree.save_status()
        projectTree = None
        del globals()["projectTree"]
        vim.command("setlocal modifiable")
        vim_buffer = vim.current.buffer
        vim_buffer[:] = None
        vim.command("setlocal nomodifiable")

        if treeType == "workSpaceTree"  :
            projectTree = ProjectTree.create_workspace_tree()
        elif treeType == "workSetTree" :
            projectTree = ProjectTree.create_workset_tree()
        else:
            projectTree = ProjectTree.create_project_tree()
        projectTree.restore_status()
        projectTree.render_tree()

        vim.command("exec 'wincmd w'")
        projectTree.restore_status(node_type="file")
        vim.command("exec 'wincmd w'")

