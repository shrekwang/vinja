import vim,os,re,sys,random
from subprocess import Popen, PIPE
import subprocess
import logging
import socket
from StringIO import StringIO

HOST = 'localhost'
PORT = 9527

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

def agentHasStarted() :
    agentStarted = True
    s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    try :
        s.connect((HOST, PORT))
        s.close()
    except Exception , e :
        agentStarted = False
    return agentStarted

def startAgent():

    if agentHasStarted() : return

    sztool_home = vim.eval("g:sztool_home")
    libpath = os.path.join(sztool_home,"lib")
    cmdArray=["java"]
    cps=[os.path.join(libpath,item) for item in os.listdir(libpath) if item.endswith(".jar") ]
    if os.name == "nt" :
        swtLibPath = os.path.join(libpath,"swt-win\\swt.jar")
    else :
        swtLibPath = os.path.join(libpath,"swt-linux/swt.jar")
    cps.append(swtLibPath)
    cmdArray.append("-classpath")
    cmdArray.append(os.path.pathsep.join(cps))
    cmdArray.append('-Djava.library.path=%s' % libpath )
    cmdArray.append("com.google.code.vimsztool.ui.JdtUI")
    cmdArray.append("--sztool-home")
    cmdArray.append(sztool_home)

    logging.debug("cmd array is %s "  % str(cmdArray))

    if os.name == "posix" :
        Popen(" ".join(cmdArray),shell = True)
    else :
        Popen(cmdArray,shell = True)

def strWidth(value):
    if value == None : return 0
    return int(vim.eval("strdisplaywidth('%s')" % value))

def watchExample(name):
    examples_dir = os.path.join(getShareHome(),"examples")
    content = None
    example_file=None
    for file_name in os.listdir(examples_dir):
        if file_name.find(name) > -1 :
            example_file = os.path.join(examples_dir , file_name)
            break
    if example_file :
        example_file.replace(" ","\ ")
        vim.command("exec 'silent! belowright split %s '" % example_file)

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

    for index,line in enumerate(str(lines).split("\n")):  
        if index == 0 and not append :
            buffer[0]=line
        else :
            buffer.append(line)  

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

def getLastBuffer():
    """get last edit vim buffer """
    listwinnr=str(vim.eval("winnr('#')"))
    vim.command("exec '%s wincmd w'" % listwinnr)
    buffer=vim.current.buffer
    listwinnr=str(vim.eval("winnr('#')"))
    vim.command("exec '%s wincmd w'" % listwinnr)
    return buffer

def getWorkBuffer(bufName):
    work_buffer=None

    for buffer in vim.buffers:
        if buffer.name == bufName :
            work_buffer=buffer
            break
    return work_buffer

def getOutputBuffer(name):
        shext_buffer=None
        for buffer in vim.buffers:
            if buffer.name and "SzToolView_%s" % name in buffer.name :
                shext_buffer=buffer
                break
        return shext_buffer

def pydoc(name):
    """ invoke help(name) to print help info, the text will be
    output to a splited buffer.
    """
    old_stdout=sys.stdout
    sys.stdout=StringIO()
    help(name)
    help_text=sys.stdout.getvalue().split("\n")
    sys.stdout=old_stdout
    createOutputBuffer("pydoc")
    outbuffer=getOutputBuffer("pydoc")
    output(help_text,outbuffer)


def startfile():
    """ invoke os.startfile to open the file under the cursor"""
    (row, col) = vim.current.window.cursor  
    line = vim.current.buffer[row-1]  
    os.startfile(line)

def openInFirefox():
    """ open current editing file in firefox browser """
    import subprocess
    cmd='firefox "%s"' %(vim.current.buffer.name)
    print cmd
    p=subprocess.Popen(cmd,shell=False)

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

def getVisualBlock():
    vb=vim.eval("GetVisualBlock()")
    lines=vb.split("\n")
    sql=""
    for line in lines:
      sql=sql+line+" "
    return sql.strip()

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

def startScriptEdit():

    vim.command("call SwitchToSzToolView('script')")    
    vim.command("map <buffer><silent>,, :python runScript()<cr>")
    vim.command("set filetype=python")
    vim.command("set bufhidden=delete")
    vim.command("autocmd BufLeave <buffer>  python saveScratchText()")

    buffer_name=vim.current.buffer.name
    if not scratch_buf :
        template=[]
        template.append("import vim")
        template.append("buffer=getLastBuffer()")
        output(template)
    else :
        output(scratch_buf)
    return

def saveScratchText():
    global scratch_buf
    scratch_buf = [] 
    for line in vim.current.buffer :
        scratch_buf.append(line.replace("\n",""))


def printScriptResult(result):
    """ output to result to a temp vim buffer named "scriptResult" """
    vim.command("call SwitchToSzToolView('scriptResult')")    
    output(result)

def runScript():
    script="\n".join([line for line in vim.current.buffer])
    exec script

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

def searchDict(word):
    global stardict
    createOutputBuffer("dict")
    outbuffer=getOutputBuffer("dict")
    dict_path = os.path.join(getDataHome(), "dict")
    if not stardict :
        stardict = Dictionary(os.path.join(dict_path, 'stardict-dreye', 'DrEye4in1'))
    result=getWordDef(stardict, word)
    if not result :
        if word.endswith("ed") :
            word=word[0:-2]
        elif word.endswith("s") :
            word=word[0:-1]
        result = getWordDef(stardict,word)
        if not result :
            result = "can't find the word definition"
    else :
        dict_search_log_path = os.path.join(getDataHome(), "dict/log.txt")
        log_file=open(dict_search_log_path,"aw")
        log_file.write(word+"\n")
        log_file.close()
    result=unicode(result,"utf-8")
    codepage=sys.getdefaultencoding()
    result=result.encode(codepage,"replace")
    output(result,outbuffer)


def getAppHome():
    sztool_home=vim.eval("g:sztool_home")
    sztool_app_home=os.path.join(sztool_home,"python")
    return sztool_app_home

def getDataHome():
    user_home = os.path.expanduser('~')
    sztool_data_home=os.path.join(user_home,".sztools")
    return sztool_data_home

def getShareHome():
    sztool_home=vim.eval("g:sztool_home")
    sztool_data_home=os.path.join(sztool_home,"share")
    return sztool_data_home

def getVisualArea():
    startCol=int(vim.eval("""col("'<")"""))-1
    endCol=int(vim.eval("""col("'>")"""))+1
    startLine=int(vim.eval("""line("'<")"""))
    endLine=int(vim.eval("""line("'>")"""))
    return [startCol,endCol,startLine,endLine]

def getVisualSynCmd(area=None):

    if area == None :
        startCol,endCol,startLine,endLine=getVisualArea()
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

def initHightLightScheme():
    vim.command("highlight def MarkWord1  ctermbg=Cyan     ctermfg=Black  guibg=#8CCBEA    guifg=Black")
    vim.command("highlight def MarkWord2  ctermbg=Green    ctermfg=Black  guibg=#A4E57E    guifg=Black")
    vim.command("highlight def MarkWord3  ctermbg=Yellow   ctermfg=Black  guibg=#FFDB72    guifg=Black")
    vim.command("highlight def MarkWord4  ctermbg=Red      ctermfg=Black  guibg=#FF7272    guifg=Black")
    vim.command("highlight def MarkWord5  ctermbg=Magenta  ctermfg=Black  guibg=#FFB3FF    guifg=Black")
    vim.command("highlight def MarkWord6  ctermbg=Blue     ctermfg=Black  guibg=#9999FF    guifg=Black")

def markVisual():
    if (not markSchemeInited ) :
        initHightLightScheme()
    for vimCmd in getVisualSynCmd():
        vim.command(vimCmd)

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

def initSztool():
    data_home = getDataHome()
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
    gscope["sztoolsCfg"]=SzToolsConfig(os.path.join(getShareHome(),"conf/sztools.cfg"))

    #append app path to sys.path
    import sys
    sys.path.append(getAppHome())

initSztool()
