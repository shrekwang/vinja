import os,os.path,fnmatch,shutil,shlex
import glob,time,vim,re, sys, logging
from subprocess import Popen, PIPE
from optparse import OptionParser 
import sqlite3 as sqlite
import chardet
from datetime import timedelta, datetime ,date


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

    def __init__(self, screen_width, pathResolver) :
        self.cmp_types = ["zip","rar","tgz","tar","jar","war","gz"]
        self.exe_types = ["exe","bat","sh"]
        self.col_pad = 3
        self.max_column = 10
        self.file_types = {}
        self.colorInfos = []
        self.screen_width = screen_width
        self.pathResolver = pathResolver

    def colorOutput(self):
        vim.command("call SwitchToVinjaView('shext')" )
        vim.command("syntax clear")
        for item in self.colorInfos :
            vim.command(item)

        dir_color = vinja_cfg.get("dir_color")
        exe_color = vinja_cfg.get("exe_color")
        cmp_color = vinja_cfg.get("cmp_color")

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
            max = MiscUtil.displayWidth(allValue[0][i])
            for j in range(rows):
                if MiscUtil.displayWidth(allValue[j][i]) > max : max = MiscUtil.displayWidth(allValue[j][i])
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
            nnlist.append("".join([col+((col_max_widths[index] - MiscUtil.displayWidth(col))*" ") for index,col in enumerate(item)]))
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

        abspath = os.getcwd()
        file_list = self.pathResolver.resolve(pathname, True)
        if len(file_list) > 0 :
            abspath = os.path.dirname(os.path.normpath(os.path.join(os.getcwd(),file_list[0])))
            file_list = [os.path.basename(item) for item in file_list]

        file_list = self.sort_file(abspath, file_list, options)
        self.extract_file_typeinfo(abspath, file_list)
            
        if options.longformat :
            nlist = self.getLongFormatInfo(abspath, file_list)
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

        class TypeFilter(object):
            def __init__(self, typeName) :
                self.typeName = typeName
            def accepted(self, filename, filepath ):
                abPath = os.path.join(filepath, filename)
                if self.typeName == "dir" and os.path.isdir(abPath) :
                    return True
                if self.typeName == "file" and os.path.isfile(abPath):
                    return True
                return False

        class NameFilter(object):
            def __init__(self, filter, includeDot ):
                self.filter = filter
                self.includeDot = includeDot

            def accepted(self, filename, filepath) :
                if filename.startswith(".") and not self.includeDot :
                    return False
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
            def __init__(self, content, encoding_param):
                self.content = content
                self.encoding_param = encoding_param
            def accepted(self, filename, filepath):
                abPath = os.path.join(filepath, filename)
                if os.path.isdir(abPath) or not os.path.exists(abPath) :
                    return False
                file_object = open(abPath,"r")
                try:
                    all_the_text = file_object.read()
                finally:
                    file_object.close()

                # only do decode when there's a encoding param 
                if self.encoding_param :
                    if self.encoding_param == "auto" :
                        file_encoding = chardet.detect(all_the_text).get("encoding")
                    else :
                        file_encoding = self.encoding_param
                    if file_encoding != None :
                        all_the_text = all_the_text.decode(file_encoding, "ignore")
                if all_the_text.find(self.content) > -1 :
                    return True
                return False

        class MtimeFilter(object):

            def __init__(self, timeDesc):

                if timeDesc.startswith("-") :
                    self.operator = "GT"
                    timeDesc = timeDesc[1:]
                elif timeDesc.startswith("+") :
                    self.operator = "LT"
                    timeDesc = timeDesc[1:]
                else :
                    self.operator = "EQ"
                delta = int(timeDesc[0:-1])
                deltaUnit = timeDesc[-1]
                now = datetime.now()
                self.cmpDate = None
                if deltaUnit == "m" :
                    self.cmpDate = now - timedelta(minutes=delta)
                elif deltaUnit == "h":
                    self.cmpDate = now - timedelta(hours=delta)
                elif deltaUnit == "d":
                    self.cmpDate = now - timedelta(days=delta)

            def accepted(self, filename, filepath):
                abPath = os.path.join(filepath, filename)
                mtime = datetime.fromtimestamp(os.path.getmtime(abPath))
                if self.operator == "GT" and mtime > self.cmpDate :
                    return True
                elif self.operator =="LT" and mtime < self.cmpDate :
                    return True
                else :
                    endOfCmpDay = self.cmpDate + timedelta(days=1)
                    if (mtime > self.cmpDate and mtime < endOfCmpDay ) :
                        return True

        Shext.stdout("")

        parser = ShextOptionParser( usage="usage: find [paths] [options]")
        parser.add_option("-n","--name",action="store", dest="name", help= "simple pattern for matching file name")
        parser.add_option("-t","--text",action="store", dest="text", help = "simple pattern for matching file content")
        parser.add_option("-c","--type",action="store", dest="type", help="colud be 'dir' or 'file'")
        parser.add_option("-s","--size",action="store", dest="size", help ="not implement yet.")
        parser.add_option("-p","--path",action="store", dest="path", help="regex pattern for matching full file path")
        parser.add_option("-e","--encoding",action="store", dest="encoding" , help="could be encoding name like 'gbk' or just 'auto' " )
        parser.add_option("-m","--mtime",nargs=1,action="store", dest="mtime")
        parser.add_option("-a","--include-dot",action = "store_true", dest="includeDot", default=False)

        (options, args) = parser.parse_args(cmd_array)
        if parser.noAction : return 
        if len(args) > 0 : 
            search_paths = args
        else :
            search_paths = ["."]
        
        filters = []
        if options.name :
            filters.append(NameFilter(options.name,options.includeDot))
        else :
            filters.append(NameFilter("*",options.includeDot))

        if options.path:
            filters.append(PathFilter(options.path))
        if options.size:
            filters.append(SizeFilter(options.size))
        if options.text :
            filters.append(ContentFilter(options.text,options.encoding))
        if options.type :
            filters.append(TypeFilter(options.type))
        if options.mtime :
            filters.append(MtimeFilter(options.mtime))

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
            self._match_one_file(filename, search_path, filters)
            if os.path.isdir(abspath) :
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

class JdeUtilCmd(object):

    def do_command(self,cmd_array):
        if cmd_array[0] == "start" : 
            self.jde_start()
        elif cmd_array[0] == "help" :
            self.jde_help()
        elif cmd_array[0] == "project" :
            self.do_project_command(cmd_array[1:])
        elif cmd_array[0] == "title" :
            vim.command("let g:vinja_title = \"%s\"" % cmd_array[1])
            vim.command("let &titlestring = MyTitleString()")
        else :
            Shext.stdout("not recognized jde command,please run 'jde help' .")
            return

    def do_project_command(self, cmd_array):
        if cmd_array[0] == "init" :
            self.jde_create_project()
            return

        if not self._jde_has_started() : return

        if cmd_array[0] == "index" :
            Shext.stdout("not implement yet.")

        elif cmd_array[0] == "loadjar" :
            Shext.stdout("")
            classPathXml = os.path.join(os.getcwd(),".classpath")
            if not os.path.exists(classPathXml):
                Shext.stdout("no .classpath file in current dir, not a valid project")
                Shext.stdout("( jde project loadjar finished.)", True)
                return
            Talker.loadJarMeta(classPathXml)

        elif cmd_array[0] == "classpath" :
            Shext.stdout("")
            classPathXml = os.path.join(os.getcwd(),".classpath")
            if not os.path.exists(classPathXml):
                Shext.stdout("no .classpath file in current dir, not a valid project")
                return
            Talker.printClassPath(classPathXml)

        elif cmd_array[0] == "clean" :
            Shext.stdout("")
            classPathXml = os.path.join(os.getcwd(),".classpath")
            if not os.path.exists(classPathXml):
                Shext.stdout("no .classpath file in current dir, not a valid project")
                Shext.stdout("( jde project clean finished.)", True)
                return
            Talker.projectClean(classPathXml)

        elif cmd_array[0] == "build" :
            Shext.stdout("start building....\n")
            classPathXml = os.path.join(os.getcwd(),".classpath")
            if not os.path.exists(classPathXml):
                Shext.stdout("no .classpath file in current dir, not a valid project")
                Shext.stdout("( jde project build finished.)", True)
                return
            current_file_name = "All"
            Talker.compileFile(classPathXml,current_file_name)

        else :
            Shext.stdout("not recognized jde command.")
            return

    def jde_help(self):
        help_text =[
                "jde start ---> start jde mode.",
                "jde stop  ---> no, you can't stop jde mode unless you quit vim :)" ,
                "jde help  ---> print this help.",
                "jde title [name]    --> change vim title",
                "jde project init    --> create .classpath file and src dir.",
                "jde project build   --> build current project." ,
                "jde project clean   --> reload classpath and classinfo in current project." ,
                "jde project loadjar --> load classinfo in jar package." ,
                "jde project index   --> not implement yet." ]
        Shext.stdout(help_text)

    def _jde_has_started(self):
        if vim.eval("exists(':Jdb')") != "2":
            Shext.stdout("jde mode not started yet. please run 'jde start'.")
            return False
        return True

    def jde_start(self):
        if self._jde_has_started() : 
            Shext.stdout("jde mode has started already.")
            return
        vim.command("call Jdext()")
        Shext.stdout("jde mode started.")

    def jde_create_project(self):
        if not self._jde_has_started() : return
        classPathXml = os.path.join(os.getcwd(),".classpath")
        if os.path.exists(classPathXml):
            Shext.stdout("project already exists.")
            return
        ProjectManager.projectInit()
        Shext.stdout("project created.")


class ZipUtilCmd(object):

    def do_command(self,cmd_array):
        parser = ShextOptionParser()
        parser.add_option("-c","--cmd",action="store", dest="cmd" , help ="cmd {unzip,zip,list,find}" )
        parser.add_option("-t","--text",action="store", dest="text" , help ="find text pattern in zip" )
        (options, args) = parser.parse_args(cmd_array)
        if parser.noAction :
            return 
        zip_file_path = args[0]
        if not options.cmd in ["list","zip","unzip","find"] :
            Shext.stdout("not valid command")
            return

        if options.cmd == "list" :
            self.list_content(zip_file_path)
        elif options.cmd == "find" :
            self.find_text(zip_file_path,options.text)

    def list_content(self,zip_file_path):
        zipFile = zipfile.ZipFile(zip_file_path)  
        infolist = zipFile.infolist()
        content = []
        for info in infolist :
            content.append("%s %s %s" % (info.file_size, info.date_time, info.filename))
        Shext.stdout(content)

    def find_text(self,zip_file_path, pat):
        zipFile = zipfile.ZipFile(zip_file_path)  
        namelist = zipFile.namelist()
        result = []
        for name in namelist :
            content = "".join(zipFile.open(name).readlines())
            if pat in content :
                result.append(name)
        if len(result) > 0 :
            Shext.stdout(result)
        else :
            Shext.stdout("can't find any entry contains text " + pat)

class ShUtil(object):

    def __init__(self,screen_width,yank_buffer):
        self.findcmd = FindCmd()
        self.shext_bm_path = os.path.join(VinjaConf.getDataHome(), "shext-bm.txt")
        if not os.path.exists(self.shext_bm_path) :
            open(self.shext_bm_path, 'w').close()
        shext_locatedb_path = os.path.join(VinjaConf.getDataHome(), "locate.db")
        self.locatecmd = LocateCmd(shext_locatedb_path)
        self.zipcmd = ZipUtilCmd()
        self.jdecmd = JdeUtilCmd()
        self.yank_buffer = yank_buffer
        self.cd_history = [os.getcwd()]
        self.pathResolver = PathResolver(self)
        self.lscmd = LsCmd(screen_width,self.pathResolver)

    def ls(self,cmd_array):
        self.lscmd.ls(cmd_array)

    def dozip(self,cmd_array):
        self.zipcmd.do_command(cmd_array)

    def dojde(self,cmd_array):
        self.jdecmd.do_command(cmd_array)

    def updatedb(self,cmd_array):
        self.locatecmd.updatedb(cmd_array)

    def locate(self,cmd_array):
        self.locatecmd.locate(cmd_array)

    def lgrep(self,cmd_array):

        parser = ShextOptionParser(usage="usage: lgrep [options] [search-pattern] [filename]")
        parser.add_option("-n","--alias",action="store", dest="alias" , help ="search in designated index entry" )
        parser.add_option("-p","--path", action="store", dest="path" , help = "the path need to match with")
        parser.add_option("-o","--open", action="store_true", dest="open" ,help="open search results in new tab ")
        (options, args) = parser.parse_args(cmd_array)
        if parser.noAction : 
            return 

        if len(args) != 2 : 
            Shext.stdout("incorrect number of arguments.")
            return

        result = self.locatecmd.grep(args[0],args[1],options.alias, options.path)
        if len(result) == 0 :
            return 

        if options.open :
            qflist = []
            first_file_name = result[0][0]
            for filename,lineNum,lineText in result :
                qfitem = dict(filename=str(self.relpath(filename)),lnum=lineNum,text=lineText.strip())
                qflist.append(qfitem)
            vim.command("tabnew")
            vim.command("edit %s" %(first_file_name))
            vim.command("call setqflist(%s)" % qflist)
        else :
            outputText = []
            tmp_file_name = result[0][0]
            outputText.append(tmp_file_name)
            for filename,lineNum,lineText in result :
                if filename != tmp_file_name :
                    tmp_file_name = filename
                    outputText.append("")
                    outputText.append(tmp_file_name)
                outputText.append("   "+lineNum+":" + "  " + lineText.strip())
            Shext.stdout(outputText)

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

            #find a window that's a normal buf (not ProjectTree or other Szjde Window) 
            #to edit the file
            for i in range(1,5):
                vim.command("%swincmd w" % str(i))    
                bufname = vim.current.buffer.name
                if bufname == None :
                    continue
                if vim.eval("&buftype") == "" \
                        or bufname.endswith(".temp_src") \
                        or bufname.endswith(".java") :
                    break
            filename= os.path.basename(name)
            filename = vim.eval("fnameescape(\"%s\")" % filename)
            dirname = os.path.dirname(name)
            name = os.path.join(dirname,filename)
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
        if not os.path.isdir(abspath):
            Shext.stdout("'%s' is not a directory." % path)
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

    def relpath(self, path):
        if path.startswith(os.getcwd()) :
            return os.path.relpath(path)
        else :
            return path

    def mkdir(self,args):
        for arg in args :
            abspath = os.path.join(os.getcwd(),arg)
            os.makedirs(abspath)
        Shext.stdout("dir %s had been created ." % (";".join(args)))

    def rm(self,args):
        count = 0 
        for arg in args :
            names = self.pathResolver.resolve(arg)
            for name in names :
                count += 1
                FileUtil.fileOrDirRm(name)
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
        records = []
        for arg in args[:-1]:
            names = self.pathResolver.resolve(arg)
            for src in names :
                records.append("cp %s to %s" % (self.relpath(src), self.relpath(dst)))
                FileUtil.fileOrDirCp(src,dst)
        records.append("%s items had been copied." % str(len(records)))
        Shext.stdout(records)

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
        records = []
        for mode,src in self.yank_buffer :
            if mode == "yank" : 
                FileUtil.fileOrDirCp(src,dst) 
            else :
                FileUtil.fileOrDirMv(src,dst)
            records.append("%s pasted." % ( self.relpath(src)))
        records.append("%s items had been pasted." % str(len(records)))
        Shext.stdout(records)

    def mv(self,args):
        dst = args[-1]
        records = []
        for arg in args[:-1]:
            names = self.pathResolver.resolve(arg)
            for src in names :
                records.append("mv %s to %s" % (self.relpath(src), self.relpath(dst)))
                FileUtil.fileOrDirMv(src,dst)
        records.append("%s items had been moved." % str(len(records)))
        Shext.stdout(records)
        

    def bmadd(self, bm=False):
        pwd = os.getcwd()
        bm_file_obj = open(self.shext_bm_path,"a")
        basename = os.path.basename(pwd).replace(" ","")
        bm_file_obj.write(basename + " " + pwd +"\n")
        bm_file_obj.close()
        Shext.stdout("the current dir has been added to bookmark.")

    def bmlist(self, bm=False):
        bmfile = self.shext_bm_path 
        lines = [line.rstrip() for line in open(bmfile).readlines()]
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
        VimUtil.createOutputBuffer("shext")
        shext.ls()

    @staticmethod
    def ledit(name):
        shUtil = ShUtil(screen_width=80,yank_buffer=None)
        shUtil.ledit(name,True)
        del shUtil

    @staticmethod
    def getOutputBuffer():
        buf = VimUtil.getOutputBuffer("shext")
        return buf

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
        vim.command("call SwitchToVinjaView('shext')" )
        vim.command("set filetype=")
        vim.command("syntax clear")
        listwinnr = str(vim.eval("winnr('#')"))
        vim.command("exec '"+listwinnr+" wincmd w'")

    def __init__(self):
        self.yank_buffer = []
        self.shUtil = ShUtil(screen_width=80,yank_buffer=self.yank_buffer)
        self.pathResolver = PathResolver(self.shUtil)
        self.special_cmds = ["exit","edit","ledit","bmedit"]
        self.extra_cmd_home= vinja_cfg.get("extra_cmd_home")

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
        #cmdArray = [ item.replace("$$"," ") for item in re.split(r"\s+",cmdLine)]
        cmdLine = cmdLine.replace("\\","/").strip()

        try :
            lexer = shlex.shlex(cmdLine)
            lexer.quotes = '"'
            lexer.commenters = ''
            lexer.wordchars += '\''
            lexer.whitespace_split = True
            cmdArray = list(lexer)
            cmdArray = [ item.replace("$$"," ") for item in cmdArray ]
        except:
            cmdArray = [ item.replace("$$"," ") for item in re.split(r"\s+",cmdLine)]

        result = []
        varPat = re.compile(r".*(#|\$)\[(\d+)(:\d+)?\].*")
        for item in cmdArray :
            #replace the $[1] $[1:2] like variables
            #  $[1] stands for output buffer line 1
            #  #[1] stands for cmd edit buffer line 1
            if varPat.match(item):
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
            if buffer.name and buffer.name.find( "VinjaView_shext") > -1 :
                vim.command("bd! %s" %buffer.name)
        """
        vim.command("bw! VinjaView_shext")
        vim.command("bw!")
        shext = None

    def feedInput(self,cmdLine):
        try :
            serverName = vim.eval("v:servername")
            inputString = cmdLine
            BasicTalker.feedInput(serverName,inputString)
        except (OSError,ValueError) , msg:
            Shext.stdout(msg)

    def runSysCmd(self,cmdArray,cmdLine):
        try :
            cmdArray = self.resolveExtraPythonCmd(cmdArray)
            serverName = vim.eval("v:servername")
            workDir = os.getcwd()
            file_list = self.pathResolver.resolve(cmdArray[0])
            path_resolved_cmdarray = []
            if len(file_list) == 1 and  os.path.isfile(file_list[0]) :
                cmdArray[0] = os.path.abspath(file_list[0])
            for item in cmdArray :
                if "*" in item or "?" in item :
                    path_resolved_cmdarray.extend(self.pathResolver.resolve(item))
                else :
                    path_resolved_cmdarray.append(item)

            runInShell = "true"
            if os.name == "posix" :
                runInShell = "false"
            Shext.stdout("")
            BasicTalker.runSys(serverName,"::".join(path_resolved_cmdarray),runInShell,"shext",workDir,cmdLine)
        except (OSError,ValueError) , msg:
            Shext.stdout(msg)

    def resolveExtraPythonCmd(self, cmdArray):
        if not os.path.exists(self.extra_cmd_home) :
            return cmdArray
        ab_path = os.path.join(self.extra_cmd_home, cmdArray[0]+".py")
        if os.path.exists(ab_path):
            cmdArray.insert(0, "python")
            cmdArray[1] = ab_path
        return cmdArray

    def listExtraCmds(self):
        if not os.path.exists(self.extra_cmd_home) :
            Shext.stdout("no extra_cmd_home difined in vinja.cfg")
            return 
        result = []
        for name in os.listdir(self.extra_cmd_home):
            if name.endswith(".py"):
                ab_path = os.path.join(self.extra_cmd_home, name)
                output = Popen(["python", ab_path, "--desc"], stdout=PIPE, shell = True).communicate()[0]
                result.append(name[0:name.rfind(".")] +" : " )
                result.append(output.replace("\r",""))

        Shext.stdout("\n".join(result))

    def runInBackground(self,cmdline):
        try :
            pid = Popen([cmdline],shell = True).pid
        except (OSError,ValueError) , msg:
            Shext.stdout(msg)

    def help(self):
        help_file = open(os.path.join(VinjaConf.getShareHome(),"doc/vinja.help"))
        content = [line.replace("\n","") for line in help_file.readlines()]
        help_file.close()
        Shext.stdout(content)

    def locatedb(self,args):
        result = BasicTalker.doLocatedbCommand(args)
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
            self.dispatchCmd(cmd,cmdLine)

        resultFileType = self.findCmdFileType(cmd)
        if resultFileType != None :
            vim.command("call SwitchToVinjaView('shext')" )
            vim.command("set filetype=%s" % resultFileType )
            listwinnr = str(vim.eval("winnr('#')"))
            vim.command("exec '"+listwinnr+" wincmd w'")

        if cmd[0] not in self.special_cmds and not batchMode and insertMode :
            vim.command("normal o")
            vim.command("startinsert")

    def findCmdFileType(self, cmd):
        if len(cmd)> 1 and cmd[1] == "diff" :
            return "diff"
        if len(cmd)> 1 and cmd[0] == "xxd" :
            return "xxd"
        if len(cmd)> 1 and cmd[0] == "man" :
            return "man"
        if len(cmd) == 3 and cmd[0] == "jde" and cmd[2] == "build":
            return "builderror"
        if len(cmd) == 3 and cmd[0] == "jde" and cmd[2] == "clean":
            return "builderror"
        return None

    def dispatchCmd(self, cmd,cmdLine) :
        if cmd[0] == "cd" and len(cmd) > 1 :
            path = cmd[1]
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
        elif cmd[0] == "zu" :
            self.shUtil.dozip(cmd[1:])
        elif cmd[0] == "jde":
            self.shUtil.dojde(cmd[1:])

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
        elif cmd[0] == "listext" :
            self.listExtraCmds()
        elif cmd[0] == "feed" :
            self.feedInput(" ".join(cmd[1:]))
        else :
            self.runSysCmd(cmd,cmdLine)

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
        pat=re.compile(".*\s+(?P<var>\$(?P<row>\d+))$")
        match =pat.match(line)
        if match :
            shext_ngt_index = int(match.group("row")) -1
            start_col = col - len(match.group("var"))
        #vim.current.window.cursor = ( row , start_col )
        OutputNavigator.completeNavLine(shext_ngt_index ,start_col)

    @staticmethod
    def next():
        if "shext_ngt_index" not in globals() :
            return
        global shext_ngt_index 
        outputBuffer = Shext.getOutputBuffer()
        if shext_ngt_index >= len(outputBuffer)-1 :
            return
        shext_ngt_index = shext_ngt_index + 1
        OutputNavigator.completeNavLine(shext_ngt_index,start_col)
        
    @staticmethod
    def prev():
        if "shext_ngt_index" not in globals() :
            return
        global shext_ngt_index 
        if shext_ngt_index == 0 : return
        shext_ngt_index = shext_ngt_index - 1
        OutputNavigator.completeNavLine(shext_ngt_index,start_col)

    @staticmethod
    def highlightLine(ngt_index,length):
        vim.command("call SwitchToVinjaView('shext')" )
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

    def resolve(self,path, expand = False):
        path = path.strip()
        if path.startswith("~"):
            rtlpath = "" if len(path) < 2 else path[2:]
            abspath = os.path.join(os.path.expanduser("~"),rtlpath)
            path = abspath
        else :
            abspath = os.path.join(os.getcwd(),path)
        if os.path.exists(abspath):
            if expand :
                return [os.path.join(abspath, item) for item in os.listdir(abspath)]
            else :
                return [abspath]

        filelist = glob.glob(path)
        if len(filelist) > 0 :
            if path.rfind("/") > path.rfind("*") and expand and len(filelist) == 1 and os.path.isdir(filelist[0]) :
                tmp = [os.path.join(filelist[0], item) for item in os.listdir(filelist[0])]
                return tmp
            return filelist 

        if "/" in path :
            tmpPath = path[0:path.find("/")]
            rtlPath = path[path.find("/")+1:]
            abspath = os.path.join(self.shUtil.getbm(tmpPath),rtlPath)
        else :
            abspath = self.shUtil.getbm(path)
        if os.path.exists(abspath):
            if expand :
                return [os.path.join(abspath, item) for item in os.listdir(abspath)]
            else :
                return [abspath]

        filelist = glob.glob(abspath)
        if len(filelist) > 0 :
            if path.rfind("/") > path.rfind("*") and expand and len(filelist) == 1 and os.path.isdir(filelist[0]) :
                tmp = [os.path.join(filelist[0], item) for item in os.listdir(filelist[0])]
                return tmp
            return filelist 

        if expand :
            return []
        else :
            return [path]
