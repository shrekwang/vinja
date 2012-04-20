import socket
import vim
import os.path
import time
import re
import traceback
import StringIO
import shutil
from subprocess import Popen
from string import Template
import difflib
from common import output,SzToolsConfig,MiscUtil,VimUtil,BasicTalker,ZipUtil,PathUtil

from pyparsing import *
from xml.etree.ElementTree import *

HOST = 'localhost'
PORT = 9527
END_TOKEN = "==end=="
MAX_CPT_COUNT = 200
bp_data = {}
lastProjectRoot = None

class ProjectManager(object):
    @staticmethod
    def getProjectRoot(filePath, useLastRoot = True):
        global lastProjectRoot
        projectRoot = None
        parent = filePath
        if not filePath :
            return None
        while True :
            tmpdir = os.path.dirname(parent)
            if tmpdir == "" or tmpdir == "/" or tmpdir == parent :
                break
            parent = tmpdir
            fullname = lambda name : os.path.join(parent,name)
            prj_names =[fullname(name) for name in [".project",".classpath"]]
            if os.path.exists(prj_names[0]) and os.path.exists(prj_names[1]):
                projectRoot = parent
                break
        if projectRoot != None :
            lastProjectRoot = projectRoot
        elif useLastRoot :
            projectRoot = lastProjectRoot
        return projectRoot

    @staticmethod
    def getSrcLocations(filePath):
        tree = ElementTree()
        classpathXml = ProjectManager.getClassPathXml(filePath)
        if os.path.isdir(classpathXml) :
            return [ classpathXml ]
        project_root = ProjectManager.getProjectRoot(filePath)
        tree.parse(classpathXml)
        entries = tree.findall("classpathentry")
        src_locs = []
        for entry in  entries :
            if entry.get("kind") == "src" :
                abpath = os.path.normpath(os.path.join(project_root,entry.get("path")))
                src_locs.append(abpath)
        return src_locs

    @staticmethod
    def getClassPathXml(filePath):
        projectRoot = ProjectManager.getProjectRoot(filePath)
        if not projectRoot :
            parent = os.path.dirname(filePath)
            return parent
        return os.path.join(projectRoot,".classpath")

    @staticmethod
    def getAntBuildXml(filePath):
        projectRoot = ProjectManager.getProjectRoot(filePath)
        if not projectRoot : return None
        antBuildXml = os.path.join(projectRoot,"build.xml")
        if os.path.exists(antBuildXml):
            return antBuildXml
        return None

    @staticmethod
    def projectInit():
        pwd = os.getcwd()
        projectName =  os.path.basename(os.getcwd())
        examples_dir = os.path.join(SzToolsConfig.getShareHome(),"examples")
        projectInitXml = os.path.join(examples_dir,"project.xml")
        classpathInitXml = os.path.join(examples_dir,"classpath.xml")
        jdeInitXml = os.path.join(examples_dir,"jde.xml")
        if not os.path.exists("src") :
            os.mkdir("src")
        if not os.path.exists("lib") :
            os.mkdir("lib")
        if not os.path.exists("dst") :
            os.mkdir("dst")
        lines = open(projectInitXml).readlines()
        f = open(".project","w")
        for line in lines :
            line=line.replace("test",projectName)
            f.write(line)
        f.close()
        shutil.copy2(classpathInitXml, "./.classpath")
        shutil.copy2(jdeInitXml, "./.jde")
        print "project initialized in current dir succeed."

    @staticmethod
    def projectClean():
        vim_buffer = vim.current.buffer
        current_file_name = vim_buffer.name
        classPathXml = ProjectManager.getClassPathXml(current_file_name)
        Talker.projectClean(classPathXml)

    @staticmethod
    def projectOpen():
        classPathXml = os.path.join(os.getcwd(),".classpath")
        if not os.path.exists(classPathXml) :
            vim_buffer = vim.current.buffer
            current_file_name = vim_buffer.name
            if current_file_name != None and os.path.exists(current_file_name):
                classPathXml = ProjectManager.getClassPathXml(current_file_name)
            else :
                classPathXml = ProjectManager.getClassPathXml(os.getcwd())
        #if we can't find .classpath, defer the project init process later .
        if classPathXml.endswith(".classpath") :
            Talker.projectOpen(classPathXml)

    @staticmethod
    def loadJarMeta():
        vim_buffer = vim.current.buffer
        current_file_name = vim_buffer.name
        classPathXml = ProjectManager.getClassPathXml(current_file_name)
        Talker.loadJarMeta(classPathXml)

class Talker(BasicTalker):
    
    @staticmethod
    def getPackageList(pkgname,xmlPath):
        params = dict()
        params["cmd"]="completion"
        params["completionType"] = "package"
        params["pkgname"] = pkgname
        params["classPathXml"] = xmlPath
        data = Talker.send(params)
        return data

    @staticmethod
    def getClassList(classNameStart,xmlPath,ignoreCase="false"):
        params = dict()
        params["cmd"]="completion"
        params["completionType"] = "class"
        params["className"] = classNameStart
        params["classPathXml"] = xmlPath
        params["ignoreCase"] = ignoreCase
        data = Talker.send(params)
        return data

    @staticmethod
    def locateSource(className, xmlPath, sourceType="declare"):
        "sourceType in { 'declare', 'impl] }"
        params = dict()
        params["cmd"]="locateSource"
        params["className"] = className
        params["classPathXml"] = xmlPath
        params["sourceType"] = sourceType
        data = Talker.send(params)
        return data

    @staticmethod
    def getMemberList(args):
        params = dict()
        sourceFile ,classnameList,xmlPath,memberType,expTokens = args
        params["cmd"]="completion"
        params["sourceFile"] = sourceFile
        params["completionType"] = memberType
        params["classnames"] = ",".join(classnameList)
        params["classPathXml"] = xmlPath
        params["expTokens"] = ",".join(expTokens)
        data = Talker.send(params)
        return data

    @staticmethod
    def getMethodDefs(args):
        "print all methods of a variable"
        params = dict()
        sourceFile ,classnameList,xmlPath,expTokens,memberName= args
        params["cmd"]="getMethodDefs"
        params["sourceFile"] = sourceFile
        params["classnames"] = ",".join(classnameList)
        params["classPathXml"] = xmlPath
        params["expTokens"] = ",".join(expTokens)
        params["memberName"] = memberName
        data = Talker.send(params)
        return data

    @staticmethod
    def getMethodDefClass(args):
        "get whic class defines a method"
        params = dict()
        sourceFile ,classnameList,xmlPath,expTokens,memberName,sourceType= args
        params["cmd"]="getMethodDefClass"
        params["sourceFile"] = sourceFile
        params["classnames"] = ",".join(classnameList)
        params["classPathXml"] = xmlPath
        params["expTokens"] = ",".join(expTokens)
        params["memberName"] = memberName
        params["sourceType"] = sourceType
        data = Talker.send(params)
        return data

    @staticmethod
    def searchRef(xmlPath,sourceFile,memberDesc):
        params = dict()
        params["cmd"]="searchRef"
        params["classPathXml"] = xmlPath
        params["sourceFile"] = sourceFile
        params["memberDesc"] = memberDesc
        data = Talker.send(params)
        return data

    @staticmethod
    def getConstructDefs(sourceFile,classnameList,xmlPath):
        params = dict()
        params["cmd"]="getConstructDefs"
        params["sourceFile"] = sourceFile
        params["classnames"] = ",".join(classnameList)
        params["classPathXml"] = xmlPath
        data = Talker.send(params)
        return data


    @staticmethod
    def compileFile(xmlPath,sourceFile):
        params = dict()
        params["cmd"]="compile"
        params["classPathXml"] = xmlPath
        params["sourceFile"] = sourceFile
        data = Talker.send(params)
        return data


    @staticmethod
    def typeHierarchy(xmlPath,sourceFile):
        params = dict()
        params["cmd"]="typeHierarchy"
        params["classPathXml"] = xmlPath
        params["sourceFile"] = sourceFile
        data = Talker.send(params)
        return data

    @staticmethod
    def copyResource(xmlPath,sourceFile):
        params = dict()
        params["cmd"]="copyResource"
        params["classPathXml"] = xmlPath
        params["sourceFile"] = sourceFile
        data = Talker.send(params)
        return data

    @staticmethod
    def runFile(xmlPath,sourceFile,vimServer,bufname,runCmd="run"):
        params = dict()
        params["cmd"]=runCmd
        params["classPathXml"] = xmlPath
        params["sourceFile"] = sourceFile
        params["vimServer"] = vimServer
        params["bufname"] = bufname
        data = Talker.send(params)
        return data

    @staticmethod
    def autoImport(xmlPath,varNames,pkgName):
        params = dict()
        params["cmd"]="autoimport"
        params["classPathXml"] = xmlPath
        params["varNames"] = varNames
        params["pkgName"] = pkgName
        data = Talker.send(params)
        return data

    @staticmethod
    def dumpClass(xmlPath,classnameList):
        params = dict()
        params["cmd"]="dumpClass"
        params["classPathXml"] = xmlPath
        params["dumpClassNames"] = ",".join(classnameList)
        data = Talker.send(params)
        return data

    @staticmethod
    def overideMethod(xmlPath,varNames):
        params = dict()
        params["cmd"]="overide"
        params["classPathXml"] = xmlPath
        params["varNames"] = ",".join(varNames)
        data = Talker.send(params)
        return data


    @staticmethod
    def runAntBuild(vimServer,cmdName,runInShell):
        params = dict()
        params["cmd"]="runSys"
        params["vimServer"] = vimServer
        params["cmdName"] = cmdName
        params["runInShell"] = runInShell
        data = Talker.send(params)
        return data


    @staticmethod
    def projectClean(xmlPath):
        params = dict()
        params["cmd"]="projectClean"
        params["classPathXml"] = xmlPath
        data = Talker.send(params)
        return data


    @staticmethod
    def projectOpen(xmlPath):
        params = dict()
        params["cmd"]="projectOpen"
        params["classPathXml"] = xmlPath
        data = Talker.send(params)
        return data

    @staticmethod
    def loadJarMeta(xmlPath):
        params = dict()
        params["cmd"]="loadJarMeta"
        params["classPathXml"] = xmlPath
        data = Talker.send(params)
        return data

class EditUtil(object):

    @staticmethod
    def createSkeleton():
        vim_buffer = vim.current.buffer
        if len(vim_buffer) > 10 :
            return 
        buf_content = "\n".join(vim_buffer)
        if not re.match("^\s*$",buf_content) :
            return 

        cur_file = vim_buffer.name
        if cur_file.startswith("jar:"):
            return
        if os.path.exists(cur_file) :
            file_content ="\n".join(open(cur_file,"r").readlines())
            if not re.match("^\s*$",file_content) :
                return

        cur_path = os.path.dirname(cur_file)
        prj_root = ProjectManager.getProjectRoot(cur_file)
        src_locs = ProjectManager.getSrcLocations(cur_file)
        pkg = ""
        for abs_src in src_locs :
            if cur_path.startswith(abs_src) :
                pkg = cur_path[ len(abs_src)+1 : ]
        if pkg != "" :
            vim_buffer.append("package %s;" % pkg.replace(os.path.sep,"."))
            vim_buffer.append("")

        class_name = os.path.splitext(os.path.basename(cur_file))[0]
        s = Template("public class $name {\n\n}")
        skeleton = s.substitute(name=class_name)
        for line in skeleton.split("\n"):
            vim_buffer.append(line)
        del vim_buffer[0]

    @staticmethod
    def generateGseter():
        jdef_parser = Parser.getJavaVarDefParser(None,False)
        statement = OneOrMore(Group(jdef_parser))
        statement.ignore( javaStyleComment )
        startCol,endCol,startLine,endLine=MiscUtil.getVisualArea()
        vim_buffer = vim.current.buffer
        selectedText = "\n".join(vim_buffer[startLine-1:endLine])
        results = statement.parseString(selectedText)

        template = """
          public void set$1($2 $3) {
            this.$3=$3;
          }
          public $2 get$1() {
            return this.$3;
          }
          """

        sb = []
        for item in  results :
            tmp = []
            for type_token in item[0]:
                if type_token not in "<>[]"  :
                    if len(tmp) > 0 and tmp[-1] not in "<>[]":
                        tmp.append(",")
                tmp.append(type_token)
            vartype = "".join(tmp)
            varname = item[1]
            result = template.replace("$1",varname[0].upper() + varname[1:])
            result = result.replace("$2",vartype)
            result = result.replace("$3",varname)
            sb.append(result)
        if sb == [] :
            return
        endline = Parser.parseClassEnd()
        del vim_buffer[endline]
        output(sb,append=True)
        output("}",append=True)
        #format the getter,setter code
        vim.command("normal %sGV200j=" % endline)

    @staticmethod
    def overideMethod():
        vim_buffer = vim.current.buffer
        current_file_name = vim_buffer.name
        classPathXml = ProjectManager.getClassPathXml(current_file_name)
        if not classPathXml : return
        allFullClassNames = Parser.getAllSuperClassFullNames()
        resultText = Talker.overideMethod(classPathXml,allFullClassNames)
        VimUtil.writeToSzToolBuffer("JdeConsole",resultText)

    @staticmethod
    def locateDefinition(sourceType):
        (row,col) = vim.current.window.cursor
        vim_buffer = vim.current.buffer
        line = vim_buffer[row-1]
        current_file_name = vim_buffer.name
        classPathXml = ProjectManager.getClassPathXml(current_file_name)

        dotExpParser = Parser.getJavaDotExpParser()
        tokenEndCol = col
        
        for char in line[col:] :
            if not re.match("\w",char) :
                break
            tokenEndCol += 1

        #if current line starts with "." or last line ends with "." . join the line
        if re.match(r"^\s*\..*", line) or re.match(r".*\.$",vim_buffer[row-2]) :
            line = vim_buffer[row-2].strip() + line[0:tokenEndCol+1]
            tokenEndCol = len(line) - 1 

        #if locate the class source
        classNamePat = r".*\b(?P<name>[A-Z]\w+)$"
        searchResult = re.search(classNamePat, line[0:tokenEndCol])
        if searchResult :
            className = searchResult.group("name")
            tmpLine = line[0:tokenEndCol]
            #check if the exp is some member name started with uppercase letter.
            if tmpLine[len(tmpLine) - len(className) - 1] != "." :
                classNameList = Parser.getFullClassNames(className)
                for className in classNameList :
                    sourcePath = Talker.locateSource(className, classPathXml,sourceType)
                    if sourcePath != "None" :
                        sourcePath, className = sourcePath.split("\n")
                        matchedLine = EditUtil.searchClassDefLineNum(className, sourcePath)
                        vim.command("edit +%s %s" % (matchedLine, sourcePath ))
                        break
                return 

        #locate the member of class
        expTokens = dotExpParser.searchString(line[0:tokenEndCol])[0]
        if not expTokens :
            return 
        if len(expTokens) == 1 :
            varName = "this"
        else :
            varName = expTokens[0]

        endTokenIndex = 0 if len(expTokens)==1 else -1
        #try find in local file first
        if len(expTokens) == 1 or (len(expTokens) == 3  and varName == "this"):
            if len(expTokens) ==1 :
                memberName = expTokens[0]
            else :
                memberName = expTokens[2]

            #search in visible scope(only upward)
            var_type, var_type_row = Parser.getVarTypeInfo(memberName,row-1)
            if var_type != None :
                vim.command("let @/='\<%s\>'" % memberName)
                vim.command("normal %sG" % str(var_type_row + 1))
                return

            #search in class member info
            members = Parser.parseAllMemberInfo(vim_buffer)
            for name,mtype,rtntype,param,lineNum in members :
                if name == memberName :
                    matched_row = lineNum
                    vim.command("let @/='\<%s\>'" % memberName)
                    vim.command("normal %sG" % str(matched_row))
                    return
        else :
            expTokens = expTokens[1:]
            memberName = expTokens[-1]
        if line[tokenEndCol] == "(":
            expTokens[endTokenIndex] = expTokens[endTokenIndex] + "()"

        superClass = Parser.getSuperClass()
        if varName[0].isupper():
            classname = varName
        elif varName == "this" :
            classname = "this"
        elif varName.endswith("()") : 
            classname = "this"
            expTokens.insert(0,varName)
        elif varName == "super" :
            classname = superClass
        else :
            classname = Parser.getVarType(varName,row-1)

        if not classname :
            if not superClass : return
            expTokens.insert(0,varName)
            classname = superClass

        classNameList = Parser.getFullClassNames(classname)
        expTokens = expTokens[:-1]
        tmpName = memberName + "()" if line[tokenEndCol] == "(" else memberName 

        #get the param count of method, match method by name and count of param first
        param_count = -1 
        if line[tokenEndCol] == "(":
            matched_count = -1
            param_names = None
            for index, ch in enumerate(line[tokenEndCol+1:]) :
                if "(" == ch :
                    matched_count = matched_count - 1
                if ")" == ch :
                    matched_count = matched_count + 1
                if matched_count == 0 :
                    param_names = line[tokenEndCol+1 : tokenEndCol+1+index]
                    break
            if param_names != None :
                param_count = len(param_names.split(",")) if param_names.strip() != "" else 0

        params =(current_file_name,classNameList,classPathXml,expTokens,tmpName,sourceType)
        sourcePath = Talker.getMethodDefClass(params)
        if sourcePath != "None" :
            matchedLine = EditUtil.searchMemeberLineNum(memberName, sourcePath,param_count)
            vim.command("let @/='\<%s\>'" % memberName)
            vim.command("edit +%s %s" % (matchedLine, sourcePath ))
        else :
            print "cant' locate the source code"
        return

    @staticmethod
    def reprDictInDoubleQuote(dic):
        pairs = []
        for item in dic.keys() :
            value = re.escape(dic[item])
            pair = '"'+str(item)+'"'+":"+'"'+value+'"'
            pairs.append(pair)
        return "{"+",".join(pairs)+"}"

    @staticmethod
    def searchRef():
        (row,col) = vim.current.window.cursor
        vim_buffer = vim.current.buffer
        line = vim_buffer[row-1]
        current_file_name = vim_buffer.name
        classPathXml = ProjectManager.getClassPathXml(current_file_name)

        result = Parser.parseCurrentMethod()
        if result == None :
            print "current line not contains a method declaration."
            return 
        rtntype,name,param = result
        memberDesc = rtntype + " " + name + "(" + param +")"
        resultText = Talker.searchRef(classPathXml,current_file_name,memberDesc)

        hltype = "R"
        HighlightManager.removeHighlightType(hltype)
        qflist = []
        for line in resultText.split("\n"):
            if line.strip() == "" : continue
            try :
                filename,lnum,text= line.split("::")
                bufnr=str(vim.eval("bufnr('%')"))
                absname = os.path.normpath(filename)
                filename = Compiler.relpath(absname)
                if filename != Compiler.relpath(current_file_name) :
                    qfitem = dict(filename=filename,lnum=lnum,text=text)
                else :
                    qfitem = dict(bufnr=bufnr,lnum=lnum,text=text)
                lstart = text.find(name) 
                lend = lstart + len(name) + 2 
                HighlightManager.addHighlightInfo(absname,lnum,hltype,text,lstart, lend)
                qflist.append(qfitem)
            except Exception , e:
                fp = StringIO.StringIO()
                traceback.print_exc(file=fp)
                message = fp.getvalue()
                logging.debug(message)

        HighlightManager.highlightCurrentBuf()
        if len(qflist) > 0 :
            #since vim use single quote string as literal string, the escape char will not
            #been handled, so repr the dict in a double quoted string
            qflist_str = "[" + ",".join([EditUtil.reprDictInDoubleQuote(item) for item in qflist])+"]" 
            vim.command("call setqflist(%s)" % qflist_str)
            vim.command("cwindow")
        else :
            print "can't find any reference location."

    @staticmethod
    def searchMemeberLineNum(memberName,sourcePath,paramCount = -1):
        if sourcePath.startswith("jar:") :
            lines = ZipUtil.read_zip_entry(sourcePath)
        else :
            lines = open(sourcePath).readlines()
        matched_row = 1
        members = Parser.parseAllMemberInfo(lines)
        nameMatches = [1]
        gotExactMatch = False
        for name,mtype,rtntype,param,lineNum in members :
            if name == memberName :
                tmp_count = len(param.split(",")) if  param.strip() != "" else 0
                if paramCount == -1 or paramCount == tmp_count :
                    matched_row = lineNum
                    gotExactMatch = True
                    break
                else :
                    nameMatches.append(lineNum)
        if gotExactMatch :
            return str(matched_row)
        else :
            return str(nameMatches[-1])

    @staticmethod
    def searchClassDefLineNum(className, sourcePath):
        if sourcePath.startswith("jar:") :
            lines = ZipUtil.read_zip_entry(sourcePath)
        else :
            lines = open(sourcePath).readlines()
        matched_row = 1

        clsPat = re.compile(r"\s*((public|private|protected)\s+)?"
                "((abstract|static|final|strictfp)\s+)?"
                "(class|interface)\s"+className+r"\b")
        for index,line in enumerate(lines):
            if clsPat.match(line):
                matched_row = index + 1
                break
        
        return str(matched_row)

    @staticmethod
    def searchAndEdit(current_file_name, className,memberName, mode="local",param_count=-1):
        classPathXml = ProjectManager.getClassPathXml(current_file_name)
        sourcePath = Talker.locateSource(className, classPathXml)

        if mode == "local" :
            editCmd="edit"
        elif mode == "buffer" :
            vim.command("split")
        elif mode == "tab" :
            vim.command("tabnew")

        if sourcePath != "None" :
            sourcePath, className = sourcePath.split("\n")
            if memberName.strip() != "" :
                matchedLine = EditUtil.searchMemeberLineNum(memberName, sourcePath,param_count)
            else :
                matchedLine = EditUtil.searchClassDefLineNum(className, sourcePath)
            vim.command("edit +%s %s" % (matchedLine, sourcePath ))
            
        return

    @staticmethod
    def tipMethodParameter():
        (row,col) = vim.current.window.cursor
        vim_buffer = vim.current.buffer
        line = vim_buffer[row-1]
        current_file_name = vim_buffer.name
        classPathXml = ProjectManager.getClassPathXml(current_file_name)

        tokenEndCol = line[0:col+1].rfind("(")
        if tokenEndCol < 0 : return

        newClassPat = r".*\bnew\s+(?P<name>[A-Z]\w+)$"
        searchResult = re.search(newClassPat, line[0:tokenEndCol])
        if searchResult :
            className = searchResult.group("name")
            classNameList = Parser.getFullClassNames(className)
            constructDefs = Talker.getConstructDefs(current_file_name,classNameList,classPathXml)
            if constructDefs == "" : return
            VimUtil.writeToSzToolBuffer("JdeConsole",constructDefs)
            return 


        dotExpParser = Parser.getJavaDotExpParser()
        expTokens = dotExpParser.searchString(line[0:tokenEndCol])[0]
        if not expTokens : return 
        if len(expTokens) == 1 :
            varName = "this"
        else :
            varName = expTokens[0]
        endTokenIndex = 0 if len(expTokens)==1 else -1

        if len(expTokens) == 1 or (len(expTokens) == 3  and varName == "this"):
            if len(expTokens) ==1 :
                memberName = expTokens[0]
            else :
                memberName = expTokens[2]
            members = Parser.parseAllMemberInfo(vim_buffer)
            for name,mtype,rtntype,param,lineNum in members :
                if name == memberName :
                    matched_row = lineNum
                    vim.command("normal %sG" % str(matched_row))
                    return
        else :
            expTokens = expTokens[1:]
            memberName = expTokens[-1]

        if varName[0].isupper():
            classname = varName
        elif varName == "this" :
            classname = "this"
        else :
            classname = Parser.getVarType(varName,row-1)

        superClass = Parser.getSuperClass()
        if not classname :
            if not superClass : return
            expTokens.insert(0,varName)
            classname = superClass

        classNameList = Parser.getFullClassNames(classname)
        expTokens = expTokens[:-1]
        params =(current_file_name,classNameList,classPathXml,expTokens,memberName)
        methodDefs = Talker.getMethodDefs(params)
        if methodDefs == "" :
            return
        VimUtil.writeToSzToolBuffer("JdeConsole",methodDefs)
        
        return

    @staticmethod
    def dumpClassInfo():
        classname = vim.eval("expand('<cword>')")
        if classname == "" : return
        vim_buffer = vim.current.buffer
        current_file_name = vim_buffer.name
        classPathXml = ProjectManager.getClassPathXml(current_file_name)
        if not classPathXml : return
        classNameList = Parser.getFullClassNames(classname)
        result = Talker.dumpClass(classPathXml,classNameList)
        VimUtil.writeToSzToolBuffer("JdeConsole",result)

    @staticmethod
    def toggleBreakpoint():
        global bp_data
        file_name = vim.current.buffer.name
        (row,col) = vim.current.window.cursor
        bp_set = bp_data.get(file_name)
        if bp_set == None :
            bp_set = set()

        mainClassName = Parser.getMainClass()
        class_path_xml = ProjectManager.getClassPathXml(file_name)
        serverName = vim.eval("v:servername")
        bufnr=str(vim.eval("bufnr('%')"))

        if row in bp_set :
            cmdline = "breakpoint_remove %s %s" % (mainClassName,row)
            data = JdbTalker.submit(cmdline,class_path_xml,serverName)
            if data == "success" :
                bp_set.remove(row)
                HighlightManager.removeSign(file_name,row,"B")
            else :
                print "remove breakpoint error : msgs "+data
        else :
            cmdline = "breakpoint_add %s %s" % (mainClassName,row)
            data = JdbTalker.submit(cmdline,class_path_xml,serverName)
            if data == "success" :
                HighlightManager.addSign(file_name,row, "B")
                bp_set.add(row)
                bp_data[file_name] = bp_set
            else :
                print "can't create breakpoint here"

    @staticmethod
    def addConditionalBreakpoint(lineNum):
        global bp_data
        file_name = vim.current.buffer.name
        (row,col) = vim.current.window.cursor
        bp_set = bp_data.get(file_name)
        if bp_set == None :
            bp_set = set()

        HighlightManager.addSign(file_name,lineNum, "B")
        bp_set.add(row)
        bp_data[file_name] = bp_set

    @staticmethod
    def syncBreakpointInfo():

        global bp_data
        current_file = vim.current.buffer.name
        bp_set = bp_data.get(current_file)
        if bp_set :
            for row_num in bp_set :
                HighlightManager.removeSign(current_file,row_num,"B")

        source_file_path = vim.current.buffer.name
        serverName = vim.eval("v:servername")
        class_path_xml = ProjectManager.getClassPathXml(source_file_path)
        data = JdbTalker.submit("syncbps",class_path_xml,serverName)
        bp_data = {}
        if data : 
            for line in data.split("\n"):
                if line.strip() == "" or line.find(" ") < 0 :
                    continue
                class_name, line_num = line.split(" ")
                rlt_path = class_name.replace(".", os.path.sep)+".java"
                src_locs = ProjectManager.getSrcLocations(source_file_path)
                matched_file = None
                for src_loc in src_locs :
                    abs_path = os.path.normpath(os.path.join(src_loc, rlt_path))
                    if os.path.exists(abs_path) :
                        matched_file = abs_path
                        break
                if matched_file != None :
                    bp_set = bp_data.get(matched_file)
                    if bp_set == None :
                        bp_set = set()
                    bp_set.add(int(line_num))
                    bp_data[matched_file] = bp_set
                    if matched_file == vim.current.buffer.name :
                        HighlightManager.addSign(matched_file,line_num,"B")

    @staticmethod
    def getTypeHierarchy():
        (row,col) = vim.current.window.cursor
        vim_buffer = vim.current.buffer
        line = vim_buffer[row-1]
        current_file_name = vim_buffer.name
        classPathXml = ProjectManager.getClassPathXml(current_file_name)
        if not classPathXml :
            return
        resultText = Talker.typeHierarchy(classPathXml,current_file_name)
        return resultText

class HighlightManager(object):

    class Info(object):
        def __init__(self,msg,lstart,lend,hltype):
            self.msg = msg
            self.lstart = lstart
            self.lend = lend
            self.hltype = hltype
        def __str__(self):
            return "(%s,%s)" % (self.hltype,self.msg)
        def __repr__(self):
            return "(%s,%s)" % (self.hltype,self.msg)

    @staticmethod
    def removeHighlightType(removed_hltype):
        global all_hl_info
        all_hl_info = globals().get("all_hl_info")
        if all_hl_info == None :
            return 
        empty_buf = []
        for file_path in all_hl_info :
            buf_hl_info = all_hl_info.get(file_path)
            empty_line =[]
            for line_num in buf_hl_info :
                infos = [info for info in buf_hl_info.get(line_num) if info.hltype != removed_hltype]
                if len(infos) > 0 :
                    buf_hl_info[line_num] = infos
                else :
                    empty_line.append(line_num)
            for line_num in empty_line:
                del buf_hl_info[line_num]

            if len(buf_hl_info) == 0 :
                empty_buf.append(file_path)
        for file_path in empty_buf :
            del all_hl_info[file_path]


    @staticmethod
    def addHighlightInfo(abs_path,lnum,hltype,msg="",lstart=-1,lend=-1): 
        global all_hl_info
        lnum=int(lnum)
        all_hl_info = globals().get("all_hl_info")
        if all_hl_info == None :
            all_hl_info = {} 

        buf_hl_info = all_hl_info.get(abs_path)
        if buf_hl_info == None :
            buf_hl_info = {}

        cur_hl_info = HighlightManager.Info(msg,lstart,lend,hltype)
        line_hl_info = buf_hl_info.get(lnum)
        if line_hl_info == None :
            line_hl_info = []
        line_hl_info.append(cur_hl_info)
        buf_hl_info[lnum] = line_hl_info
        all_hl_info[abs_path] = buf_hl_info

    @staticmethod
    def addSign(abs_path, lnum,hltype,bufnr=None):
        lnum=int(lnum)
        HighlightManager.addHighlightInfo(abs_path,lnum,hltype)
        if bufnr == None :
            bufnr=str(vim.eval("bufnr('%')"))

        global all_hl_info
        all_hl_info = globals().get("all_hl_info")
        buf_hl_info = all_hl_info.get(abs_path)
        line_hl_info = buf_hl_info.get(lnum)

        hltypes = "".join([info.hltype for info in line_hl_info])
        group = HighlightManager._getGroupName(hltypes)
        HighlightManager._signErrorGroup(lnum,group,bufnr)

    @staticmethod
    def removeSign(abs_path, lnum, hltype,bufnr = None):
        global all_hl_info
        lnum = int(lnum)
        all_hl_info = globals().get("all_hl_info")
        if all_hl_info == None :
            return
        buf_hl_info = all_hl_info.get(abs_path)
        if buf_hl_info == None :
            return
        line_hl_info = buf_hl_info.get(lnum)
        if line_hl_info == None :
            return
        line_hl_info = [info for info in line_hl_info if info.hltype != hltype]

        if bufnr == None :
            bufnr=str(vim.eval("bufnr('%')"))
        unsigncmd = "sign unplace %s buffer=%s" % (str(lnum),bufnr)
        unsigncmd = "sign unplace %s buffer=%s" % (str(lnum),bufnr)
        vim.command(unsigncmd)
        if len(line_hl_info) == 0 :
            del buf_hl_info[lnum]
        else :
            buf_hl_info[lnum] = line_hl_info
            hltypes = "".join([info.hltype for info in line_hl_info])
            group = HighlightManager._getGroupName(hltypes)
            signcmd=Template("sign place ${id} line=${lnum} name=${name} buffer=${nr}")
            signcmd =signcmd.substitute(id=lnum,lnum=lnum,name=group, nr=bufnr)
            vim.command(signcmd)

    @staticmethod
    def displayMsg():
        global all_hl_info
        all_hl_info = globals().get("all_hl_info")
        if all_hl_info == None :
            return 
        vim_buffer = vim.current.buffer
        buf_hl_info = all_hl_info.get(vim_buffer.name)
        if buf_hl_info == None : 
            return
        (row,col) = vim.current.window.cursor
        if buf_hl_info.get(row) != None :
            fist_hl_info = buf_hl_info.get(row)[0]
            msg = fist_hl_info.msg
            vim.command("call DisplayMsg('%s')" % msg)
        else :
            vim.command("call DisplayMsg('%s')" % "")

    @staticmethod
    def highlightCurrentBuf():
        global all_hl_info
        HighlightManager._clearHighlightInVim()
        all_hl_info = globals().get("all_hl_info")
        if all_hl_info == None :
            return 
        file_name = vim.current.buffer.name
        buf_hl_info = all_hl_info.get(file_name)
        if buf_hl_info == None : 
            return
        bufnr=str(vim.eval("bufnr('%')"))
        for lnum in buf_hl_info :
            hltypes =[]
            for info in buf_hl_info.get(lnum) :
                group = HighlightManager._getGroupName(info.hltype)
                HighlightManager._highlightErrorGroup(lnum,info.lstart,info.lend,group)
                hltypes.append(info.hltype)
            group = HighlightManager._getGroupName("".join(hltypes))
            HighlightManager._signErrorGroup(lnum,group,bufnr)

    @staticmethod
    def _getGroupName(highlightType):
        infos = {"W":"SzjdeWarning","E":"SzjdeError","R":"SzjdeReference",\
                "B":"SzjdeBreakPoint","S":"SuspendLine", "SB":"SuspendLineBP"}
        if "S" in highlightType and "B" in highlightType :
            group = "SuspendLineBP"
        elif "S" in highlightType:
            group = "SuspendLine"
        elif "B" in highlightType: 
            group = "SzjdeBreakPoint"
        elif "E" in highlightType:
            group = "SzjdeError"
        elif "W" in highlightType :
            group = "SzjdeWarning"
        elif "R" in highlightType:
            group = "SzjdeReference"
        return group

    @staticmethod
    def _highlightErrorGroup(errorRow,start,end,group):
        errorRow,start,end = int(errorRow), int(start), int(end)
        if start < 0 or end < 0 or errorRow < 0 :
            return
        vim_buffer = vim.current.buffer
        charCount = 0
        fileformat = vim.eval("&fileformat")
        newLineCount =1 
        if fileformat == "dos" :
            newLineCount = 2

        if group=="SzjdeError" or group == "SzjdeWarning" :
            for row in vim_buffer[0:errorRow-1] :
                charCount += len(unicode(row)) +  newLineCount
            rowStart = 0 if start - charCount < 0 else start - charCount
            #TODO : shit! where does that magic number come from ? don't fucing rember.
            rowEnd = end - charCount + 3
            if rowEnd < 0 :
                rowEnd = rowStart + len(unicode(vim_buffer[errorRow]))  
        else :
            rowStart = start
            rowEnd = end

        syncmd = """syn match %s "\%%%sl\%%>%sc.\%%<%sc" """ %(group, errorRow, rowStart, rowEnd)
        vim.command(syncmd)

    @staticmethod
    def _signErrorGroup(errorRow,group,bufnr):
        signcmd=Template("sign place ${id} line=${lnum} name=${name} buffer=${nr}")
        signcmd =signcmd.substitute(id=errorRow,lnum=errorRow,name=group, nr=bufnr)
        vim.command(signcmd)

    @staticmethod
    def initBreakpointSign():
        HighlightManager.highlightCurrentBuf()
        return
        
    @staticmethod
    def _clearHighlightInVim():

        vim.command("syntax clear SzjdeError")
        vim.command("syntax clear SzjdeWarning")
        vim.command("syntax clear SzjdeReference")

        pat = re.compile(r".*id=(?P<name>\d+)\b.*$")
        bufnr=str(vim.eval("bufnr('%')"))
        vim.command("redir => g:current_signplace")
        vim.command("silent sign place buffer=%s" % bufnr )
        vim.command("redir END")
        output = vim.eval("g:current_signplace")
        lines = output.split("\n")
        for line in lines :
            sign_ids = pat.findall(line)
            if (len(sign_ids) == 1 ) :
                vim.command("silent sign unplace %s buffer=%s" % (sign_ids[0], bufnr ))
    
class Compiler(object):

    @staticmethod
    def relpath(path):
        if path.startswith(os.getcwd()) :
            return os.path.relpath(path)
        else :
            return path

    @staticmethod
    def compileCurrentFile(buildProject = False):
        (row,col) = vim.current.window.cursor
        vim_buffer = vim.current.buffer

        line = vim_buffer[row-1]
        current_file_name = vim_buffer.name
        classPathXml = ProjectManager.getClassPathXml(current_file_name)
        if not classPathXml :
            return

        need_compiled = False
        src_locs = ProjectManager.getSrcLocations(current_file_name)
        for abs_src in src_locs :
            if current_file_name.startswith(abs_src) :
                need_compiled = True
                break
        if not need_compiled :
            return 

        if buildProject :
            current_file_name = "All"
            print "build project can take a while, please wait....."
        resultText = Talker.compileFile(classPathXml,current_file_name)
        allsrcFiles, errorMsgList = resultText.split("$$$$$")
        allsrcFiles = allsrcFiles.split("\n")
        errorMsgList = errorMsgList.split("\n")
        qflist = []
        #clear highlight type E and W
        HighlightManager.removeHighlightType("E")
        HighlightManager.removeHighlightType("W")
        error_files = []
        for line in errorMsgList:
            if line.strip() == "" : continue
            try :
                errorType,filename,lnum,text,lstart,lend = line.split("::")
                if errorType == "E" :
                    error_files.append(filename)
                bufnr=str(vim.eval("bufnr('%')"))
                absname = os.path.normpath(filename)
                filename = Compiler.relpath(absname)
                if filename != Compiler.relpath(current_file_name) :
                    qfitem = dict(filename=filename,lnum=lnum,text=text,type=errorType)
                else :
                    qfitem = dict(bufnr=bufnr,lnum=lnum,text=text,type=errorType)
                HighlightManager.addHighlightInfo(absname,lnum,errorType,text,lstart,lend)
                qflist.append(qfitem)
            except Exception , e:
                fp = StringIO.StringIO()
                traceback.print_exc(file=fp)
                message = fp.getvalue()
                logging.debug(message)
        EditUtil.syncBreakpointInfo()
        HighlightManager.highlightCurrentBuf()
        pathflags =[(filename.replace("\n",""),False) for filename in allsrcFiles]
        pathflags.extend([(filename,True) for filename in error_files])
        Compiler.set_error_flags(pathflags)

        vim.command("call setqflist(%s)" % qflist)
        if len(error_files) > 0 :
            vim.command("cwindow")
        else :
            vim.command("cclose")

    @staticmethod
    def set_error_flags(pathflags):
        if "projectTree" not in globals() :
            return 
        
        for path,flag in pathflags :
            node = projectTree.find_node(path)
            if node != None :
                node.set_error_flag(flag)

        if not VimUtil.isSzToolBufferVisible('ProjectTree'):
            return 
        vim.command("call SwitchToSzToolView('ProjectTree')" )
        (row,col) = vim.current.window.cursor
        projectTree.render_tree()
        vim.current.window.cursor = (row,col)
        vim.command("exec 'wincmd w'")

    @staticmethod
    def copyResource():
        vim_buffer = vim.current.buffer
        current_file_name = vim_buffer.name
        if not current_file_name : return
        if current_file_name.endswith(".java") : return
        classPathXml = ProjectManager.getClassPathXml(current_file_name)
        if not classPathXml : return
        resultText = Talker.copyResource(classPathXml,current_file_name)
        VimUtil.writeToSzToolBuffer("JdeConsole",resultText)

class Runner(object):

    @staticmethod
    def runCurrentFile(runCmd="run"):
        (row,col) = vim.current.window.cursor
        vim_buffer = vim.current.buffer
        line = vim_buffer[row-1]
        current_file_name = vim_buffer.name
        classPathXml = ProjectManager.getClassPathXml(current_file_name)
        if not classPathXml : return
        serverName = vim.eval("v:servername")
        resultText = Talker.runFile(classPathXml,current_file_name,serverName,"JdeConsole",runCmd)
        VimUtil.writeToSzToolBuffer("JdeConsole",resultText)

    @staticmethod
    def runAntBuild(target=None):
        serverName = vim.eval("v:servername")
        vim_buffer = vim.current.buffer
        current_file_name = vim_buffer.name
        antBuildXml = ProjectManager.getAntBuildXml(current_file_name)
        if antBuildXml :
            cmdName = "ant -file "+antBuildXml
            runInShell = "false"
            if os.name == "nt" :
                runInShell = "true"
            resultText = Talker.runAntBuild(serverName,cmdName,runInShell)
        else :
            print "can't find the build.xml."

class AutoImport(object):

    @staticmethod
    def getImportInsertLocation():
        vim_buffer = vim.current.buffer
        pkgLocation = 0
        impLocation = -1

        for index,line in enumerate(vim_buffer):
            if line.strip().startswith("import "):
                impLocation = index
                break
        if impLocation > -1 :
            return impLocation

        for index,line in enumerate(vim_buffer):
            if line.strip().startswith("package") :
                pkgLocation = index + 1
                break
        return pkgLocation

    @staticmethod
    def addImportDef(line):
        if line.strip() == "" : return
        vim_buffer_text ="\n".join(vim.current.buffer)
        tmpDefs = line[:-1].split(";")
        hadImported = False

        for tmpDef in tmpDefs :
            pat = r"import\s+%s\b|import\s+%s" % (tmpDef, tmpDef[0:tmpDef.rfind(".")]+"\.\*")
            if re.search(pat,vim_buffer_text) :
                hadImported = True
                break
        if not hadImported :
            location = AutoImport.getImportInsertLocation()
            if ( len(tmpDefs) == 1 ) :
                insertText = "import %s;" % tmpDefs[0]
                vim.command("call append(%s,'%s')" %(str(location),insertText))
            else :
                selectedIndex = VimUtil.inputOption(tmpDefs)
                if (selectedIndex) :
                    insertText = "import %s;" % tmpDefs[int(selectedIndex)]
                    vim.command("call append(%s,'%s')" %(str(location),insertText))

    @staticmethod
    def autoImportVar():
        AutoImport.removeUnusedImport()
        vim_buffer = vim.current.buffer
        current_file_name = vim_buffer.name
        classPathXml = ProjectManager.getClassPathXml(current_file_name)
        if not classPathXml : return

        currentPackage = Parser.getPackage()
        if not currentPackage :
            currentPackage = ""

        searchText = "\n".join(vim_buffer)
        #remove comments
        commentPat = re.compile(r"(\/\*.*?\*\/)|((\/\/.*?)(?=\n))", re.DOTALL)
        searchText = commentPat.sub("",searchText)

        strliteralPat = re.compile(r'"(\\"|[^"])*"')
        searchText = strliteralPat.sub("",searchText)

        dclClassNamePat = re.compile(r"(?<=class)\b\W\w+\b")
        dclClassNames =[item.strip() for item in  dclClassNamePat.findall(searchText)]

        # upercase words except preceded by "."
        classNamePat = re.compile(r"\b(?<!\.)[A-Z]\w+\b")
        var_type_set=set(classNamePat.findall(searchText))

        for clsName in dclClassNames :
            var_type_set.discard(clsName)

        varNames=",".join(var_type_set)
        resultText = Talker.autoImport(classPathXml,varNames,currentPackage)
        lines = resultText.split("\n")
        for line in lines :
            AutoImport.addImportDef(line)
        location = AutoImport.getImportInsertLocation()
        if location > 0 and vim_buffer[location-1].strip() != "" :
            vim.command("call append(%s,'')" %(str(location)))

    @staticmethod
    def removeUnusedImport():
        vim_buffer = vim.current.buffer
        rowIndex = 0
        while True :
            line = vim_buffer[rowIndex].strip()
            if line.startswith("import") :
                lastName = line[7:-1].split(".")[-1]
                if lastName == "*" : 
                    del vim_buffer[rowIndex]
                    continue
                groups = re.findall(r"\b%s\b" % lastName, "\n".join(vim_buffer))
                if len(groups) <= 1 :
                    del vim_buffer[rowIndex]
                    continue
            rowIndex += 1
            if rowIndex > len(vim_buffer) -1 :
                break

class Parser(object):

    @staticmethod
    def parseClassEnd():
        vim_buffer = vim.current.buffer
        row_count = len(vim_buffer)
        end_line = 0
        for lineNum in range(row_count-1,-1,-1):
            line = vim_buffer[lineNum]
            if line.endswith("}") :
                end_line = lineNum
                break
        return end_line

    @staticmethod
    def parseCurrentMethod():
        (row,col) = vim.current.window.cursor
        vim_buffer = vim.current.buffer
        fullDeclLine = vim_buffer[row-1]
        methodPat = re.compile(r"(?P<rtntype>[\w<>,]+)\s+(?P<name>\w+)\s*\((?P<param>.*)\)")
        if "(" in fullDeclLine :
            startLine = row
            while True :
                if ")" in fullDeclLine :
                    break
                fullDeclLine = fullDeclLine +vim_buffer[startLine].replace("\t","  ")
                startLine = startLine + 1
        result =  methodPat.search(fullDeclLine)
        if result == None : return None

        name = result.group("name")
        rtntype = result.group("rtntype")
        param = result.group("param")
        return rtntype,name,param

    @staticmethod
    def parseCurrentMethodName():
        result = Parser.parseCurrentMethod()
        if result == None :
            return "", None
        rtntype,name,param = result
        return name,param

    @staticmethod
    def parseAllMemberInfo(lines):
        memberInfo = []
        scopeCount = 0
        methodPat = re.compile(r"(?P<rtntype>[\w<>\[\],]+)\s+(?P<name>\w+)\s*\((?P<param>.*)\)")
        assignPat = re.compile("(?P<rtntype>[\w<>\[\],]+)\s+(?P<name>\w+)\s*=")
        defPat = re.compile("(?P<rtntype>[\w<>\[\],]+)\s+(?P<name>\w+)\s*;")
        commentLine = False
        for lineNum,line in enumerate(lines) :
            line = line.strip()
            if (line.startswith("/*") and not line.endswith("*/") ) :
                commentLine = True
                continue
            if line.endswith("*/"):
                commentLine = False
                continue
            if commentLine == True or line.startswith("//"):
                continue
            if scopeCount == 1 :
                fullDeclLine = line
                if "=" in line :
                    pat = assignPat
                    mtype = "field"
                elif "(" in line :
                    startLine = lineNum + 1
                    while True :
                        if ")" in fullDeclLine or startLine >= len(lines) :
                            break
                        fullDeclLine = fullDeclLine +lines[startLine].replace("\t","  ")
                        startLine = startLine + 1
                    pat = methodPat
                    mtype = "method"
                else :
                    pat = defPat
                    mtype = "field"
                search_res=pat.search(fullDeclLine)
                if search_res :
                    name = search_res.group("name")
                    rtntype = search_res.group("rtntype")
                    if mtype == "method" :
                        param = search_res.group("param")
                    else :
                        param = ""
                    memberInfo.append((name,mtype,rtntype,param,lineNum+1))
            if "{" in line :
                scopeCount = scopeCount + line.count("{")
            if "}" in lines[lineNum] :
                scopeCount = scopeCount - line.count("}")
        return  memberInfo

    @staticmethod
    def isJavaKeyword(word):
        keyword_str = """
            abstract    default    if            private      this
            do         implements    protected    throw
            break       import        public       throws
            else       instanceof    return       transient
            case        extends    try
            catch       final      interface     static       void
            finally    strictfp     volatile
            class       native        super        while
            const       for        new           switch
            continue    goto       package       synchronized """
        match=re.search(r"\b%s\b" % word ,keyword_str)
        if match :
            return True
        return False

    @staticmethod
    def getVisibleRowNum(cursorRow):
        vim_buffer = vim.current.buffer
        scopeCount = 0
        scopeUnvisible = False
        visibleRowNum = []
        commentLine = False
        methodPat = re.compile(r"(?P<modifier>\w+\s+)?(?P<rtntype>[\w<>\[\],]+)\s+(?P<name>\w+)\s*\((?P<param>.*\)).*$")
        for lineNum in range(cursorRow,-1,-1):
            lineText = vim_buffer[lineNum].strip()
            if lineText.startswith("//"):
                continue
            if lineText.endswith("*/") :
                commentLine = True
            if lineText.startswith("/*"):
                commentLine = False
                continue
            if commentLine :
                continue

            if "}" in lineText :
                scopeCount = scopeCount - 1
                scopeUnvisible = True
            if "{" in lineText and scopeUnvisible :
                scopeCount = scopeCount + 1
                if (scopeCount > -1 ) :
                    scopeUnvisible = False
                if methodPat.match(lineText):
                    continue
            if not scopeUnvisible :
                visibleRowNum.append(lineNum)

        return visibleRowNum

    @staticmethod
    def getJavaVarDefParser(var_name=None,suppress=True):
        modifier_token = Suppress(Optional(oneOf("public private protected" )))
        storage_token = Suppress(Optional(OneOrMore(oneOf("static transient volatile final"))))
        var_type = Forward()
        if suppress :
            generic_def = Suppress("<")+delimitedList(var_type) + Suppress(">")
            array_def = Suppress("[") + Suppress("]")
        else :
            generic_def = ("<")+delimitedList(var_type) + (">")
            array_def = ("[") + ("]")
        var_type << Word(alphas,alphanums) + Optional(generic_def | array_def )
        if not var_name :
            var_name = Word( alphas, alphanums ).setResultsName("var_name")
        else :
            var_name = Keyword(var_name).setResultsName("var_name")
        statement = modifier_token + storage_token + Group(var_type) + var_name + restOfLine
        return statement

    @staticmethod
    def getJavaDotExpParser():
        java_exp = Forward()
        param_atom = java_exp | Optional('"')+Word(alphanums)+Optional('"')  
        func_param = "("+Suppress(Optional(delimitedList(param_atom))) + ")"
        atom_exp =  Combine(Word(alphas,alphanums+"_") + Optional(func_param))
        java_exp << atom_exp + Optional(OneOrMore("." + atom_exp))
        comp_exp = java_exp + Optional(".") +  lineEnd
        return comp_exp

    @staticmethod
    def getVarTypeInfo(varName,cursorRow):
        vim_buffer = vim.current.buffer
        jdef_parser = Parser.getJavaVarDefParser(varName)
        visibleRowNum = Parser.getVisibleRowNum(cursorRow)
        var_type = None
        var_type_row = -1
        searchLines = []
        found = False
        for row in visibleRowNum:
            lineText = vim_buffer[row].strip()
            if lineText.startswith("*") or lineText.startswith("//") :
                continue
            if varName not in lineText :
                continue
            for result in jdef_parser.searchString(vim_buffer[row]):
                if Parser.isJavaKeyword(result[0][0]) : continue
                var_type = result[0][0]
                var_type_row = row
                found = True
                break
            if found : break

        return var_type, var_type_row

    @staticmethod
    def getVarType(varName,cursorRow):
        var_type, var_type_row = Parser.getVarTypeInfo(varName,cursorRow)
        return var_type
        
    @staticmethod
    def getFullClassNames(classname):
        vim_buffer = vim.current.buffer
        binNames = []
        currentPackage = Parser.getPackage()
        if not classname :
            return []
        if not currentPackage :
            currentPackage = ""
        hasExactMatch = False
        for line in vim_buffer:
            line = line.strip()
            if line.startswith("import "):
                lastName=line[7:-1].split(".")[-1]
                if lastName == classname :
                    binNames = [ line[7:-1] ]
                    hasExactMatch = True
                    break
                elif lastName == "*" :
                    binNames.append(line[7:-1])
        if hasExactMatch :
            return binNames
        samePkgClass=currentPackage+"."+classname
        if len(binNames) > 0 :
            binNames= [name.replace("*",classname) for name in binNames]
            binNames.append(samePkgClass)
            binNames.append(classname)
            return binNames
        else :
            return [classname,samePkgClass]


    @staticmethod
    def searchPattern(pat,groupName):
        vim_buffer = vim.current.buffer
        search_text = "\n".join(vim_buffer)
        result  = pat.search(search_text)
        if result :
            return result.group(groupName)
        return None

    @staticmethod
    def getSuperClass():
        extPat = re.compile("\s+extends\s+(?P<superclass>\w+)")
        return  Parser.searchPattern(extPat,"superclass")

    @staticmethod
    def getAllNewedClasses():
        pat = re.compile("\W+new\s+(?P<newclass>\w+)")
        vim_buffer = vim.current.buffer
        search_text = "\n".join(vim_buffer)
        result  = pat.findall(search_text)
        return result

    @staticmethod
    def getPackage():
        pkgPat = re.compile("\s*package\s+(?P<package>[\w.]+)\s*;")
        return  Parser.searchPattern(pkgPat,"package")

    @staticmethod
    def getMainClass():
        pkgName = Parser.getPackage()
        clsPat = re.compile(r"\s*public\s+(\w+\s+)?class\s(?P<className>\w+)\b")
        className = Parser.searchPattern(clsPat,"className")
        if pkgName :
            className = pkgName + "." + className
        return className

    @staticmethod
    def getInterfaces():
        impPat = re.compile("\s+implements\s+(?P<interface>[\w, ]+)")
        result = Parser.searchPattern(impPat , "interface")
        if result :
             return [item.strip() for item in result.split(",")]
        return None

    @staticmethod
    def getAllSuperClassFullNames():
        allSuper = Parser.getInterfaces()
        if not allSuper :
            allSuper = []
        superClass = Parser.getSuperClass()
        if superClass :
            allSuper.append(superClass)
        allFullClassNames = []
        for name in allSuper :
            classNameList = Parser.getFullClassNames(name)
            for fullName in classNameList :
                allFullClassNames.append(fullName)
        return allFullClassNames

class SzJdeCompletion(object):
    @staticmethod
    def completion(findstart,base):
        if str(findstart) == "1":
            (row,col) = vim.current.window.cursor
            line = vim.current.buffer[row-1]
            index = 0
            for i in range(col-1,-1, -1):
                char = line[i]
                if char in " =;,.'()!^+-/<>[]@\"\t" :
                    index = i + 1
                    break
            cmd = "let g:SzJdeCompletionIndex = %s" %str(index)
        else:
            try :
                result = SzJdeCompletion.getCompleteResult(base)
            except Exception , e :
                fp = StringIO.StringIO()
                traceback.print_exc(file=fp)
                message = fp.getvalue()
                logging.debug(message)
                result = []
            cmd = "let g:SzJdeCompletionResult = %s" % result
        vim.command(cmd)

    @staticmethod
    def getCompleteType(line):
        importExp = re.compile(r'\s*import\s+.*')
        #memberExp = re.compile(r'.*\s+\w+\.\w*$')
        omniType = None
        if importExp.match(line):
            omniType = "package"
        else :
            dotExpParser = Parser.getJavaDotExpParser()
            result = dotExpParser.searchString(line)
            omniType = "word"
            if len(result) > 0 and len(result[0]) > 1 :
                if result[0][-1] == "." or result[0][-2] == "." :
                    omniType = "member"
        return omniType

    @staticmethod
    def filterList(srcList,exp):
        exp = exp.upper()
        if exp.find("*") > -1 :
            pat = re.compile("^%s$" % exp.replace("*",".*"))
        else:
            pat = re.compile("^%s.*" %exp)

        result = [str(item) for item in srcList if pat.match(item.upper())]
        return result

    @staticmethod
    def getContextName(line):
        context = line[line.rfind(" ")+1:]
        if context.endswith(".") :
            name = context[0:-1]
        else :
            name = context[0: context.rfind(".")]
        return name

    @staticmethod
    def getWordCompleteResult(base):
        (row,col) = vim.current.window.cursor
        visibleRowNum = Parser.getVisibleRowNum(row-1)
        vim_buffer = vim.current.buffer
        bufferText = "\n".join([vim_buffer[row] for row in visibleRowNum])
        #bufferText = "\n".join([line for line in vim.current.buffer])

        pattern = r"\b%s\w*\b" % base.replace("*","\w*")
        matches = re.findall(pattern,bufferText)
        completeList = []
        if matches :
            for item in matches :
                if item not in completeList :
                    completeList.append(item)
        else :
            pattern = r"\b\w+\b"
            matches = re.findall(pattern,bufferText)
            for item in matches :
                if SzJdeCompletion.simpleMatch(item,base) and item not in completeList:
                    completeList.append(item)

        return completeList

    @staticmethod
    def getWordFuzzyCompleteResult(base):
        vim_buffer = vim.current.buffer
        bufferText = "\n".join(vim_buffer)
        pattern = r"\b\w+\b"
        matches = re.findall(pattern,bufferText)
        completeList = set()
        if not matches :
            return []
        for item in matches :
            completeList.add(item)
        result = SzJdeCompletion.getCloseMatches(base,completeList)
        return result

    @staticmethod
    def getCloseMatches(base,completeList):
        result = [] 
        for item in completeList :
            if SzJdeCompletion.simpleMatch(item,base):
                result.append(item)
        if result :
            return result
        return difflib.get_close_matches(base,completeList)

    @staticmethod
    def simpleMatch(value,pat):
        pat_len=len(pat)
        value_len = len(value)
        if value_len < pat_len :
            return False
        # first char must same
        if value[0] != pat[0] :
            return False

        value_index = 0
        noMatch = False
        for i in range(0,pat_len):
            if value[value_index] == pat[i] :
                value_index += 1
            else :
                while value[value_index] != pat[i] :
                    if value_index >= value_len - 1  :
                        noMatch = True
                        break
                    value_index += 1
            if noMatch :
                return False

        return True

    @staticmethod
    def getMemberCompleteResult(completionType,base):
        (row,col) = vim.current.window.cursor
        vim_buffer = vim.current.buffer
        line = vim_buffer[row-1]
        current_file_name = vim_buffer.name
        classPathXml = ProjectManager.getClassPathXml(current_file_name)

        dotExpParser = Parser.getJavaDotExpParser()
        expTokens = dotExpParser.searchString(line[0:col])[0]
        needParenthesis = True
        if (len(line) > col ) and line[col] == "(" :
            needParenthesis = False
        varName = expTokens[0]
        superClass = Parser.getSuperClass()
        # expTokens not include base and type
        if expTokens[-1] != "." :
            expTokens = expTokens[1:-1]
        else :
            expTokens = expTokens[1:]

        pat = re.compile("^%s.*" % base.replace("*",".*"), re.IGNORECASE)
        #pat = re.compile("^%s.*" %base, re.IGNORECASE)
        result = []


        if varName[0].isupper():
            classname = varName
            completionType = "classmember"
        elif varName == "super" :
            classname = superClass
            completionType = "inheritmember"
        elif varName == "this" :
            classname = "this"
            completionType = "inheritmember"
        elif varName.endswith("()") : 
            expTokens.insert(0,varName)
            classname = "this"
            completionType = "objectmember"
        else :
            classname = Parser.getVarType(varName,row-1)
            completionType = "objectmember"

        if not classname :
            if not superClass : return []
            expTokens.insert(0,varName)
            classname = superClass
            completionType = "inheritmember"

        classNameList = Parser.getFullClassNames(classname)
        params =(current_file_name,classNameList,classPathXml,completionType,expTokens)
        memberInfoLines = Talker.getMemberList(params).split("\n")

        memberInfos = []
        for line in memberInfoLines :
            if line == "" : continue
            mtype,mname,mparams,mreturntype,mexceptions = line.split(":")
            memberInfos.append( (mtype,mname,mparams,mreturntype) )

        if varName == "this" and len(expTokens) < 2 :
            members = Parser.parseAllMemberInfo(vim_buffer)
            for mname,mtype,mreturntype,mparams,lineNum in members :
                memberInfos.append((mtype,mname,mparams, mreturntype))

        result = SzJdeCompletion.buildCptDictArrary(memberInfos, pat,base,needParenthesis)
        
        return result

    @staticmethod
    def buildCptDictArrary(memberInfos,pat,base,needParenthesis=True):
        result = []
        for memberInfo in memberInfos :
            mtype,mname,mparams,mreturntype = memberInfo
            if not pat.match(mname): continue
            menu = SzJdeCompletion.buildCptMenu(mtype,mname,mparams,mreturntype,needParenthesis)
            result.append(menu)

        if len(result) == 0 :
            names = list(set([ mname for mtype,mname,mparams,mreturntype in memberInfos]))
            matched_names = SzJdeCompletion.getCloseMatches(base,names)
            for memberInfo in memberInfos :
                mtype,mname,mparams,mreturntype = memberInfo
                if mname in matched_names: 
                    menu = SzJdeCompletion.buildCptMenu(mtype,mname,mparams,mreturntype,needParenthesis)
                    result.append(menu)

        return result

    @staticmethod
    def buildCptMenu(mtype,mname,mparams,mreturntype,needParenthesis):
        menu = dict()
        padStr = ""
        if needParenthesis :
            padStr = "("
        menu["icase"] = "1"
        menu["dup"] = "1"
        if mtype == "method" :
            menu["word"] = mname + padStr
            menu["abbr"] = "%s(%s) : %s " % (mname,mparams,mreturntype)
        else :
            menu["word"] = mname
            menu["abbr"] = "%s : %s " % (mname,mreturntype)
        return menu

    @staticmethod
    def getMemberTypeAbbr(mtype):
        if mtype=="method":
            return "f"
        if mtype=="field":
            return "v"
        if mtype=="constructor":
            return "d"
        return "x"

    @staticmethod
    def getCompleteResult(base):
        (row,col) = vim.current.window.cursor
        vim_buffer = vim.current.buffer
        line = vim_buffer[row-1]
        current_file_name = vim_buffer.name
        classPathXml = ProjectManager.getClassPathXml(current_file_name)
        if not classPathXml :
            return str([])
        completionType = SzJdeCompletion.getCompleteType(line[0:col])
        result = []
        if completionType == "package" :
            pkgname = SzJdeCompletion.getContextName(line)
            names = Talker.getPackageList(pkgname,classPathXml).split("\n")
            result = SzJdeCompletion.filterList(names,base)
        elif completionType == "member" :
            result = SzJdeCompletion.getMemberCompleteResult(completionType,base)
        elif completionType == "word" :
            classNameMenus = []
            if base[0].isupper():
                classNameList = Talker.getClassList(base,classPathXml).split("\n")
                for className in classNameList :
                    menu = dict()
                    shortName = className[ className.rfind(".") + 1 : ]
                    if shortName in result :
                        continue
                    menu["kind"] = "c"
                    menu["dup"] = "1"
                    menu["word"] = shortName
                    menu["menu"] = className
                    classNameMenus.append(menu)
            #try get work complete in scope visible lines.
            result = SzJdeCompletion.getWordCompleteResult(base)

            #try get member complete in buffer.
            pat = re.compile("^%s.*" % base.replace("*",".*"), re.IGNORECASE)
            if base[0].isupper():
                pat = re.compile("^%s.*" % base.replace("*",".*"))
            members = Parser.parseAllMemberInfo(vim_buffer)
            memberInfos = [(mtype,name,param,rtntype) for name,mtype,rtntype,param,lineNum in members]
            bufmembers = SzJdeCompletion.buildCptDictArrary(memberInfos, pat,base)

            for item in bufmembers :
                result.append(item)

            #try get member complete in supper class 
            if len(bufmembers) == 0 :
                completionType = "inheritmember"
                classNameList = ["this"]
                expTokens = []
                params =(current_file_name,classNameList,classPathXml,completionType,expTokens)
                memberInfos = []
                memberInfoLines = Talker.getMemberList(params).split("\n")
                
                for line in memberInfoLines :
                    if line == "" : continue
                    mtype,mname,mparams,mreturntype,mexceptions = line.split(":")
                    memberInfos.append( (mtype,mname,mparams,mreturntype) )
                inheritMembers = SzJdeCompletion.buildCptDictArrary(memberInfos, pat,base)
                for item in inheritMembers :
                    result.append(item)

            result.extend(classNameMenus)

            if len(result) == 0 :
                result = SzJdeCompletion.getWordFuzzyCompleteResult(base)

        if len(result) > MAX_CPT_COUNT :
            result = result[0:MAX_CPT_COUNT]
        return str(result)

class JdbTalker(object):
    @staticmethod
    def send(params):
        s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        s.connect((HOST, PORT))
        sb = []
        for item in params :
            sb.append("%s=%s\n" %(item,params[item]))
        sb.append('%s\n' % END_TOKEN)
        s.send("".join(sb))
        total_data=[]
        while True:
            data = s.recv(8192)
            if not data: break
            total_data.append(data)
        s.close()
        return ''.join(total_data)

    @staticmethod
    def submit(cmdLine,xmlPath,serverName):
        params = dict()
        params["cmd"]="debug"
        params["debugCmdArgs"] = cmdLine
        params["classPathXml"] = xmlPath
        params["serverName"] = serverName
        data = JdbTalker.send(params)
        return data

class Jdb(object):

    def __init__(self):
        self.cur_dir = os.getcwd()
        self.bp_data = {}
        fake_path = os.path.join(self.cur_dir,"fake")
        self.project_root = ProjectManager.getProjectRoot(fake_path)
        self.class_path_xml = ProjectManager.getClassPathXml(fake_path)
        self.serverName = vim.eval("v:servername")
        self.suspendRow = -1
        self.suspendBufnr = -1
        self.suspendBufName = ""
        self.display = True
        self.cmd_buf_list = []
        self.out_buf_list = []
        self.ivp = InspectorVarParser()

    def show(self):
        self.display = True
        self.defaultClassName = Parser.getMainClass()
        # 30% height
        height = vim.eval("winheight(0) / 10 * 3")
        #vim.command("call SwitchToSzToolView('JdeConsole','belowright','%s')" % height)
        #vim.command("call SwitchToSzToolView('Jdb','aboveleft','7')")
        vim.command("call SwitchToSzToolView('Jdb','belowright','%s')" % height)
        vim.command("call SwitchToSzToolViewVertical('JdbStdOut')")
        if self.out_buf_list :
            output(self.out_buf_list)
        vim.command("call SwitchToSzToolView('Jdb')")
        vim.command("vertical resize 45")
        buffer=vim.current.buffer

        if self.cmd_buf_list :
            output(self.cmd_buf_list)
            buffer=vim.current.buffer
            cur_row = len(buffer)
            cur_col = len(buffer[-1])
        else :
            output(">",buffer,False)
            cur_row =1 
            cur_col =1
        
        vim.current.window.cursor = (cur_row,cur_col)
        #vim.command("startinsert")
        vim.command("imap <buffer><silent><cr>  <Esc>:python jdb.executeCmd()<cr>")
        vim.command("nnoremap <buffer><silent><cr>   :python jdb.executeCmd(insertMode=False)<cr>")

    def __str__(self):

        return """Jdb : { cur_dir : %s,
                project_root : %s ,
                classPathXml : %s } 
                """ % (self.cur_dir, self.project_root, self.class_path_xml)

    def handleJdiEvent(self,args):
        if args[0] == "suspend" :
            self.handleSuspend(args[1],args[2],args[3])
        elif args[0] == "msg" :
            buffer = VimUtil.getSzToolBuffer("JdbStdOut", createNew = False )
            if (buffer == None ) :
                print args[1]
                return
            vim.command("call SwitchToSzToolView('JdbStdOut')")
            self.stdout(args[1])
            buffer=vim.current.buffer
            row = len(buffer)
            vim.current.window.cursor = (row, 0)
            vim.command("call SwitchToSzToolView('Jdb')")
            vim.command("redraw")

    def switchSourceBuffer(self):
        for i in range(1,6):
            vim.command("%swincmd w" % str(i))    
            bufname = vim.current.buffer.name
            if bufname == None :
                continue
            if vim.eval("&buftype") == "" \
                    or bufname.endswith(".temp_src") \
                    or bufname.endswith(".java") :
                break

    def handleSuspend(self,abs_path,lineNum,className):
        tab_count = int(vim.eval("tabpagenr('$')"))
        for i in range(1,tab_count+1):
            jdbvar = vim.eval('gettabvar('+str(i)+',"jdb_tab")')
            if jdbvar == "true" :
                vim.command("tabn %s" % str(i)) 
                break

        self.switchSourceBuffer()
        
        bufnr=str(vim.eval("bufnr('%')"))
        if os.path.exists(abs_path) or abs_path.startswith("jar:") :
            if not PathUtil.same_path(abs_path, vim.current.buffer.name):
                #if abs_path != vim.current.buffer.name :
                vim.command("edit %s" % abs_path)
                bufnr=str(vim.eval("bufnr('%')"))
                signcmd="sign place 1 line=1 name=SzjdeFR buffer=%s" % str(bufnr)
                vim.command(signcmd)
            
            HighlightManager.addSign(vim.current.buffer.name,lineNum,"S")
            self.suspendRow = lineNum
            self.suspendBufnr = bufnr
            self.suspendBufName = vim.current.buffer.name
           
            winStartRow = int(vim.eval("line('w0')"))
            winEndRow = int(vim.eval("line('w$')"))
            lineNum = int(lineNum)
            if lineNum < winStartRow or lineNum > winEndRow :
                vim.command("normal %sG" % str(lineNum))
                vim.command("normal zt")
                vim.command("redraw")

        else :
            vim.command("edit .temp_src")
            vim.command("setlocal buftype=nowrite")
            vim.current.buffer.append("the source code can't be found")
            vim.current.buffer.append("class name is " + className)
            vim.current.buffer.append("the current line is " + lineNum)
        if (self.display == False ) :
            self.show()
        else :
            vim.command("call SwitchToSzToolView('Jdb')")

        data = JdbTalker.submit("show_watch",self.class_path_xml,self.serverName)
        if data : 
            self.stdout(data)
        vim.command("call foreground()")

    def resumeSuspend(self):
        if self.suspendRow == -1 :
            return
        HighlightManager.removeSign(self.suspendBufName,self.suspendRow,"S",bufnr=self.suspendBufnr)
        self.suspendRow = -1

    @staticmethod
    def runApp():
        global jdb
        tab_count = int(vim.eval("tabpagenr('$')"))
        cur_tab = int(vim.eval("tabpagenr()"))
        for i in range(1,tab_count+1):
            if i == cur_tab :
                vim.command('call settabvar('+str(i)+',"jdb_tab","true")')
            else :
                vim.command('call settabvar('+str(i)+',"jdb_tab","false")')
        
        if "jdb" not in globals() :
            jdb = Jdb()
        jdb.show()

    def stdout(self,msg):
        buffer = VimUtil.getSzToolBuffer("JdbStdOut")
        output(msg,buffer,False)

    def editBufOut(self,msg):
        buffer = VimUtil.getSzToolBuffer("Jdb")
        output(msg,buffer,True)

    def printHelp(self):
        help_file = open(os.path.join(SzToolsConfig.getShareHome(),"doc/jdb.help"))
        content = [line.rstrip() for line in help_file.readlines()]
        help_file.close()
        self.stdout(content)

    def closeBuffer(self):
        self.display = False
        vim.command("bw! SzToolView_Jdb")
        vim.command("bw! SzToolView_JdbStdOut")

    def getCmdLine(self):
        work_buffer = vim.current.buffer
        row,col = vim.current.window.cursor
        return work_buffer[row-1]

    def appendPrompt(self):
        self.editBufOut(">")
        buffer=vim.current.buffer
        row = len(buffer)
        col = len(buffer[-1])
        vim.current.window.cursor = (row, col)
        vim.command("startinsert")

    def stepCmd(self, cmd):
        self.resumeSuspend()
        data = JdbTalker.submit(cmd,self.class_path_xml,self.serverName)

    def removeDuplicate(self):
        curBuf = vim.current.buffer
        lineset = set()
        rowNum = len(curBuf)
        while True :
            if rowNum == 0 :
                break
            trimed_line = curBuf[rowNum-1].strip()
            if trimed_line in lineset :
                del curBuf[rowNum-1]
            else :
                lineset.add(curBuf[rowNum-1])
            rowNum = rowNum -1

    def executeCmd(self, insertMode = True):
        
        #if self.project_root == None or not os.path.exists(self.project_root) :
        #    return 
        cmdLine = self.getCmdLine()

        if cmdLine.strip() == "" :
            self.appendPrompt()
            return

        cmdLine = cmdLine.replace("\ ","$$").strip()[1:]
        #remove duplicate line 
        self.removeDuplicate()

        if cmdLine == "wow":
            self.stdout(self)
            self.appendPrompt()
            return 

        if cmdLine.startswith("print") :
            cmdLine = "eval " + cmdLine[5:]

        """
        need_gen_ast_cmds = ["eval","reftype","watch","unwatch","inspect"]
        for cmd_name in need_gen_ast_cmds :
            if cmdLine.startswith(cmd_name) :
                arg = cmdLine[ cmdLine.find(" ")+1 : ]
                ast = self.ivp.generate(arg)
                cmd = cmdLine[ 0 : cmdLine.find(" ") ]
                cmdLine = cmd + " " + ast
        """

        if cmdLine == "run" and self.defaultClassName :
            cmdLine = "run " + self.defaultClassName

        change_suspend_cmds = ["step_into","step_over","step_return","resume","exit","shutdown","frame","disconnect"]
        for cmd_name in change_suspend_cmds :
            if cmdLine.strip().split(" ")[0] in change_suspend_cmds :
                self.resumeSuspend()

        if cmdLine.startswith("help"):
            self.printHelp()
            self.appendPrompt()
            return

        if cmdLine.startswith("hide"):
            vim.command("call SwitchToSzToolView('Jdb')")
            lines = [line.strip() for line in vim.current.buffer]
            self.cmd_buf_list = lines
            vim.command("call SwitchToSzToolViewVertical('JdbStdOut')")
            lines = [line.strip() for line in vim.current.buffer]
            self.out_buf_list = lines
            self.closeBuffer()
            return 

        if cmdLine.startswith("run"):
            self.switchSourceBuffer()
            # 30% height
            height = vim.eval("winheight(0) / 10 * 3")
            vim.command("call SwitchToSzToolView('JdeConsole','belowright','%s')" % height)

        data = JdbTalker.submit(cmdLine,self.class_path_xml,self.serverName)
        if data : 
            self.stdout(data)

        if cmdLine.startswith("bpa") and data == "success" :
            args = cmdLine.split(" ")
            for i in range(1,5):
                vim.command("%swincmd w" % str(i))    
                if vim.eval("&buftype") == "" :
                    break
            EditUtil.addConditionalBreakpoint(args[2])

        if cmdLine == "exit" :
            self.closeBuffer()
            return 
        if cmdLine.startswith("clear"):
            for i in range(1,5):
                vim.command("%swincmd w" % str(i))    
                if vim.eval("&buftype") == "" :
                    break
            EditUtil.syncBreakpointInfo()

        vim.command("call SwitchToSzToolView('Jdb')")
        if insertMode :
            self.appendPrompt()

    def fetchJdbResult(self):
        resultText = JdbTalker.submit("fetchJdbResult",self.class_path_xml,self.serverName)
        if resultText == "" :
            return 
        lines = resultText.split("\n")
        VimUtil.writeToSzToolBuffer("JdeConsole",lines,append=True)

class InspectorVarParser():

    def __init__(self):
        self.parser = self.getParser()
        self.locs = []

    @staticmethod
    def convertNumbers(s,l,toks):
        n = toks[0]
        try:
            return int(n)
        except ValueError, ve:
            return float(n)

    def getParser(self):
        java_exp = Forward()
        java_num = Combine( Optional('-') + ( '0' | Word('123456789',nums) ) +
                            Optional( '.' + Word(nums) ) +
                            Optional( Word('eE',exact=1) + Word(nums+'+-',nums) ) )
                
        java_num.setParseAction( InspectorVarParser.convertNumbers )

        TRUE = Keyword("true").setParseAction( replaceWith(True) )
        FALSE = Keyword("false").setParseAction( replaceWith(False) )
        NULL = Keyword("null").setParseAction( replaceWith(None) )

        java_str = dblQuotedString.setParseAction( removeQuotes )

        param_atom = TRUE | FALSE | NULL | Group(java_exp) | java_str | java_num  

        func_param = Suppress("(")+Optional(delimitedList(param_atom)) + Suppress(")")
        array_index_exp = Suppress("[")+param_atom.setResultsName("arrayidx") + Suppress("]")

        post_exp = Optional(Group(func_param).setResultsName("params")) + Optional(array_index_exp)

        atom_exp =  Group(Word(alphas+"_",alphanums+"_").setResultsName("name") + post_exp)
        java_exp << atom_exp + Optional(OneOrMore(Suppress(".") + atom_exp)).setResultsName("members")

        java_exp_group = Group(java_exp)
        java_exp_group.setParseAction(self.store_token_starts)
        return delimitedList(java_exp_group)

    def store_token_starts(self, string, loc, tokens):
        self.locs.append(loc)

    def getClassNameFromEditBuf(self, name):
        for i in range(1,5):
            vim.command("%swincmd w" % str(i))    
            if vim.eval("&buftype") == "" :
                break
        fullName = Parser.getFullClassNames(name)
        vim.command("call SwitchToSzToolView('Jdb')")
        return fullName


    def buildAstTree(self, exp,parentEle, ori_exp_str = None):
        expType = "expression"
        
        if isinstance(exp,basestring) :
            expType = "string"
        elif exp == None :
            expType = "null"
            exp = "null"
        elif isinstance(exp,bool) and exp == True :
            expType = "boolean"
            exp = "true"
        elif isinstance(exp,bool) and exp == False :
            expType = "boolean"
            exp = "false"
        elif isinstance(exp,int) or isinstance(exp,float):
            expType = "number"
            exp = str(exp)

        if (expType != "expression") :
            ele = Element("exp",{'name' : exp,'exptype':expType})
            parentEle.append(ele)
            return

        ele = Element("exp",{'name' : exp[0].name})
        if not isinstance(exp[0].params,str):
            ele.set("method","true")

        if ori_exp_str :
            ele.set("oriExp", ori_exp_str)

        if parentEle.tag == "root" and exp[0].name[0].isupper():
            fullName = self.getClassNameFromEditBuf(exp[0].name)[0]
            ele.set("name",fullName)
            ele.set("clazz", "true")

        if exp[0].params :
            paramsEle = Element("params")
            for param in exp[0].params:
                self.buildAstTree(param,paramsEle)
            ele.append(paramsEle)

        if exp[0].arrayidx !=None :
            if not isinstance(exp[0].arrayidx,basestring) or exp[0].arrayidx !="" :
                arrayidxEle = Element("arrayidx")
                self.buildAstTree(exp[0].arrayidx, arrayidxEle)
                ele.append(arrayidxEle)

        if exp.members:
            membersEle = Element("members")
            ele.append(membersEle)
            for item in exp.members:
                if item.name[0].isupper():
                    item.name = self.getClassNameFromEditBuf(item.name)[0]
                memberExpEle = Element("exp",{'name' : item.name})
                if not isinstance(item.params,str):
                    memberExpEle.set("method","true")
                membersEle.append(memberExpEle)
                if item.params :
                    paramsEle = Element("params")
                    for param in item.params:
                        self.buildAstTree(param,paramsEle)
                    memberExpEle.append(paramsEle)
        parentEle.append(ele)

    def generate(self, exp):
        self.locs = []
        stringIO = StringIO.StringIO()
        root = Element('root')
        eleTree = ElementTree(root)
        parseResult = self.parser.parseString(exp)

        ori_exp_names = []
        for loc in self.locs[::-1]:
            ori_exp = exp[loc:].strip()
            if ori_exp.endswith(","):
                ori_exp = ori_exp[0:-1]
            exp = exp[0 : loc]
            ori_exp_names.insert(0,ori_exp)

        index = 0
        for item in  parseResult :
            self.buildAstTree(item,root, ori_exp_names[index])
            index += 1
        eleTree.write(stringIO)
        msg = stringIO.getvalue()
        return msg

if __name__ == "__main__" :
    #ivp = InspectorVarParser()
    #result = ivp.generate('a[what.get(10)]')
    #print result
    pass
