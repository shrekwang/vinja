import zipfile, os  
import shutil
from common import ZipUtil,FileUtil 
from xml.etree.ElementTree import *
from jde import ProjectManager

class TreeNode(object):
    mark_postfix = " [mark]"
    edit_postfix = " [edit]"


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

    def get_display_str(self):
        dis_str = self.name
        if self.isMarked :
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
        print "unsupported operation."
        return False

    def dispose(self):
        print "unsupported operation."
        return False

    def refresh(self):
        "there's nothing to do here"
        return

    def paste(self, file_names):
        print "unsupported operation."
        return False

    def toggle_mark(self):
        if self.isMarked :
            self.isMarked = False
        else :
            self.isMarked = True

    def set_edit_flag(self, flag) :
        self.isEdited = flag


class NormalDirNode(TreeNode):

    def __init__(self,dir_name,abpath,projectTree):
        self.projectTree = projectTree
        self.hidden_nodes = set()
        super(NormalDirNode, self).__init__(dir_name,abpath, True)

    def _load_dir_content(self):
        old_children = self._children
        self._children = []
        self.hidden_nodes = set()
        files, dirs = [], []
        for file_name in os.listdir(self.realpath) :
            if file_name.startswith(".") :
                continue
            abpath = os.path.join(self.realpath, file_name)
            if not self.projectTree.node_visible(abpath):
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
                node = NormalDirNode(dir_name, abpath, self.projectTree)
            self.add_child(node)
        for file_name, abpath in files :
            node = NormalFileNode(file_name, abpath, isDirectory=False)
            self.add_child(node)
        self.isLoaded = True

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
        abspath = os.path.join(self.realpath, name)
        if name.endswith("/") :
            os.makedirs(abspath)
            node = NormalDirNode(name[:-1], abspath[:-1], self.projectTree)
            self.add_child(node)
        else :
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
        node_paths = [node.realpath for node in nodes]
        commonprefix = os.path.commonprefix(node_paths)

        added_nodes = []
        for node in nodes :
            file_abpath = node.realpath
            basename = os.path.basename(file_abpath)
            #keep the original direcotry structure
            relpath = os.path.relpath(file_abpath, commonprefix)
            rel_dir = os.path.dirname(relpath)
            dst_dir = os.path.join(self.realpath, rel_dir)
            if not os.path.exists(dst_dir):
                os.makedirs(dst_dir)

            if remove_orignal :
                FileUtil.fileOrDirMv(file_abpath,dst_dir)
                node.parent.remove_child(node)
            else :
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

    def open_node(self, edit_cmd):
        vim.command("exec 'wincmd w'")
        vim.command("%s %s" %(edit_cmd, self.realpath))

    def dispose(self):
        FileUtil.fileOrDirRm(self.realpath)
        self.parent.remove_child(self)
        return True

    def add_sub_node(self, name):
        print "selected item is not a dir, can't add node"
        return False

class ZipFileItemNode(TreeNode):

    def set_zip_file(self,zip_file_path) :
        self.zip_file_path = zip_file_path

    def open_node(self, edit_cmd):
        vim.command("exec 'wincmd w'")
        scheme_path = "jar://"+self.zip_file_path+"!"+ self.realpath
        vim.command("%s %s" %(edit_cmd, scheme_path))

class ZipRootNode(TreeNode):

    def __init__(self, name, realpath, isDirectory, isOpen = False, isLoaded = False):
        TreeNode.__init__(self,name,realpath, isDirectory,isOpen, isLoaded)
        zipFile = zipfile.ZipFile(self.realpath)  
        self.isDirectory = True
        self.isOpen = False
        for name in zipFile.namelist() :
            self.add_tree_entry(name)
        zipFile.close()

    def add_tree_entry(self,line):
        sections = line.strip().split("/")
        parentNode = self

        for index, item in enumerate(sections):
            if item == "" :
                continue
            sameChild = parentNode.get_child(item)
            if sameChild == None :
                isDirectory = True
                if index == len(sections)-1 and not line.endswith("/"):
                    isDirectory = False
                if isDirectory :
                    node = TreeNode(item,line,isDirectory,isOpen=False)
                    parentNode.insert_child(node)
                else :
                    node = ZipFileItemNode(item,line,isDirectory,isOpen=False)
                    node.set_zip_file(self.realpath)
                    parentNode.add_child(node)
                parentNode = node
            else :
                parentNode = sameChild

class ProjectRootNode(NormalDirNode):

    def __init__(self,root_dir, projectTree):
        super(ProjectRootNode, self).__init__("project",root_dir, projectTree)
        self.root_dir = root_dir
        self.projectTree = projectTree
        self.is_java_project = True
        self.var_dict = {}
        self.lib_srcs = []
        self._load_class_path()
        self._load_dir_content()
        self._build_virtual_noes()
        self.isOpen = True

    def _load_class_path(self):
        varConfig = os.path.expanduser("~/.sztools/vars.txt")
        if not os.path.exists(varConfig) :
            varConfig = os.path.join(SzToolsConfig.getShareHome() , "conf/vars.txt")
        if os.path.exists(varConfig):
            for line in open(varConfig).readlines() :
                if line.strip() != "" and not line.startswith("#") :
                    key,value = line.split("=")
                    key,value = key.strip() , value.strip()
                    self.var_dict[key] = value
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
            if kind == "var" and sourcepath :
                    var_key = sourcepath[0: sourcepath.find("/")]
                    abpath = sourcepath.replace(var_key, self.var_dict.get(var_key))
                    self.lib_srcs.append(abpath)
            elif kind == "lib" and sourcepath :
                abpath = os.path.normpath(os.path.join(self.root_dir,sourcepath))
                self.lib_srcs.append(abpath)

    def _build_virtual_noes(self):
        if not self.is_java_project :
            return 
        lib_src_node = TreeNode("Referenced Libraries","v", True,False,True)
        self.add_child(lib_src_node)

        for lib_src in self.lib_srcs :
            basename = os.path.basename(lib_src)
            node = ZipRootNode(basename,lib_src, False,False)
            lib_src_node.add_child(node)

        jdk_lib_src = os.path.join(os.getenv("JAVA_HOME"),"src.zip")
        if os.path.exists(jdk_lib_src) :
            node = ZipRootNode("src.zip",jdk_lib_src, False,False)
            lib_src_node.add_child(node)

    def _build_file_nodes(self):
        files, dirs = [], []
        
        for file_name in os.listdir(self.root_dir) :
            if file_name.startswith(".") :
                continue
            abpath = os.path.join(self.root_dir,file_name)
            if not self.projectTree.node_visible(abpath) :
                self.hidden_nodes.add(file_name)
                continue
            if os.path.isdir(abpath) :
                dirs.append((file_name, abpath))
            else :
                files.append((file_name, abpath))

        dirs.sort()
        files.sort()
        for dir_name, abpath in dirs :
            node = NormalDirNode(dir_name, abpath, self.projectTree)
            self.add_child(node)
        for file_name, abpath in files :
            node = NormalFileNode(file_name, abpath, isDirectory=False)
            self.add_child(node)

    def refresh(self):
        self._load_dir_content()
        self._build_virtual_noes()


class ProjectTree(object):

    def __init__(self, root_dir):
        self.yank_buffer = []
        self.remove_orignal = False 
        self.render_root = None
        self.work_path_set = []
        self.workset_config_path = os.path.join(root_dir, ".jde_work_set")
        self.prefix_pat = re.compile(r"[^ \-+~`|]")
        self.tree_markup_pat =re.compile(r"^[ `|]*[\-+~]")
        self.root_dir = root_dir
        self._load_project_workset()
        self.root = ProjectRootNode(self.root_dir,self)

    def _load_project_workset(self):
        if not os.path.exists(self.workset_config_path):
            return
        self.work_path_set = []
        for line in open(self.workset_config_path):
            line = line.strip()
            self.work_path_set.append(os.path.join(self.root_dir,line))


    def _get_render_root(self):
        if self.render_root == None :
            return self.root
        return self.render_root

    def node_visible(self, abs_path):
        if len(self.work_path_set) == 0 :
            return True
        for work_path in self.work_path_set :
            if work_path == abs_path or work_path.startswith(abs_path) \
                    or abs_path.startswith(work_path) :
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


    def filter_display_node(self):
        node = self.get_selected_node()
        if not node.isDirectory :
            return 
        if len(node.hidden_nodes) > 0 :
            hidden_item_str = "hidden items: " + ",".join(node.hidden_nodes) +"\n"
            displayd_items = ",".join([item.name for item in node.get_children()])
        else :
            hidden_item_str = ""
            displayd_items = ""
        inputStr = VimUtil.getInput(hidden_item_str+"enter displayed items:\n", displayd_items)
        if not inputStr :
            return
        file_names = inputStr.split(",")
        self._save_display_info(node, file_names)
        self.refresh_selected_node()
    

    def _save_display_info(self, parent_node, file_names):
        
        workset = []
        if os.path.exists(self.workset_config_path):
            workset = open(self.workset_config_path,"r").readlines()
        parent_relpath = os.path.relpath(parent_node.realpath, self.root.realpath)
        not_parent_filter = lambda path : not parent_relpath.startswith(path) \
            and os.path.dirname(path) != parent_relpath
        workset = [ line.strip() for line in workset if not_parent_filter(line.strip()) ]

        for file_name in file_names :
            if file_name.strip() == "*" : 
                file_name = ""
            abpath = os.path.join(parent_node.realpath, file_name)
            relpath = os.path.relpath(abpath, self.root.realpath)
            exits_deeper_filter = False
            for old_item in workset :
                if old_item.startswith(relpath) :
                    exits_deeper_filter = True
                    break
            if not exits_deeper_filter :
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
        print "node path yanked"

    def get_selected_node(self):
        (row,col) = vim.current.window.cursor
        path = self._get_path()
        if path == "" :
            return self._get_render_root()

        if path.startswith("/") :
            path = path[1:]
        if path.endswith(TreeNode.mark_postfix) :
            path = path[0: len(TreeNode.mark_postfix)]
        if path.endswith(TreeNode.edit_postfix) :
            path = path[0: len(TreeNode.edit_postfix)]
        node = self._get_node_from_path(path)
        return node

    def select_node(self, node):
        node_list =[node.name]
        #root node
        if node.parent == None :
            vim.current.window.cursor = (1,0)
            return
        while True :
            node = node.parent
            if node == None :
                break
            node_list.insert(0,node.name)
        tree_path = "/".join(node_list[1:])
        (row,col) = projectTree.get_path_cursor(tree_path)
        vim.current.window.cursor = (row,col)

    def add_node(self):
        node = self.get_selected_node()
        prompt = "enter file name to be created, dirs endswith / \n" + node.realpath +"/"
        added_file = VimUtil.getInput(prompt)
        if not added_file :
            print "add node aborted."
            return
        suc = node.add_sub_node(added_file)
        node_name = added_file
        if added_file.endswith("/") :
            node_name = added_file[:-1]
        if suc :
            self.render_tree()
            self.select_node(node.get_child(node_name))

    def rename_node(self):
        node = self.get_selected_node()
        prompt = "enter new file name .\n" + os.path.dirname(node.realpath) +"/"
        new_file_name = VimUtil.getInput(prompt,node.name)
        if not new_file_name :
            print "rename node aborted."
            return
        try :
            new_file_path = os.path.join(os.path.dirname(node.realpath),new_file_name)
            shutil.move(node.realpath, new_file_path)
            node.realpath = new_file_path
            node.name = new_file_name
            self.render_tree()
            self.select_node(node)
        except Exception , e:
            print e
            print "rename operation failed"

    def delete_node(self):
        node = self.get_selected_node()
        prompt = "Are you sure you with to delete the node \n" + node.realpath +" (yN):"
        answer = VimUtil.getInput(prompt)
        if not answer or answer != "y" :
            print "delete node abort."
            return
        parent = node.parent
        suc = node.dispose()
        if suc :
            self.render_tree()
            self.select_node(parent)

    def yank_selected_node(self, remove_orignal = False):
        node = self.get_selected_node()
        self.yank_buffer = [node]
        self.remove_orignal = remove_orignal
        print "selected node has been yanked"

    def yank_marked_node(self) :
        nodes = self.get_marked_nodes()
        self.yank_buffer = nodes
        self.remove_orignal = False
        print "visible marked node has been yanked"

    def delete_marked_node(self) :
        nodes = self.get_marked_nodes()
        prompt = "Are you sure you with to delete marked node (yN):"
        answer = VimUtil.getInput(prompt)
        if not answer or answer != "y" :
            print "delete node abort."
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

    def paste(self):
        node = self.get_selected_node()
        added_nodes = node.paste(self.yank_buffer, self.remove_orignal)
        if added_nodes :
            node.isOpen = True
            self.render_tree()
            last_sub_node = added_nodes[-1]
            self.select_node(last_sub_node)

    def change_root(self):
        node = self.get_selected_node()
        if node.isDirectory :
            self.render_root = node
            self.render_tree()

    def change_back(self):
        node = self.get_selected_node()
        self.render_root = None
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
        output(self.renderToString())
        vim.command("setlocal nomodifiable")
            
    def _get_path(self):
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

    def renderToString(self):
        node = self._get_render_root()
        result = node.renderToString(0,0, [],0)
        return result

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
            render_root = node

        if path.startswith("jar:") :
            zip_file_path, inner_path =ZipUtil.split_zip_scheme(path)
            inner_path = inner_path.replace("\\","/")
            zip_base_name = os.path.basename(zip_file_path)
            lib_node = node.get_child("Referenced Libraries")
            lib_node.isOpen = True
            zip_file_node = lib_node.get_child(zip_base_name)
            zip_file_node.isOpen = True
            return self.open_path(inner_path, zip_file_node, False)
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
                if node == render_root :
                    break
                tree_path.insert(0,node.name)
            return "/".join(tree_path)

    def find_node(self,path):
        node = self._get_render_root() 
        try :
            path = os.path.relpath(path, node.realpath)
        except :
            return None
        path = path.replace("\\","/")
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

    @staticmethod
    def locate_buf_in_tree(current_file_name = None):

        if current_file_name == None :
            vim_buffer = vim.current.buffer
            current_file_name = vim_buffer.name

        if current_file_name == None or "ProjectTree" in current_file_name:
            return 
        vim.command("call SwitchToSzToolView('ProjectTree')" )
        tree_path = projectTree.open_path(current_file_name)
        if tree_path == None :
            print "can't find node %s in ProjectTree" % current_file_name
            return
        projectTree.render_tree()
        (row,col) = projectTree.get_path_cursor(tree_path)
        vim.current.window.cursor = (row,col)

    @staticmethod
    def create_project_tree():
        vim_buffer = vim.current.buffer
        current_file_name = vim_buffer.name

        if current_file_name == None or current_file_name.startswith("jar:") :
            fake_file = os.path.join(os.getcwd(),"what_ever_fake_file_name")
            projectRoot = ProjectManager.getProjectRoot(fake_file)
        else :
            projectRoot = ProjectManager.getProjectRoot(current_file_name)

        if projectRoot == None :
            projectRoot = os.path.abspath(os.getcwd())
        tree = ProjectTree(projectRoot)
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
        else : 
            return

        if not VimUtil.isSzToolBufferVisible('ProjectTree'):
            return 
        vim.command("call SwitchToSzToolView('ProjectTree')" )
        (row,col) = vim.current.window.cursor
        projectTree.render_tree()
        vim.current.window.cursor = (row,col)
        vim.command("exec 'wincmd w'")

    @staticmethod
    def dispose_tree():
        if VimUtil.isSzToolBufferVisible("ProjectTree"):
            VimUtil.closeSzToolBuffer("ProjectTree")
            global projectTree
            projectTree = None

    @staticmethod
    def runApp():
        if "projectTree" not in globals() :
            global projectTree
            projectTree = None
        if projectTree == None :
            projectTree = ProjectTree.create_project_tree()

        vim_buffer = vim.current.buffer
        current_file_name = vim_buffer.name
        
        if VimUtil.isSzToolBufferVisible("ProjectTree"):
            VimUtil.closeSzToolBuffer("ProjectTree")
        else :
            vim.command("call SplitLeftPanel(30, 'SzToolView_ProjectTree')")
            vim.command("set filetype=ztree")
            vim.command("call SwitchToSzToolView('ProjectTree')" )
            projectTree.render_tree()
            if current_file_name != None :
                ProjectTree.locate_buf_in_tree(current_file_name)


