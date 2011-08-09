import vim,os,re,sys,random
from subprocess import Popen, PIPE
import subprocess
import logging
import shutil
import socket
from StringIO import StringIO
from distutils import dir_util
from distutils import file_util
import zipfile

HOST = 'localhost'
PORT = 9527
END_TOKEN = "==end=="


class ZipUtil(object):
    @staticmethod
    def read_zip_cmd():
        path = vim.current.buffer.name
        inner_path = path.split("!")[1]
        vim.command("silent doau BufReadPre " + inner_path)
        content = ZipUtil.read_zip_entry(path)
        output(content)
        vim.command("silent doau BufReadPost " + inner_path)
        vim.command("silent doau BufWinEnter " + inner_path)

    @staticmethod
    def read_zip_entry(path):
        zip_file_path, inner_path = ZipUtil.split_zip_scheme(path)
        zipFile = zipfile.ZipFile(zip_file_path)  
        content = [line.rstrip() for line in zipFile.open(inner_path).readlines()]
        zipFile.close()
        return content

    @staticmethod
    def split_zip_scheme(path):
        path = path.replace("\\","/")
        path = path[path.find("://")+3 : ]
        zip_file_path, inner_path = path.split("!")
        return zip_file_path, inner_path

class FileUtil(object):
    @staticmethod
    def fileOrDirCp(src,dst):
        if os.path.isdir(src):
            dst = os.path.join(dst,os.path.basename(src)) if os.path.exists(dst) else dst
            dir_util.copy_tree(src , dst)
        else:
            file_util.copy_file(src, dst)

    @staticmethod
    def fileOrDirMv(src,dst):
        if os.path.isdir(src):
            dst = os.path.join(dst,os.path.basename(src))  if os.path.exists(dst) else dst
            shutil.move(src, dst)
        else:
            shutil.move(src, dst)

    @staticmethod
    def fileOrDirRm(src):
        if os.path.isdir(src):
            shutil.rmtree(src)
        else:
            os.remove(src)

class FuzzyCompletion(object):

    @staticmethod
    def completion(findstart,base):
        if str(findstart)=="1":
            (row,col)=vim.current.window.cursor
            line=vim.current.buffer[row-1]

            index=0
            for i in range(col-1,-1, -1):
                char=line[i]
                if char == " " or char==";" or char=="," or char=="." or char =="'":
                    index=i+1
                    break

            cmd="let g:FuzzyCompletionIndex=%s" %str(index)
        else:
            result=FuzzyCompletion.getCompletionList(base)
            liststr="['"+ "','".join(result)+"']"
            cmd="let g:FuzzyCompletionResult=%s" % liststr
        vim.command(cmd)

    @staticmethod
    def getCompletionList(base):
        bufferTexts=[]
        for buffer in vim.buffers :
            bufferTexts.append("\n".join([unicode(line) for line in buffer]))
        allText="\n".join(bufferTexts)
        if base.find("*") > -1 :
            pattern = unicode(r"\b%s\b" % base.replace("*","[a-zA-Z0-9-_]*") )
        else :
            pattern = unicode(r"\b%s[a-zA-Z0-9-_]*\b" % base)
        matches=re.findall(pattern,allText)
        completeList = []
        if matches :
            for item in matches :
                if item not in completeList :
                    completeList.append(item)
        return completeList

class SztoolAgent(object):

    @staticmethod
    def agentHasStarted() :
        agentStarted = True
        s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        try :
            s.connect((HOST, PORT))
            s.close()
        except Exception , e :
            agentStarted = False
        return agentStarted

    @staticmethod
    def startAgent():

        if SztoolAgent.agentHasStarted() : return

        sztool_home = vim.eval("g:sztool_home")
        libpath = os.path.join(sztool_home,"lib")
        cmdArray=["java"]
        cps=[os.path.join(libpath,item) for item in os.listdir(libpath) if item.endswith(".jar") ]
        if os.name == "nt" :
            swtLibPath = os.path.join(libpath,"swt-win\\swt.jar")
        else :
            swtLibPath = os.path.join(libpath,"swt-linux/swt.jar")
        cps.append(swtLibPath)
        toolsJarPath = os.path.join(os.getenv("JAVA_HOME"),"lib/tools.jar")
        cps.append(toolsJarPath)
        cmdArray.append("-classpath")
        cmdArray.append(os.path.pathsep.join(cps))
        cmdArray.append('-Djava.library.path=%s' % libpath )
        cmdArray.append("com.google.code.vimsztool.ui.JdtUI")
        cmdArray.append("--sztool-home")
        cmdArray.append(sztool_home)


        if os.name == "posix" :
            Popen(" ".join(cmdArray),shell = True)
        else :
            Popen(cmdArray,shell = True)

    @staticmethod
    def stopAgent():
        BasicTalker.stopAgent()
        return

def output(content,buffer=None,append=False):

    if buffer == None:
        buffer=vim.current.buffer
    if not append :
        buffer[:]=None
    if content == None :
        return 

    lines=""
    if type(content)==type([]) :
        lines="\n".join([item for item in content])
    else :
        lines=content

    if lines.endswith("\n") :
        lines = lines[:-1]
    rowList = str(lines).split("\n")
    for index,line in enumerate(rowList):  
        if index == 0 and not append :
            buffer[0]=line
        else :
            buffer.append(line)  

class MiscUtil(object):
    @staticmethod
    def startfile():
        """ invoke os.startfile to open the file under the cursor"""
        (row, col) = vim.current.window.cursor  
        line = vim.current.buffer[row-1]  
        os.startfile(line)

    @staticmethod
    def openInFirefox():
        """ open current editing file in firefox browser """
        import subprocess
        cmd='firefox "%s"' %(vim.current.buffer.name)
        print cmd
        p=subprocess.Popen(cmd,shell=False)

    @staticmethod
    def simpleFormatSQL():
        vr=vim.current.range
        sql=""
        for line in vr:
            sql=sql+line+" "

        vr[:]=None
        sql=sql[:-1]

        start=vr.start

        p1=re.compile(r"(\binsert\b|\bselect\b|\bupdate\b|\bdelete\b)",re.IGNORECASE)
        p2=re.compile(r"(\bfrom\b|\bwhere\b|\band\b|\bor\b)",re.IGNORECASE)

        sql=p1.sub(r"\n\1",sql)
        sql=p2.sub(r"\n\1",sql)
        for line in sql.split("\n") :
            if line!="" : vr.append(line)

        vim.current.window.cursor=(start+1,0)

    @staticmethod
    def getVisualBlock():
        vb=vim.eval("GetVisualBlock()")
        lines=vb.split("\n")
        sql=""
        for line in lines:
          sql=sql+line+" "
        return sql.strip()

    @staticmethod
    def transform(value, method):
        global incValue
        global digit_len
        global search_pat

        if method == "setter" :
            tr_result = "set%s();" % value.capitalize()

        if method == "getter" :
            tr_result = "get%s();" % value.capitalize()
            
        if method == "inc" :

            tmp_search_pat=vim.eval("@/")
            if incValue == 0 or tmp_search_pat != search_pat :
                match=digit_pat.search(value)
                digit_value = match.group()
                digit_len = len(digit_value)
                tmpValue=int(digit_value)
                incValue = tmpValue
                search_pat = tmp_search_pat
            else :
                incValue += 1
            tr_result = digit_pat.sub(str(incValue).rjust(digit_len,'0'),value)

        vim.command("let g:sztransform_result='%s'" % tr_result )

    @staticmethod
    def watchExample(name):
        examples_dir = os.path.join(SzToolsConfig.getShareHome(),"examples")
        content = None
        example_file=None
        for file_name in os.listdir(examples_dir):
            if file_name.find(name) > -1 :
                example_file = os.path.join(examples_dir , file_name)
                break
        if example_file :
            example_file.replace(" ","\ ")
            vim.command("exec 'silent! belowright split %s '" % example_file)

    @staticmethod
    def displayWidth(value):
        if value == None : return 0
        return int(vim.eval("strdisplaywidth('%s')" % value))

    @staticmethod
    def getVisualArea():
        startCol=int(vim.eval("""col("'<")"""))-1
        endCol=int(vim.eval("""col("'>")"""))+1
        startLine=int(vim.eval("""line("'<")"""))
        endLine=int(vim.eval("""line("'>")"""))
        return [startCol,endCol,startLine,endLine]

    @staticmethod
    def getVisualSynCmd(area=None):

        if area == None :
            startCol,endCol,startLine,endLine=MiscUtil.getVisualArea()
        else :
            startCol,endCol,startLine,endLine=area

        cmds=[]
        if endLine > startLine :
            randValue=random.randint(1,6)
            for i in range(1,(endLine- startLine)):
                valueTuple=(randValue, str(startLine+i), 0, 200)
                colorInfo="""syn match MarkWord%s "\%%%sl\%%>%sc.\%%<%sc" """ % valueTuple
                cmds.append(colorInfo)
            valueTuple=(randValue, startLine,startCol,200)
            colorInfo="""syn match MarkWord%s "\%%%sl\%%>%sc.\%%<%sc" """  % valueTuple
            cmds.append(colorInfo)
            valueTuple=(randValue, endLine,0,endCol)
            colorInfo="""syn match MarkWord%s "\%%%sl\%%>%sc.\%%<%sc" """ % valueTuple
            cmds.append(colorInfo)
        else :
            valueTuple=(random.randint(1,6), startLine,startCol,endCol)
            colorInfo="""syn match MarkWord%s "\%%%sl\%%>%sc.\%%<%sc" """ % valueTuple
            cmds.append(colorInfo)
        return cmds

    @staticmethod
    def tabulate():
        startCol,endCol,startLine,endLine=MiscUtil.getVisualArea()
        buffer=vim.current.buffer
        pat = re.compile("\s+")
        rows = []
        for row in buffer[startLine-1:endLine]:
            fields = pat.split(row)
            rows.append(fields)
        result = []
        maxlens = [0] * len(rows[0])
        for row in rows :
            for index,field in enumerate(row):
                field = str(field).rstrip()
                if (len(field)>maxlens[index]):
                    maxlens[index] = len(field)
        headline = ""
        for item in maxlens:
            headline = headline + "+" + ("-"*item) + "--"
        headline = headline+ "+" 

        for rowindex,row in enumerate(rows):
            line = ""
            for index,field in enumerate(row):
                field = str(field).rstrip().replace("\n","")
                line = line+ "| " + field.ljust(maxlens[index] + 1)
            if rowindex<2: result.append(headline)
            result.append(line + "|")
        result.append(headline)
        del buffer[startLine-1:endLine]
        for line in result[::-1] :
            vim.command("call append(%s,'%s')" %(str(startLine-1),line))

    @staticmethod
    def initHightLightScheme():
        vim.command("highlight def MarkWord1  ctermbg=Cyan     ctermfg=Black  guibg=#8CCBEA    guifg=Black")
        vim.command("highlight def MarkWord2  ctermbg=Green    ctermfg=Black  guibg=#A4E57E    guifg=Black")
        vim.command("highlight def MarkWord3  ctermbg=Yellow   ctermfg=Black  guibg=#FFDB72    guifg=Black")
        vim.command("highlight def MarkWord4  ctermbg=Red      ctermfg=Black  guibg=#FF7272    guifg=Black")
        vim.command("highlight def MarkWord5  ctermbg=Magenta  ctermfg=Black  guibg=#FFB3FF    guifg=Black")
        vim.command("highlight def MarkWord6  ctermbg=Blue     ctermfg=Black  guibg=#9999FF    guifg=Black")

    @staticmethod
    def markVisual():
        if (not markSchemeInited ) :
            MiscUtil.initHightLightScheme()
        for vimCmd in MiscUtil.getVisualSynCmd():
            vim.command(vimCmd)

    @staticmethod
    def startMailAgent():

        sztool_home = vim.eval("g:sztool_home")
        mail_app_path = os.path.join(sztool_home,"python/mailext.py")
        print mail_app_path
        print sztool_home
        try :
            os.spawnlp(os.P_NOWAIT,"python","python", mail_app_path, "-p", sztool_home)
        except Exception as e :
            logging.debug(e)
            logging.debug("start ageng error")

    @staticmethod
    def remove_comment():
        def replacer(match):
            s = match.group(0)
            if s.startswith('/'):
                return ""
            else:
                return s
        pattern = re.compile(
            r'//.*?$|/\*.*?\*/|\'(?:\\.|[^\\\'])*\'|"(?:\\.|[^\\"])*"',
            re.DOTALL | re.MULTILINE
        )
        text = "\n".join(vim.current.buffer)
        text = re.sub(pattern, replacer, text)
        output(text)

class ScratchUtil(object):

    @staticmethod
    def startScriptEdit():

        vim.command("call SwitchToSzToolView('script')")    
        vim.command("map <buffer><silent>,, :python ScratchUtil.runScript()<cr>")
        vim.command("set filetype=python")
        vim.command("set bufhidden=delete")
        vim.command("autocmd BufLeave <buffer>  python ScratchUtil.saveScratchText()")

        buffer_name=vim.current.buffer.name
        if not scratch_buf :
            template=[]
            template.append("import vim")
            template.append("buffer=VimUtil.getLastBuffer()")
            output(template)
        else :
            output(scratch_buf)
        return

    @staticmethod
    def saveScratchText():
        global scratch_buf
        scratch_buf = [] 
        for line in vim.current.buffer :
            scratch_buf.append(line.replace("\n",""))

    @staticmethod
    def printScriptResult(result):
        """ output to result to a temp vim buffer named "scriptResult" """
        vim.command("call SwitchToSzToolView('scriptResult')")    
        output(result)

    @staticmethod
    def runScript():
        script="\n".join([line for line in vim.current.buffer])
        exec script

class DictUtil(object):
    @staticmethod
    def playWordSound(word):
        initial_char=word[0]
        word_tts_path=sztoolsCfg.get("word_tts_path")
        word_player = sztoolsCfg.get("word_player")
        word_dir=os.path.join(word_tts_path,initial_char)
        word_file=os.path.join(word_dir,word+".wav")
        if os.path.exists(word_file):
            pid = Popen([word_player + " " +word_file],shell=True).pid
        else :
            #try some simple variations
            if word.endswith("ing") :
                word_file=os.path.join(word_dir, word[:-3]+".wav")
            if word.endswith("ed") or word.endswith("es") :
                word_file=os.path.join(word_dir, word[:-2]+".wav")
            if word.endswith("s") :
                word_file=os.path.join(word_dir, word[:-1]+".wav")
            if os.path.exists(word_file):
                pid = Popen([word_player +" " +word_file],shell=True).pid

    @staticmethod
    def getWordDef(stardict, word):
        word=word.lower()
        result = stardict.get(word)
        if not result :
            defs=[]
            #in dreye 4in1 dictionary, some words like "row" stores in
            # "row .1", "row .2" format
            for i in range(1,4):
                tmpword=word+" ."+str(i)
                section=stardict.get(tmpword)
                if not section:
                    break
                defs.append(section)
            if len(defs) > 0 : result="\n".join(defs)
        return result

    @staticmethod
    def searchDict(word):
        global stardict
        VimUtil.createOutputBuffer("dict")
        outbuffer=getOutputBuffer("dict")
        dict_path = os.path.join(SzToolsConfig.getDataHome(), "dict")
        if not stardict :
            stardict = Dictionary(os.path.join(dict_path, 'stardict-dreye', 'DrEye4in1'))
        result=DictUtil.getWordDef(stardict, word)
        if not result :
            if word.endswith("ed") :
                word=word[0:-2]
            elif word.endswith("s") :
                word=word[0:-1]
            result = DictUtil.getWordDef(stardict,word)
            if not result :
                result = "can't find the word definition"
        else :
            dict_search_log_path = os.path.join(SzToolsConfig.getDataHome(), "dict/log.txt")
            log_file=open(dict_search_log_path,"aw")
            log_file.write(word+"\n")
            log_file.close()
        result=unicode(result,"utf-8")
        codepage=sys.getdefaultencoding()
        result=result.encode(codepage,"replace")
        output(result,outbuffer)

class SzToolsConfig(object):

    def _loadCfg(self,cfg_path):
        if not os.path.exists(cfg_path):
            return
        lines = open(cfg_path,"r").readlines()
        for line in lines:
            if not line.strip() : continue
            if line[0] == "#" : continue
            split_index= line.find ("=")
            if split_index < 0 : continue 
            key = line[0:split_index].strip()
            value = line[split_index+1:].strip()
            self.cfg_dict[key] = value
    
    def __init__(self,cfg_path):
        self.cfg_dict={}
        user_home = os.path.expanduser('~')
        user_cfg_path=os.path.join(user_home , ".sztools.cfg")
        self._loadCfg(cfg_path)
        self._loadCfg(user_cfg_path)
        
    def get(self,name):
        return self.cfg_dict.get(name,"")

    @staticmethod
    def getAppHome():
        sztool_home=vim.eval("g:sztool_home")
        sztool_app_home=os.path.join(sztool_home,"python")
        return sztool_app_home

    @staticmethod
    def getDataHome():
        user_home = os.path.expanduser('~')
        sztool_data_home=os.path.join(user_home,".sztools")
        return sztool_data_home

    @staticmethod
    def getShareHome():
        sztool_home=vim.eval("g:sztool_home")
        sztool_data_home=os.path.join(sztool_home,"share")
        return sztool_data_home

def initSztool():
    data_home = SzToolsConfig.getDataHome()
    if not os.path.exists(data_home):
        os.mkdir(data_home)
    log_filename = os.path.join(data_home, "sztools.log")
    if not os.path.exists(log_filename) :
        open(log_filename,"w").close()
    logging.basicConfig(filename=log_filename,level=logging.DEBUG)

    gscope=globals()
    gscope["incValue"] = 0
    gscope["digit_pat"] = re.compile("\d+")
    gscope["search_pat"] = None
    gscope["stardict"] = None
    gscope["scratch_buf"] = []
    gscope["markSchemeInited"] = False
    gscope["sztoolsCfg"]=SzToolsConfig(os.path.join(SzToolsConfig.getShareHome(),"conf/sztools.cfg"))

    #append app path to sys.path
    import sys
    sys.path.append(SzToolsConfig.getAppHome())

def fetchCallBack(args):
    guid,bufname = args
    resultText = BasicTalker.fetchResult(guid)
    lines = resultText.split("\n")
    VimUtil.writeToSzToolBuffer(bufname,lines,append=True)

class BasicTalker(object):

    @staticmethod
    def send(params):
        s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        s.connect((HOST, PORT))
        sb = []
        for item in params :
            sb.append("%s=%s\n" %(item,params[item]))
        sb.append('%s\n' % END_TOKEN)
        s.send("".join(sb))
        if params.get("cmd") == "quit" :
            return 
        total_data=[]
        while True:
            data = s.recv(8192)
            if not data: break
            total_data.append(data)
        s.close()
        return ''.join(total_data)

    @staticmethod
    def stopAgent():
        params = dict()
        params["cmd"]="quit"
        data = BasicTalker.send(params)
        return data

    @staticmethod
    def runSys(vimServer,cmdName,runInShell,bufname,workDir):
        params = dict()
        params["cmd"]="runSys"
        params["vimServer"] = vimServer
        params["cmdName"] = cmdName
        params["runInShell"] = runInShell
        params["bufname"] = bufname
        params["workDir"] = workDir
        data = BasicTalker.send(params)
        return data

    @staticmethod
    def fetchResult(guid):
        params = dict()
        params["cmd"]="fetchResult"
        params["jobId"] = guid
        data = BasicTalker.send(params)
        return data

    @staticmethod
    def doLocatedbCommand(args):
        params = dict()
        params["cmd"]="locatedb"
        params["args"] = ";".join(args)
        params["pwd"] = os.getcwd()
        data = BasicTalker.send(params)
        return data

class VimUtil(object):

    @staticmethod
    def getInput(prompt,default_text = ""):
        vim.command("redraw")
        vim.command("let b:vjde_user_input = input('%s','%s')" % (prompt,default_text))
        exists = vim.eval("exists('b:vjde_user_input')")
        result = None
        if exists  ==  "1" :
            result = vim.eval("b:vjde_user_input")
            vim.command("unlet b:vjde_user_input")
        return result

    @staticmethod
    def inputOption(options):
        vim.command("redraw")
        for index,line in enumerate(options) :
            print " %s : %s " % (str(index), line)
        vim.command("let b:vjde_option_index = input('please enter a selection')")
        exists = vim.eval("exists('b:vjde_option_index')")
        if exists  ==  "1" :
            index = vim.eval("b:vjde_option_index")
            vim.command("unlet b:vjde_option_index")
        else :
            index = None
        return index

    @staticmethod
    def getSzToolBuffer(name, createNew = True):
        def _getConsoleBuffer():
            jde_console_buf = None
            for buffer in vim.buffers:
                if buffer.name and buffer.name.find( "SzToolView_%s" % name) > -1 :
                    jde_console_buf = buffer
                    break
            return jde_console_buf
        buf = _getConsoleBuffer()
        if buf == None and createNew :
            vim.command("call SwitchToSzToolView('%s')" % name )
            listwinnr=str(vim.eval("winnr('#')"))
            vim.command("exec '%s wincmd w'" % listwinnr)
            buf = _getConsoleBuffer()

        return buf

    @staticmethod
    def writeToSzToolBuffer(name, text, append=False):
        if not text : return
        buf = VimUtil.getSzToolBuffer(name)

        if type(text) == type(""):
            lines = text.split("\n")
        else :
            lines = text
        output(lines,buf, append)
        if append and VimUtil.isSzToolBufferVisible(name):
            endrow = len(buf)
            callback = lambda : VimUtil.scrollTo(endrow)
            VimUtil.doCommandInSzToolBuffer(name,callback)

    @staticmethod
    def closeSzToolBuffer(name):
        bufnr = vim.eval("bufnr('SzToolView_%s')" % name)    
        if bufnr == "-1" :
            return 
        vim.command("bd %s" % bufnr)

    @staticmethod
    def scrollTo(lineNum):
        winStartRow = int(vim.eval("line('w0')"))
        winEndRow = int(vim.eval("line('w$')"))
        lineNum = int(lineNum)
        if lineNum < winStartRow or lineNum > winEndRow :
            vim.command("normal %sG" % str(lineNum))
            vim.command("normal z-")

    @staticmethod
    def isSzToolBufferVisible(name):
        bufnr = vim.eval("bufnr('SzToolView_%s')" % name)    
        visibleBufList = vim.eval("tabpagebuflist()")
        if bufnr in visibleBufList :
            return True
        return False

    @staticmethod
    def doCommandInSzToolBuffer(bufName, callback):
        vim.command("call SwitchToSzToolView('%s')" % bufName )
        callback()
        listwinnr = str(vim.eval("winnr('#')"))
        vim.command("exec '"+listwinnr+" wincmd w'")

    @staticmethod
    def getLastBuffer():
        """get last edit vim buffer """
        listwinnr=str(vim.eval("winnr('#')"))
        vim.command("exec '%s wincmd w'" % listwinnr)
        buffer=vim.current.buffer
        listwinnr=str(vim.eval("winnr('#')"))
        vim.command("exec '%s wincmd w'" % listwinnr)
        return buffer

    @staticmethod
    def createOutputBuffer(name):
        """ create output vim buffer ,the created buffer is 
        a temp buffer, flowing flags has been set.

        setlocal nowrap"    
        setlocal buftype=nofile" 
        setlocal noswapfile"
        setlocal bufhidden=wipe"
        setlocal nobuflisted"

        """
        vim.command("call SwitchToSzToolView('%s')" % name )
        listwinnr=str(vim.eval("winnr('#')"))
        vim.command("exec '%s wincmd w'" % listwinnr)

    @staticmethod
    def getOutputBuffer(name):
        shext_buffer=None
        for buffer in vim.buffers:
            if buffer.name and "SzToolView_%s" % name in buffer.name :
                shext_buffer=buffer
                break
        return shext_buffer

initSztool()
