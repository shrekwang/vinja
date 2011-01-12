import os,os.path,fnmatch,shutil
import glob,time,vim,re, sys, logging
from subprocess import Popen, PIPE
from optparse import OptionParser 
import sqlite3 as sqlite
from jde import Talker


class ShextOptionParser(OptionParser):
    noAction = False

    def exit(self, status=0, msg=None):
        if msg:
            Shext.stdout(msg)
        self.noAction = True

    def error(self, msg):
        Shext.stdout(self.format_help())
        self.exit()

    def print_help(self, file=None):
        Shext.stdout(self.format_help())
        
class LsCmd(object):

    def __init__(self, screen_width) :
        self.cmp_types = ["zip","rar","tgz","tar","jar","war","gz"]
        self.exe_types = ["exe","bat","sh"]
        self.col_pad = 3
        self.max_column = 10
        self.file_types = {}
        self.colorInfos = []
        self.screen_width = screen_width

    def colorOutput(self):
        vim.command("call SwitchToSzToolView('shext')" )
        vim.command("syntax clear")
        for item in self.colorInfos :
            vim.command(item)

        dir_color = sztoolsCfg.get("dir_color")
        exe_color = sztoolsCfg.get("exe_color")
        cmp_color = sztoolsCfg.get("cmp_color")

        vim.command("highlight shextdir guifg=%s" % dir_color)
        vim.command("highlight shextcmp guifg=%s" % cmp_color)
        vim.command("highlight shextexe guifg=%s" % exe_color)

        listwinnr = str(vim.eval("winnr('#')"))
        vim.command("exec '" + listwinnr + " wincmd w'")


    def get_format_info(self, sorted_list, cols):
        allValue = []
        if len(sorted_list) % cols == 0 :
            rows = len(sorted_list) / cols
        else :
            rows = len(sorted_list) / cols + 1
        for i in range(rows):
            rowValue = []
            for j in range(cols):
                offset = (j * rows) + i
                if  offset < len(sorted_list) :
                    rowValue.append(sorted_list[offset])
                else :
                    rowValue.append("")
            allValue.append(rowValue)

        col_max_widths = []
        for i in range(cols):
            max = strWidth(allValue[0][i])
            for j in range(rows):
                if strWidth(allValue[j][i]) > max : max = strWidth(allValue[j][i])
            col_max_widths.append(max+self.col_pad)

        return allValue,col_max_widths

    def extract_file_typeinfo(self,path,file_list) :
        self.file_types.clear()
        for file in file_list:
            basename,ext = os.path.splitext(file)
            ext_len = len(ext)
            if  os.path.isdir(os.path.join(path,file)) :
                self.file_types[file] = "dir"
            elif  ext_len > 1 and ext[1:] in self.cmp_types :
                self.file_types[file] = "cmp"
            elif  ext_len > 1 and ext[1:] in self.exe_types :
                self.file_types[file] = "exe"

    def sort_file(self,path,file_list,options) :
        if not options.sort_field : 
            return file_list
        if not file_list  or len(file_list) == 0 : 
            return []
        if options.sort_field == "size" :
            sort_factor = lambda p,n : os.path.getsize(os.path.join(p,n))
        elif options.sort_field == "time" :
            sort_factor = lambda p,n : os.path.getmtime(os.path.join(p,n))
        elif options.sort_field == "name" :
            sort_factor = lambda p,n : os.path.basename(os.path.join(p,n))
        temp_list = [] 
        for item in  file_list :
            temp_list.append((sort_factor(path,item),item))
        temp_list.sort()
        return [file_name for factor, file_name in temp_list]


    def getmatrix(self, file_list,max_column=-1):
        if not file_list: 
            return []
        if max_column == -1 : 
            max_column = self.max_column
        for i in range(max_column, 0, -1):
            allValue, col_max_widths = self.get_format_info(file_list, i)
            if sum(col_max_widths) < self.screen_width :
                break

        col_bounds = []
        start = 0
        for width in col_max_widths :
            col_bounds.append((start, start+width))
            start = start + width

        self.colorInfos = []
        for rowIndex, rowValues in enumerate(allValue) :
            for colIndex, colValue in enumerate(rowValues) :
                filetype = self.file_types.get(colValue)
                if filetype :
                    start,end = col_bounds[colIndex]
                    colorInfo = """syn match shext%s "\%%%sl\%%>%sv.\%%<%sv" """ %(filetype,rowIndex+1,start,end)
                    self.colorInfos.append(colorInfo)

        nnlist = []
        for item in allValue:
            nnlist.append("".join([col+((col_max_widths[index]-strWidth(col))*" ") for index,col in enumerate(item)]))
        return nnlist

    def lsd(self):

        file_list= filter(lambda item: os.path.isdir(item), os.listdir(os.getcwd()))
        nlist = self.getmatrix(file_list)
        self.colorOutput()
        Shext.stdout(nlist)

    def ls(self,cmd_array):
        parser = ShextOptionParser()
        parser.add_option("-l","--long-format",action="store_true", dest="longformat")
        parser.add_option("-L","--single-column",action="store_true", dest="singlecolumn")
        parser.add_option("-t",action="store_const",const="time", dest="sort_field",help="sort file by modified time")
        parser.add_option("-s",action="store_const",const="size", dest="sort_field",help="sort file by file size")
        parser.add_option("-n",action="store_const",const="name", dest="sort_field",help="sort file by file name")
        (options, args) = parser.parse_args(cmd_array)
        if parser.noAction :
            return 

        if len(args) > 0 : 
            pathname = args[0]
        else :
            pathname = "*"

        if pathname.endswith(":") :
            pathname = pathname + os.path.sep
            
        if pathname.startswith("/") or re.match("^.:.*",pathname) :
            abspath = pathname
        elif pathname.startswith("~"):
            abspath = os.path.expanduser(pathname)
        else :
            abspath = os.path.join(os.getcwd(),pathname)

        file_list = []
        if os.path.exists(abspath):
            if os.path.isdir(abspath):
                file_list= os.listdir(abspath)
            else:
                file_list = [abspath]
            file_list = self.sort_file(abspath, file_list, options)
            self.extract_file_typeinfo(abspath, file_list)
        else :
            file_list = [os.path.basename(item) for item in glob.glob(abspath)]
            sepIndex = abspath.rfind("/")
            if sepIndex > -1:
                file_list = self.sort_file(abspath[0:sepIndex+1], file_list, options)
                self.extract_file_typeinfo(abspath[0:sepIndex+1], file_list)
            else :
                file_list = self.sort_file(os.getcwd(), file_list, options)
                self.extract_file_typeinfo(os.getcwd(), file_list)

        if options.longformat :
            nlist = self.getLongFormatInfo(os.getcwd(), file_list)
        elif options.singlecolumn :
            nlist = self.getmatrix(file_list, 1)
        else :
            nlist = self.getmatrix(file_list)

        self.colorOutput()
        Shext.stdout(nlist)

    def getLongFormatInfo(self, path, file_list):
        infos = []
        nlist = [os.path.basename(item) for item in file_list]
        max = 0
        for item in nlist:
            if len(item) > max : max = len(item)
        # make more space
        max = max + 4

        self.colorInfos = []
        for rowIndex, item in enumerate(file_list) :
            abspath = os.path.join(path, item)
            basename = os.path.basename(abspath)
            time_info= time.strftime("%Y-%m-%d %H:%M:%S ", time.localtime(os.path.getmtime(abspath)))
            size_info= " %0.1f kb" % float(os.path.getsize(abspath)/1024)
            infos.append( "%s %s %s" %( basename.ljust(max), time_info, size_info ) )

            filetype = self.file_types.get(item)
            if filetype :
                start,end = (0, max)
                colorInfo = """syn match shext%s "\%%%sl\%%>%sc.\%%<%sc" """ %(filetype, rowIndex+1, start, end)
                self.colorInfos.append(colorInfo)

        return infos

class FindCmd(object):
    def search_file(self,cmd_array):

        class NameFilter(object):
            def __init__(self, filter ):
                self.filter = filter
            def accepted(self, filename, filepath) :
                if fnmatch.fnmatch(filename, self.filter) :
                    return True
                return False

        class PathFilter(object):
            def __init__(self, path) :
                self.path = path
            def accepted(self, filename, filepath ):
                match = re.search(self.path, filepath) 
                if match :
                    return True
                return False

        class SizeFilter(object):
            def __init__(self, size):
                self.size = size
            def accepted(self, filename, filepath):
                return True

        class ContentFilter(object):
            def __init__(self, content):
                self.content = content
            def accepted(self, filename, filepath):
                file_object = open(os.path.join(filepath, filename))
                try:
                    all_the_text = file_object.read()
                finally:
                    file_object.close()
                if all_the_text.find(self.content) > -1 :
                    return True
                return False

        Shext.stdout("")

        parser = ShextOptionParser( usage="usage: find [paths] [options]")
        parser.add_option("-n","--name",action="store", dest="name")
        parser.add_option("-t","--text",action="store", dest="text")
        parser.add_option("-s","--size",action="store", dest="size")
        parser.add_option("-p","--path",action="store", dest="path")

        (options, args) = parser.parse_args(cmd_array)
        if parser.noAction : return 
        if len(args) > 0 : 
            search_paths = args
        else :
            search_paths = ["."]
        
        filters = []
        if options.name :
            filters.append(NameFilter(options.name))
        else :
            filters.append(NameFilter("*"))

        if options.path:
            filters.append(PathFilter(options.path))
        if options.size:
            filters.append(SizeFilter(options.size))
        if options.text :
            filters.append(ContentFilter(options.text))

        for search_path in search_paths :
            self._search_file(search_path, filters)

    def _match_one_file(self, filename,search_path, filters ):
        abspath = os.path.join(search_path, filename)
        matched = True
        for filter in filters :
            if not filter.accepted(filename, search_path) :
                matched = False
                break
        if matched :
            Shext.stdout(abspath, append=True)

    def _search_file(self,search_path, filters):
        if  os.path.isfile(search_path) : 
            filename= os.path.basename(search_path)
            dirname = os.path.dirname(search_path)
            self._match_one_file(filename, dirname, filters)
            return 

        if not os.path.isdir(search_path): 
            return 

        file_list= os.listdir(search_path)
        if file_list == [] : 
            return 

        for filename in file_list:
            abspath = os.path.join(search_path, filename)
            if os.path.isfile(abspath) :
                self._match_one_file(filename, search_path, filters)
            else :
                if not filename.startswith("."):
                    self._search_file(abspath, filters)

class LocateCmd(object):

    def __init__(self, db_path):
        self.db_path = db_path
        self.records = []
        self.default_exclude = [".svn",".cvs"]

        if os.path.exists(db_path): return 

        parent_path = os.path.dirname(db_path)
        if not os.path.exists(parent_path):
          os.makedirs(parent_path)
        
    def locateFile(self,fname,alias=None,searchPath=None, startWithAlias = False):
        conn = sqlite.connect(self.db_path)
        cur = conn.cursor()
        oldfname = fname
        selectSql = ("select a.start_dir,a.name,a.rtl_path,b.alias "
            " from fsdb_files a, fsdb_dirs b where a.start_dir = b.start_dir ")
        if fname.find("*") > -1 or fname.find("?") > -1 :
            fname = fname.replace("*","%")
            fname = fname.replace("?","_")
        selectSql += " and a.name like ?  " 
        codepage = sys.getdefaultencoding()
        params = [ fname.decode(codepage) ]

        if searchPath :
            if searchPath.find("*") > -1 or searchPath.find("?") > -1 :
                searchPath = searchPath.replace("*","%")
                searchPath = searchPath.replace("?","_")
            selectSql += " and a.start_dir||a.rtl_path like ?  " 
            searchPath = "%%%s%%" % searchPath.strip()
            params.append( searchPath.decode(codepage))

        if alias :
            selectSql += " and b.alias = ? "
            params.append(alias.decode(codepage))

        cur.execute(selectSql,tuple(params))
        if startWithAlias :
            rows = [(os.path.join(alias,path),alias,start_dir) for (start_dir,name,path,alias) in cur.fetchall()]
        else :
            rows = [os.path.join(start_dir,path) for (start_dir,name,path,alias) in cur.fetchall()]
        conn.close()
        return rows

    def listLocatedDir(self):
        conn = sqlite.connect(self.db_path)
        cur = conn.cursor()
        selectSql = "select alias, start_dir from fsdb_dirs"
        cur.execute(selectSql)
        rows = cur.fetchall()
        conn.close()
        return rows

    def locate(self, cmd_array) :

        parser = ShextOptionParser(usage="usage: locate [options] [name-pattern]")
        parser.add_option("-n","--alias",action="store", dest="alias" , help ="search in designated index entry" )
        parser.add_option("-p","--path", action="store", dest="path" , help = "the path need to match with")
        (options, args) = parser.parse_args(cmd_array)
        if parser.noAction : return 
        fname = args[0]
        rows = self.locateFile(fname,options.alias,options.path)
        Shext.stdout(rows)

    def grep(self,pattern,name,alias,path):
        result = []
        files = self.locateFile(name,alias,path)
        for file_name in files :
            if not os.path.isfile(file_name): continue
            filecontent = open(file_name).readlines()
            for index,line in enumerate(filecontent) :
                match = re.search(pattern,line)
                if match :
                    result.append([file_name,str(index+1),line.replace("\n","")])
        return result

class ShUtil(object):

    def __init__(self,screen_width,yank_buffer):
        self.lscmd = LsCmd(screen_width)
        self.findcmd = FindCmd()
        self.shext_bm_path = os.path.join(getDataHome(), "shext-bm.txt")
        if not os.path.exists(self.shext_bm_path) :
            open(self.shext_bm_path, 'w').close()
        shext_locatedb_path = os.path.join(getDataHome(), "locate.db")
        self.locatecmd = LocateCmd(shext_locatedb_path)
        self.yank_buffer = yank_buffer
        self.cd_history = [os.getcwd()]
        self.pathResolver = PathResolver(self)

    def ls(self,cmd_array):
        self.lscmd.ls(cmd_array)

    def updatedb(self,cmd_array):
        self.locatecmd.updatedb(cmd_array)

    def locate(self,cmd_array):
        self.locatecmd.locate(cmd_array)

    def lgrep(self,cmd_array):

        parser = ShextOptionParser(usage="usage: lgrep [options] [search-pattern] [filename]")
        parser.add_option("-n","--alias",action="store", dest="alias" , help ="search in designated index entry" )
        parser.add_option("-p","--path", action="store", dest="path" , help = "the path need to match with")
        (options, args) = parser.parse_args(cmd_array)
        if parser.noAction : 
            return 

        if len(args) != 2 : 
            Shext.stdout("incorrect number of arguments.")
            return

        result = self.locatecmd.grep(args[0],args[1],options.alias, options.path)
        Shext.stdout([" : ".join(item) for item in result])


    def ledit(self,fname,nocmdBuffer = False):
        result = self.locatecmd.locateFile(fname)
        if result == None or len(result) == 0 :
            if nocmdBuffer :
                print "can't locate the file"
            else :
                Shext.stdout("can't locate the file.")
            return

        if len(result) == 1 :
            if nocmdBuffer:
                vim.command("edit %s" % result[0] )
            else :
                self.edit(result)
        else :
            for index,item in enumerate(result):
                print str(index) + ": " + item
            vim.command("let b:result_number = input('please enter a selection')")
            exists = vim.eval("exists('b:result_number')")
            if exists == "1" :
                result_number = int(vim.eval("b:result_number"))
                if nocmdBuffer:
                    vim.command("edit %s" % result[result_number] )
                else :
                    self.edit([result[result_number]])


    def lsd(self):
        self.lscmd.lsd()

    def find(self, cmd_array):
        self.findcmd.search_file(cmd_array)

    def isWildCardStr(self,value):
        globchars = ["*","?","[","]"]
        result = False
        for char in globchars:
            if value.find(char) > -1 :
                result =True
                break
        return result

    def edit(self,cmd_array):
        parser = ShextOptionParser()
        parser.add_option("-n","--notab",action="store_true",
                dest="notab", help="default behavior is edit file in new tab. turn this on to edit in next tab" )
        (options, args) = parser.parse_args(cmd_array)
        if parser.noAction :
            return 

        names = []
        currentTabNum = vim.eval("tabpagenr()")
        for arg in args:
            file_list = self.pathResolver.resolve(arg)
            names.extend(file_list)
        if not options.notab :
            vim.command("tabnew")
            vim.command("tabprevious")
        for name in names:
            vim.command("tabnext")
            nextTabNum = vim.eval("tabpagenr()")
            if currentTabNum == nextTabNum :
                vim.command("tabnew")
            vim.command("edit %s" %(name))
            vim.command("tabprevious")

    def cd(self ,path):
        abspath = ""
        if path == "-" :
            # cd_history[0] points to current dir
            if len(self.cd_history) > 1 :
                abspath = self.cd_history[1]
            else :
                abspath = self.cd_history[0]

        pathList = self.pathResolver.resolve(path)
        if len(pathList) == 1 and os.path.exists(pathList[0]) :
            abspath = pathList[0]
        if not os.path.exists(abspath) :
            Shext.stdout("directory '%s' not exists." % path)
            return

        vim.command("lcd %s" % abspath.replace(" ",r"\ "))
        if abspath in self.cd_history :
            self.cd_history.remove(abspath)
        self.cd_history.insert(0,abspath)
        self.ls(["*"])

    def cdlist(self):
        Shext.stdout(self.cd_history)

    def touch(self, args):
        for arg in args :
            abspath = os.path.join(os.getcwd(),arg)
            open(abspath, 'a').close()
            os.utime(abspath, None)
        self.ls(["*"])

    def mkdir(self,args):
        for arg in args :
            abspath = os.path.join(os.getcwd(),arg)
            os.makedirs(abspath)
        Shext.stdout("dir %s had been created ." % (";".join(args)))

    def rm(self,args):
        parser = ShextOptionParser()
        parser.add_option("-r",action = "store_true", dest="recursive")
        (options, args) = parser.parse_args(args)
        count = 0 
        for arg in args :
            names = self.pathResolver.resolve(arg)
            for name in names :
                count += 1
                if (options.recursive) : 
                    shutil.rmtree(name)
                else :
                    os.remove(name)
        Shext.stdout("%s items had been removed ." %str(count) )

    def rmdir(self,args):
        count = 0 
        for arg in args :
            abspath = os.path.join(os.getcwd(),arg)
            count += 1
            os.removedirs(abspath)
        Shext.stdout("%s dir had been removed ." %str(count))

    def merge(self,args):
        dst = args[-1]
        dstfile = open(dst,"w")
        count = 0
        for arg in args[:-1]:
            names = self.pathResolver.resolve(arg)
            for src in names :
                if os.path.isfile(src) :
                    count +=1
                    lines = open(src).readlines()
                    for line in lines :
                        dstfile.write(line)
                    dstfile.write("\n")
        dstfile.close()
        Shext.stdout("%s files had been merged to %s." %( str(count),dst ))

    def cp(self,args):
        dst = args[-1]
        count = 0
        for arg in args[:-1]:
            names = self.pathResolver.resolve(arg)
            for src in names :
                count += 1
                if os.path.isdir(src):
                    tmp_dst = os.path.exists(dst) and os.path.join(dst,os.path.basename(src)) or dst
                    shutil.copytree(src, tmp_dst)
                else:
                    shutil.copy2(src, dst)
        Shext.stdout("%s items had been copied." % str(count))

    def yank(self,args,clearBuffer = True,mode="yank"):
        """mode can be one of ('yank','cut') """
        pwd = os.getcwd()
        if clearBuffer :
            self.yank_buffer = []
        count = 0
        for arg in args :
            names = self.pathResolver.resolve(arg)
            for src in names :
                count += 1
                self.yank_buffer.append((mode,os.path.join(pwd,src)))
        Shext.stdout("%s items had been yanked." % str(count))

    def yankbuffer(self):
        lines = [src for mode,src in self.yank_buffer ]
        Shext.stdout(lines)

    def paste(self):
        dst = "."
        count = 0
        for mode,src in self.yank_buffer :
            count += 1
            if mode == "yank" :
                if os.path.isdir(src):
                    if os.path.exists(dst):
                        dst = os.path.join(dst,os.path.basename(src))
                    shutil.copytree(src, dst)
                else:
                    shutil.copy2(src, dst)
            else :
                shutil.move(src,dst)
        Shext.stdout("%s items had been pasted." % str(count))

    def mv(self,args):
        dst = args[-1]
        count = 0
        for arg in args[:-1]:
            names = self.pathResolver.resolve(arg)
            for src in names :
                count += 1
                if os.path.isdir(src):
                    tmp_dst = os.path.exists(dst) and os.path.join(dst,os.path.basename(src)) or dst
                    shutil.move(src, tmp_dst)
                else:
                    shutil.move(src, dst)
        Shext.stdout("%s items had been moved." % str(count))
        

    def bmadd(self, bm=False):
        pwd = os.getcwd()
        bm_file_obj = open(self.shext_bm_path,"a")
        basename = os.path.basename(pwd).replace(" ","")
        bm_file_obj.write(basename + " " + pwd +"\n")
        bm_file_obj.close()
        Shext.stdout("the current dir has been added to bookmark.")

    def bmlist(self, bm=False):
        bmfile = self.shext_bm_path 
        lines = [line.replace("\n","") for line in open(bmfile).readlines()]
        Shext.stdout(lines)

    def bmedit(self, bm=False):
        bmfile = self.shext_bm_path
        self.edit([bmfile])

    def pwd(self):
        lines = [os.getcwd()]
        Shext.stdout(lines)

    def cat(self,args):
        filename = args[0]
        abspath = os.path.join(os.getcwd(),filename)
        lines = [item.replace("\n","")  for item in  open(abspath,"r").readlines()]
        Shext.stdout(lines)

    def head(self,args):
        filename = args[0]
        abspath = os.path.join(os.getcwd(),filename)
        lines = [item.replace("\n","")  for item in  open(abspath,"r").readlines()][0:20]
        Shext.stdout(lines)

    def getbm(self,bm_name):
        lines = open(self.shext_bm_path).readlines()
        for line in lines:
            path_start = line.find(" ")
            name = line[0:path_start]
            if name == bm_name :
                # get rid of "\n"
                return line[path_start:-1].strip()
        return ""

class ShextArgumentError(Exception):

    def __init__(self, msg):
        self.msg = msg
    def __str__(self):
        return repr(self.msg)

class Shext(object):
    appendToOutBuffer = False

    @staticmethod
    def runApp():
        global shext
        shext = Shext()
        shext.createOutputBuffer()
        shext.ls()

    @staticmethod
    def ledit(name):
        shUtil = ShUtil(screen_width=80,yank_buffer=None)
        shUtil.ledit(name,True)
        del shUtil

    @staticmethod
    def getOutputBuffer():
        shext_buffer = None
        for buffer in vim.buffers:
            if buffer.name and buffer.name.find( "SzToolView_shext") > -1 :
                shext_buffer = buffer
                break
        return shext_buffer

    @staticmethod
    def stdout(msg, append = False):
        if (Shext.appendToOutBuffer ) :
            output("\n==================",Shext.getOutputBuffer(),True)
            output(msg,Shext.getOutputBuffer(),True)
            Shext.clearColorSyntax()
        else :
            output(msg,Shext.getOutputBuffer(),append)

    @staticmethod
    def clearColorSyntax():
        vim.command("call SwitchToSzToolView('shext')" )
        vim.command("syntax clear")
        listwinnr = str(vim.eval("winnr('#')"))
        vim.command("exec '"+listwinnr+" wincmd w'")

    def __init__(self):
        self.yank_buffer = []
        self.shUtil = ShUtil(screen_width=80,yank_buffer=self.yank_buffer)
        self.pathResolver = PathResolver(self.shUtil)
        self.special_cmds = ["exit","edit","ledit","bmedit"]

    def createOutputBuffer(self):
        vim.command("call SwitchToSzToolView('shext')" )
        vim.command("setlocal number")
        listwinnr = str(vim.eval("winnr('#')"))
        vim.command("exec '"+listwinnr+" wincmd w'")

    def enoughArguments(self,tokens):
        cmdName = tokens[0]
        min_num = 1
        if cmdName in ["touch","rm","mkdir","rmdir","yank",
                "yankadd","cut","cutadd","cat","head","edit"] :
            min_num = 2
        if cmdName in ["cp","mv","merge"]:
            min_num = 3

        if len(tokens) < min_num :
            Shext.stdout("not enough arguments.")
            return False
        return True

    def parseCmd(self,cmdLine):
        cmdLine = cmdLine.replace("\ ","$$").strip()
        cmdArray = [ item.replace("$$"," ") for item in re.split(r"\s+",cmdLine)]
        result = []
        for item in cmdArray :
            #replace the $[1] $[1:2] like variables
            #  $[1] stands for output buffer line 1
            #  #[1] stands for cmd edit buffer line 1
            if item.find("$") > -1 or item.find("#") > -1 :
                varBuffer = []
                if item.find("$") > -1 :
                    varBuffer = Shext.getOutputBuffer()
                    item = item.replace("$","varBuffer")
                else :
                    varBuffer = vim.current.buffer
                    item = item.replace("#","varBuffer")

                if item == "varBuffer" :
                    vars = eval("varBuffer[:]")
                else :
                    # only replace the first num (start row index)
                    repl = lambda matchobj : "[" + str (int(matchobj.group(1)) -1 )
                    item = re.sub(r"\[(\d+)",repl, item)
                    digits = re.findall(r"[-\d]+",item)
                    #start index can't be 0
                    if digits and digits[0] == "-1" :
                        raise ShextArgumentError("index can't be '0', it starts from '1'. ")
                    vars = eval(item)

                if type(vars) == type([]) :
                    for var in vars :
                        result.append(var)
                else :
                    result.append(vars)
            else :
                result.append(item)
        return result

    def tipDirName(self):
        work_buffer = vim.current.buffer
        row,col = vim.current.window.cursor
        line = work_buffer[row-1]
        cmd = self.parseCmd(line)
        if len(cmd) == 1 : 
            if line.strip() == "" or line[col] == " " :
                path = "/"
            else :
                path = cmd[0]
        else :
            path= ( cmd[-1].strip() == "" and "/" or cmd[-1].strip() )

        pathList = self.pathResolver.resolve(path)
        if len(pathList) == 1 and os.path.exists(pathList[0]) :
            self.shUtil.ls([pathList[0]])

    def exit(self):
        """
        for buffer in vim.buffers:
            if buffer.name and buffer.name.find( "SzToolView_shext") > -1 :
                vim.command("bd! %s" %buffer.name)
        """
        vim.command("bw! SzToolView_shext")
        vim.command("bw!")
        shext = None

    def runSysCmd(self,cmdArray):
        try :
            cmdPath = os.path.join(os.getcwd(),cmdArray[0])
            if os.path.isfile(cmdPath) :
                cmdArray[0] = os.path.abspath(cmdPath)
            if os.name == "posix" :
                cmdResult = Popen(cmdArray ,stdin=PIPE, stdout=PIPE, stderr=PIPE).communicate()
            else :
                cmdResult = Popen(cmdArray ,stdin=PIPE, stdout=PIPE, stderr=PIPE,shell=True).communicate()
        except (OSError,ValueError) , msg:
            Shext.stdout(msg)
        else :
            Shext.stdout(cmdResult[0].replace("\r\n","\n"))
            Shext.stdout(cmdResult[1].replace("\r\n","\n"),True)

    def runInBackground(self,cmdline):
        try :
            pid = Popen([cmdline],shell = True).pid
        except (OSError,ValueError) , msg:
            Shext.stdout(msg)

    def help(self):
        help_file = open(os.path.join(getShareHome(),"doc/sztools.help"))
        content = [line.replace("\n","") for line in help_file.readlines()]
        help_file.close()
        Shext.stdout(content)

    def locatedb(self,args):
        if not agentHasStarted():
            shext.stdout("the sztool agent is not started. run :StartAgent to start.")
            return
        result = Talker.doLocatedbCommand(args)
        Shext.stdout(result)

    def ls(self):
        self.shUtil.ls(["*"])

    def batch(self,args):
        for arg in args :
            self.executeCmd(arg,batchMode=True)

    def echo(self, args):
        
        if not args or len(args) == 0 :
            Shext.stdout("" )
        else :
            Shext.stdout(args[0] )
            for arg in args[1:] :
                Shext.stdout(arg ,append=True)

    def set(self , args):
        parser = ShextOptionParser( usage="usage: set [options]")
        parser.add_option("-a", action="store_true",  dest="append",help="append mode in output buffer.")
        parser.add_option("-o", action="store_false", dest="append",help="write mode in output buffer.")

        (options, args) = parser.parse_args(args)
        if parser.noAction :
            return 
        if options.append :
            Shext.appendToOutBuffer = True
        else :
            Shext.appendToOutBuffer = False

    def getCmdLine(self):
        work_buffer = vim.current.buffer
        row,col = vim.current.window.cursor
        return work_buffer[row-1]

    def executeCmd(self, cmdLine=None, batchMode=False, insertMode=True):
        
        if not cmdLine :
            cmdLine = self.getCmdLine()

        if cmdLine.strip() == "" :
            if not batchMode and insertMode :
                vim.command("normal o")
                vim.command("startinsert")
            return

        if cmdLine.startswith("!") :
            self.runInBackground(cmdLine[1:])
            return 

        try :
            cmd = self.parseCmd(cmdLine)
        except ShextArgumentError , e :
            Shext.clearColorSyntax()
            Shext.stdout(e.msg)
            return
        except Exception , e :
            Shext.clearColorSyntax()
            Shext.stdout(str(e))
            return

        if cmd[0] not in self.special_cmds :
            Shext.clearColorSyntax()

        if self.enoughArguments(cmd) :
            self.dispatchCmd(cmd)

        if cmd[0] not in self.special_cmds and not batchMode and insertMode :
            vim.command("normal o")
            vim.command("startinsert")

    def dispatchCmd(self, cmd) :
        if cmd[0] == "cd" :
            path = len(cmd) > 1 and cmd[1] or None
            self.shUtil.cd(path)
        elif cmd[0] == "cdlist" :
            self.shUtil.cdlist()
        elif cmd[0] == "pwd" :
            self.shUtil.pwd()
        elif cmd[0] == "ls" :
            if len(cmd) == 1 : cmd.append("*")
            self.shUtil.ls(cmd[1:])
        elif cmd[0] == "lsd" :
            self.shUtil.lsd()
        elif cmd[0] == "find":
            self.shUtil.find(cmd[1:])
        elif cmd[0] == "edit":
            self.shUtil.edit(cmd[1:])
            
        elif cmd[0] == "bmadd" :
            self.shUtil.bmadd(True)
        elif cmd[0] == "bmlist" :
            self.shUtil.bmlist(True)
        elif cmd[0] == "bmedit" :
            self.shUtil.bmedit(True)

        elif cmd[0] == "locatedb" :
            self.locatedb(cmd[1:])

        elif cmd[0] == "touch" :
            self.shUtil.touch(cmd[1:])
        elif cmd[0] == "rm" :
            self.shUtil.rm(cmd[1:])
        elif cmd[0] == "mkdir" :
            self.shUtil.mkdir(cmd[1:])
        elif cmd[0] == "rmdir" :
            self.shUtil.rmdir(cmd[1:])
        elif cmd[0] == "cp" :
            self.shUtil.cp(cmd[1:])
        elif cmd[0] == "mv" :
            self.shUtil.mv(cmd[1:])
        elif cmd[0] == "merge" :
            self.shUtil.merge(cmd[1:])
        elif cmd[0] == "cat" :
            self.shUtil.cat(cmd[1:])
        elif cmd[0] == "head" :
            self.shUtil.head(cmd[1:])
        elif cmd[0] == "yankbuffer" :
            self.shUtil.yankbuffer()
        elif cmd[0] == "yank" :
            self.shUtil.yank(cmd[1:])
        elif cmd[0] == "cut" :
            self.shUtil.yank(cmd[1:],mode="cut")
        elif cmd[0] == "yankadd" :
            self.shUtil.yank(cmd[1:],clearBuffer=False)
        elif cmd[0] == "cutadd" :
            self.shUtil.yank(cmd[1:],clearBuffer=False,mode="cut")
        elif cmd[0] == "paste" :
            self.shUtil.paste()
        elif cmd[0] == "updatedb" :
            self.shUtil.updatedb(cmd[1:])
        elif cmd[0] == "locate" :
            self.shUtil.locate(cmd[1:])
        elif cmd[0] == "ledit" :
            self.shUtil.ledit(cmd[1])
        elif cmd[0] == "lgrep" :
            self.shUtil.lgrep(cmd[1:])
        elif cmd[0] == "batch" :
            self.batch(cmd[1:])
        elif cmd[0] == "echo" :
            self.echo(cmd[1:])
        elif cmd[0] == "exit" :
            self.exit()
        elif cmd[0] == "set" :
            self.set(cmd[1:])
        elif cmd[0] == "help" :
            self.help()
        else :
            self.runSysCmd(cmd)

    def saveSession(self):
        work_buffer = vim.current.buffer
        shext_session_file = open(shext_session_path,"w")
        for line in work_buffer :
            shext_session_file.write(line+"\n")
        shext_session_file.close()

    def loadSession(self):
        buffer = vim.current.buffer
        if not os.path.exists(shext_session_path) :
            return
        sessionfile = open(shext_session_path,"r")
        lines = sessionfile.readlines()
        sessionfile.close()

        for index,line in enumerate(lines):
            if index == 0 :
                buffer[0] = line
            else :
                buffer.append(line)

class OutputNavigator(object):

    @staticmethod
    def completeNavLine(ngt_index, start_col):
        outputBuffer = Shext.getOutputBuffer()
        work_buffer = vim.current.buffer
        row,col = vim.current.window.cursor
        start_text = work_buffer[row-1][0:start_col+1]
        work_buffer[row-1] = start_text + outputBuffer[ngt_index].strip()
        length = len(work_buffer[row-1])
        vim.current.window.cursor = ( row , length )
        OutputNavigator.highlightLine(ngt_index + 1,length)

    @staticmethod
    def startNavigate():
        row,col = vim.current.window.cursor
        global shext_ngt_index 
        global start_col 
        shext_ngt_index = 0
        start_col = col 
        work_buffer = vim.current.buffer
        row,col = vim.current.window.cursor
        line = work_buffer[row-1]
        pat=re.compile(".*\s+(?P<var>\$\[(?P<row>\d+)\])$")
        match =pat.match(line)
        if match :
            shext_ngt_index = int(match.group("row")) -1
            start_col = col - len(match.group("var"))
        #vim.current.window.cursor = ( row , start_col )
        OutputNavigator.completeNavLine(shext_ngt_index ,start_col)

    @staticmethod
    def next():
        global shext_ngt_index 
        outputBuffer = Shext.getOutputBuffer()
        if shext_ngt_index >= len(outputBuffer)-1 :
            return
        shext_ngt_index = shext_ngt_index + 1
        OutputNavigator.completeNavLine(shext_ngt_index,start_col)
        
    @staticmethod
    def prev():
        global shext_ngt_index 
        if shext_ngt_index == 0 : return
        shext_ngt_index = shext_ngt_index - 1
        OutputNavigator.completeNavLine(shext_ngt_index,start_col)

    @staticmethod
    def highlightLine(ngt_index,length):
        vim.command("call SwitchToSzToolView('shext')" )
        vim.command("syntax clear")
        vim.command("highlight def MarkCurrentLine  ctermbg=Cyan     ctermfg=Black  guibg=#8CCBEA    guifg=Black")
        valueTuple=(ngt_index,0,length)
        colorInfo="""syn match MarkCurrentLine "\%%%sl\%%>%sc.\%%<%sc" """ % valueTuple
        vim.command(colorInfo)
        
        listwinnr = str(vim.eval("winnr('#')"))
        vim.command("exec '" + listwinnr + " wincmd w'")

class PathResolver(object):

    def __init__(self,shUtil):
        self.shUtil = shUtil

    def resolve(self,path):
        path = path.strip()
        if path.startswith("~"):
            abspath = os.path.join(os.path.expanduser("~"),path[1:])
            path = abspath
        else :
            abspath = os.path.join(os.getcwd(),path)
        if os.path.exists(abspath):
            return [abspath]

        filelist = glob.glob(path)
        if len(filelist) > 0 :
            return filelist 

        if "/" in path :
            tmpPath = path[0:path.find("/")]
            rtlPath = path[path.find("/")+1:]
            abspath = os.path.join(self.shUtil.getbm(tmpPath),rtlPath)
        else :
            abspath = self.shUtil.getbm(path)
        if os.path.exists(abspath):
            return [abspath]

        filelist = glob.glob(abspath)
        if len(filelist) > 0 :
            return filelist 

        return [path]
