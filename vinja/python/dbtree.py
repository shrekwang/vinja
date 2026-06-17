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
from tree import TreeNode, get_current_tree, set_current_tree, remove_current_tree, _get_tab_id_global

class DatabaseInstanceNode(TreeNode):
    def __init__(self,host,user,password):
        self.host=host
        self.user=user
        self.password=password
        self._load_databases()

    def _load_databases(self):
        if not os.path.exists(self.realpath):
            self.isLoaded = True
            return

    def get_child(self,name):
        for node in self._children :
            if node.name == name :
                return node
        return None

class DatabaseNode(TreeNode):

    def __init__(self,instance,dbname):
        self.instance=instance
        self.dbname=dbname


    def get_child(self,name):
        for node in self._children :
            if node.name == name :
                return node
        return None

class DatabaseTableNode(TreeNode):

    def __init__(self,instance,dbname,tblname):
        self.instance=instance
        self.dbname=dbname
        self.tblname=tblname

class DatabaseRootNode(object):
    def __init__(self):
        self._load_instances()
        
    def _load_instances(self):
        node = DatabaseInstanceNode("172.16.236.32", "tuya_dba", "Tuyadba@123")
        self.add_child(node)
        node = DatabaseInstanceNode("172.16.236.33", "tuya_dba", "Tuyadba@123")
        self.add_child(node)

class DatabaseTree(object):

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
        self.root_map = {}

        if treeType == "currentDir" :
            self.root = ProjectRootNode(root_dir,self)
            self.root.isOpen = True
        elif treeType == "workSetTree":
            self.root = WorkSetRootNode(self)
        else :
            self.root = WorkSpaceRootNode(self)
        
        self.project_encoding = self._get_project_encoding()


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
        cur_tab = vim.eval("tabpagenr()")
        effective_path = node.realpath if node is not None else self.root.realpath
        vim.command('call settabvar("%s","workspace_path","%s")' %(cur_tab, effective_path))

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
        search_str = search_str.replace(r"\<", r"\b")
        search_str = search_str.replace(r"\>", r"\b")
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
        for postfix in TreeNode.meta_status_postfixes.values():
            if path.endswith(postfix):
                path = path[:-len(postfix)]
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
        (row,col) = self.get_path_cursor(tree_path)
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

    def copy_to_clipBoard(self):
        file_path = self.get_selected_node().realpath
        files = BasicTalker.setClipbordContent(file_path)
        print("files had been copied to system clipboard. ")

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
        line = re.sub(r' \[RO\]', "", line)
        line = line.replace(TreeNode.mark_postfix, "")
        line = line.replace(TreeNode.edit_postfix, "")
        line = line.replace(TreeNode.error_postfix, "")
        for postfix in TreeNode.meta_status_postfixes.values():
            line = line.replace(postfix, "")

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
            parentDir = os.path.dirname(current_file_name)
            if os.path.exists(parentDir):
                projectRoot = parentDir
            else :
                projectRoot = os.path.abspath(os.getcwd())
        tree = ProjectTree(projectRoot)
        return tree


    @staticmethod
    def set_file_edit(path, flag):
        from tree import get_all_trees
        all_trees = get_all_trees()
        if len(all_trees) == 0:
            return 

        if flag == "true" :
            flag = True
        else :
            flag = False

        for tab_id, tree in all_trees.items():
            node = tree.find_node(path)
            if node != None :
                node.set_edit_flag(flag)
                normed_path = os.path.normpath(path)
                if normed_path in tree.edit_history :
                    tree.edit_history.remove(normed_path)
                if flag :
                    tree.edit_history.insert(0,normed_path)

                if not VimUtil.isVinjaBufferVisible('ProjectTree_%s' % tab_id):
                    continue
                vim.command("call SwitchToVinjaView('ProjectTree_%s')" % tab_id )
                (row,col) = vim.current.window.cursor
                tree.render_tree()
                vim.current.window.cursor = (row,col)
                vim.command("exec 'wincmd w'")

    @staticmethod
    def dispose_tree():
        tree = get_current_tree()
        if tree is None:
            return
        tab_id = _get_tab_id_global()
        if VimUtil.isVinjaBufferVisible("ProjectTree_%s" % tab_id):
            tree.save_status()
            VimUtil.closeVinjaBuffer("ProjectTree_%s" % tab_id)
        remove_current_tree()

    @staticmethod
    def runApp():
        tree = get_current_tree()
        if tree is None:
            tree = ProjectTree.create_project_tree()
            set_current_tree(tree)

        vim_buffer = vim.current.buffer
        current_file_name = vim_buffer.name
        
        tab_id = _get_tab_id_global()
        if VimUtil.isVinjaBufferVisible("ProjectTree_%s" % tab_id):
            VimUtil.closeVinjaBuffer("ProjectTree_%s" % tab_id)
        else :
            vim.command("call SplitLeftPanel(30, 'VinjaView_ProjectTree_%s')" % tab_id )
            vim.command("set filetype=ztree")
            vim.command(r"setlocal statusline=\ ProjectTree")
            vim.command("call SwitchToVinjaView('ProjectTree_%s')" % tab_id )
            tree.restore_status()
            tree.render_tree()
            if current_file_name != None :
                ProjectTree.locate_buf_in_tree(current_file_name)
            vim.command("exec 'wincmd w'")
            tree.restore_status(node_type="file")
            vim.command("exec 'wincmd w'")


    @staticmethod
    def toggleTreeType(treeType):
        tree = get_current_tree()
        if tree is None:
            return 

        tree.save_status()
        remove_current_tree()
        vim.command("setlocal modifiable")
        vim_buffer = vim.current.buffer
        vim_buffer[:] = None
        vim.command("setlocal nomodifiable")

        if treeType == "workSpaceTree"  :
            tree = ProjectTree.create_workspace_tree()
        elif treeType == "workSetTree" :
            tree = ProjectTree.create_workset_tree()
        else:
            tree = ProjectTree.create_project_tree()
        set_current_tree(tree)
        tree.restore_status()
        tree.render_tree()

        vim.command("exec 'wincmd w'")
        tree.restore_status(node_type="file")
        vim.command("exec 'wincmd w'")

