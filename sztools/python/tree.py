import zipfile, os  
from common import ZipUtil

class TreeNode(object):

    def __init__(self, name, isDirectory, isOpen = False):
        self.name=name
        self.isDirectory = isDirectory
        self.isOpen = isOpen
        self.children = []

    def getChild(self,name):
        for node in self.children :
            if node.name == name :
                return node
        return None

    def getChildren(self) :
        return self.children

    def addChild(self,child):
        self.children.append(child)

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
                treeParts = treeParts + self.name + "/" + "\n"
            else :
                treeParts = treeParts + self.name + "\n"

        if self.isDirectory and self.isOpen :
            childNodes = self.getChildren()
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

class ZipTree(object):

    def __init__(self,zip_file_path):

        self.prefix_pat = re.compile(r"[^ \-+~`|]")
        self.tree_markup_pat =re.compile(r"^[ `|]*[\-+~]")
        self.zip_file_path = zip_file_path
        self.root = self.build_zip_tree()

    def add_tree_entry(self,root, line):
        sections = line.split("/")
        parentNode = root

        for index, item in enumerate(sections):
            if item == "" :
                continue
            sameChild = parentNode.getChild(item)
            if sameChild == None :
                isDirectory = True
                if index == len(sections)-1 and not line.endswith("/"):
                    isDirectory = False
                node = TreeNode(item, isDirectory,isOpen=False)
                parentNode.addChild(node)
                parentNode = node
            else :
                parentNode = sameChild

    def build_zip_tree(self):
        zipFile = zipfile.ZipFile(self.zip_file_path)  
        root = TreeNode(self.zip_file_path,isDirectory=True, isOpen=True)
        root.isDirectory = True
        root.isOpen = True
        for name in zipFile.namelist() :
            self.add_tree_entry(root,name)
        zipFile.close()
        return root

    def renderToString(self):
        result = self.root.renderToString(0,0, [],0)
        return result

    def output_content(self,path):
        zipFile = zipfile.ZipFile(self.zip_file_path)  
        content = zipFile.read(path)
        zipFile.close()
        vim.command("exec 'wincmd w'")
        output(content)

    def get_indent_level(self,line):
        matches = self.prefix_pat.search(line)
        if matches :
            return matches.start() / 2
        return -1

    def strip_markup_from_line(self,line,remove_leading_spaces):

        #remove the tree parts and the leading space
        line = self.tree_markup_pat.sub("",line)

        #strip off any read only flag
        line = re.sub(' \[RO\]', "", line)

        #strip off any bookmark flags
        line = re.sub( ' {[^}]*}', "", line)

        if remove_leading_spaces :
            line = re.sub( '^ *', "", line)

        return line

    def get_path(self):
        (row,col) = vim.current.window.cursor
        vim_buffer = vim.current.buffer
        line = vim_buffer[row-1]
        indent = self.get_indent_level(line)
        curFile = self.strip_markup_from_line(line, False)
        lnum = row - 1
        dir = ""
        while lnum > 0 :
            lnum = lnum - 1
            curLine = vim_buffer[lnum]
            curLineStripped = self.strip_markup_from_line(curLine, True)
            if lnum == 0 :
                break
            lpindent = self.get_indent_level(curLine)
            if lpindent < indent :
                indent = indent - 1
                dir =  curLineStripped + dir
        if not dir.endswith("/") :
            curFile = dir + "/" + curFile
        else :
            curFile = dir + curFile
        return curFile

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
            curLineStripped = self.strip_markup_from_line(curLine, True)
            if curLineStripped.endswith("/"):
                curLineStripped = curLineStripped[:-1]
            lpindent = self.get_indent_level(curLine)
            if curLineStripped == sections[section_idx] and lpindent == indent + 1 :
                section_idx += 1
                indent +=1
                if section_idx >= len(sections) :
                    break
            lnum += 1
        return lnum+1, len(sections)*2


    def get_node_from_path(self, path):
        sections = path.split("/")
        if sections[-1] == "" :
            sections = sections[:-1]
        node = self.root
        for section in sections :
            node = node.getChild(section)
        return node

    def open_path(self, path):
        sections = path.split("/")
        if sections[-1] == "" :
            sections = sections[:-1]
        node = self.root
        for section in sections :
            node = node.getChild(section)
            node.isOpen = True

    def open_node(self):
        (row,col) = vim.current.window.cursor
        path = self.get_path()
        if path.startswith("/") :
            path = path[1:]
        node = self.get_node_from_path(path)
        if node.isDirectory :
            if node.isOpen :
                node.isOpen = False
            else :
                node.isOpen = True
            output(self.renderToString())
            vim.current.window.cursor = (row,col)
        else :
            vim.command("exec 'wincmd w'")
            schmeme_path = "jar://"+self.zip_file_path+"!"+path
            vim.command("edit %s" % schmeme_path)

    @staticmethod
    def runApp(zip_file_path=None):
        global ziptree
        inner_path = None
        if not zip_file_path :
            path = vim.current.buffer.name
            if path and path.startswith("jar:") :
                zip_file_path, inner_path = ZipUtil.split_zip_scheme(path)

        if not os.path.exists(zip_file_path):
            print "zip file path not exists."
            return
        ziptree = ZipTree(zip_file_path)
        vim.command("call SplitLeftPanel(40, 'Ztree')")
        vim.command("set filetype=ztree")
        if inner_path != None :
            ziptree.open_path(inner_path)
        output(ziptree.renderToString())
        if inner_path != None :
            (row,col) = ziptree.get_path_cursor(inner_path)
            vim.current.window.cursor = (row,col)

    
    
