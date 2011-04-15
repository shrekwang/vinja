import vim
import os.path
import re
import sys
import logging

conn_pool = {}

class QueryUtil(object):

    @staticmethod
    def queryDataBases():
        bufnumber= vim.eval("bufnr('%')")
        db_profile = dbext.getDbOption()
        if db_profile == None :  
            return
        outBuffer = Dbext.getOutputBuffer()
        name = getVisualBlock()
        server_type = db_profile["servertype"]
        if server_type == "mssql":
            sql = """ SELECT name 
                FROM master.dbo.sysdatabases
                WHERE  DATABASEPROPERTYEX(name, 'Status') = 'ONLINE'
                and name not in ('master','tempdb','model','msdb') """
        elif server_type == "mysql":
            sql = " show databases "

        columns,result = dbext.query(sql)

        if not columns :
            output(result, outBuffer)
        else :
            output(dbext.format(columns, result), outBuffer)

    @staticmethod
    def queryTables():
        name = getVisualBlock()
        columns , result = QueryUtil.queryTablesByName(name)
        outBuffer = Dbext.getOutputBuffer()
        if not columns :
            output(result, outBuffer)
        else :
            output(dbext.format(columns, result), outBuffer)

    @staticmethod
    def getAllTables():
        name = ""
        columns , result = QueryUtil.queryTablesByName(name)
        tableList = []
        for dataRow in result :
            tableList.append(dataRow[0].lower())
        return tableList

    @staticmethod
    def queryTablesByName(name):
        bufnumber= vim.eval("bufnr('%')")
        db_profile = dbext.getDbOption()
        if db_profile == None :  
            return
        server_type = db_profile["servertype"]
        if server_type == "oracle":
            sql = """ select table_name from user_tables 
                where table_name like '%%%s%%' """ 
            sql = sql % name.upper()
        elif server_type == "mssql":
            sql = """SELECT name FROM sysobjects Where 
                name like '%%%s%%' and type = 'U' order by name"""
            sql = sql % name
        elif server_type == "mysql":
            sql = """ SELECT table_name FROM INFORMATION_SCHEMA.TABLES
            where table_name like '%%%s%%' """ % name

        columns,result = dbext.query(sql)
        return columns, result
        

    @staticmethod
    def descTable():
        bufnumber= vim.eval("bufnr('%')")
        db_profile = dbext.getDbOption()
        if db_profile == None :  
            return

        outBuffer = Dbext.getOutputBuffer()
        name = getVisualBlock()
        server_type = db_profile["servertype"]
        if server_type == "oracle":
            schema = db_profile["user"]
            if name.find(".") > -1: 
                schema, name = name.split(".")
            sql = """select column_name,data_type,data_length from all_tab_columns 
                where table_name = '%s' and owner = '%s' """ % (name.upper(), schema.upper())

        elif server_type == "mssql":
            sql = """select syscolumns.name column_name,systypes.name
                        data_type,syscolumns.length
                        from sysobjects
                            join syscolumns on sysobjects.id = syscolumns.id
                            join systypes on syscolumns.xtype = systypes.xtype
                        where sysobjects.name = '""" + name + "'"

        elif server_type == "mysql":
            sql = """ SELECT COLUMN_NAME, DATA_TYPE,  CHARACTER_MAXIMUM_LENGTH , IS_NULLABLE
                          FROM INFORMATION_SCHEMA.COLUMNS
                          WHERE table_name = '""" + name +"'"

        columns,result = dbext.query(sql)
        if not columns :
            output(result,outBuffer)
        else :
            output(dbext.format(columns, result), outBuffer)

    @staticmethod
    def generateSQL():

        db_profile = dbext.getDbOption()
        if db_profile == None :  
            return
        name = getVisualBlock()
        outBuffer = Dbext.getOutputBuffer()
        server_type = db_profile["servertype"]
        if server_type == "oracle":
            sql = """select column_name from user_tab_columns 
                where table_name = '%s' """ % name.upper()
        elif server_type == "mssql":
            sql = """select syscolumns.name from sysobjects
                    join syscolumns on sysobjects.id = syscolumns.id
                    join systypes on syscolumns.xtype = systypes.xtype
                where sysobjects.name = '%s' """ % name
            

        result = [row[0] for row in dbext.query(sql)[1]]

        values = (name, ",".join(result), ",".join("?"*len(result) ))
        insertSql = "insert into %s (%s) values (%s) \n" % values
        values = (name, " = ?,".join(result) )
        updateSql = "update %s set %s where id = ?" % values
        output(insertSql + updateSql, outBuffer)

class Dbext(object):

    @staticmethod 
    def runApp():
        global dbext
        dbext = Dbext()
        dbext.promptDbOption()

    @staticmethod
    def getOutputBuffer():
        bufnum= vim.eval("bufnr('%')")
        bufname = "SzToolView_dbext%s" %(bufnum)
        dbext_output_buffer = None
        Dbext.createOutputBuffer(bufnum)
        for buffer in vim.buffers:
            if buffer.name and buffer.name.find(bufname) > -1 :
                dbext_output_buffer = buffer
                break
        return dbext_output_buffer

    @staticmethod
    def createOutputBuffer(bufnum):
        vim.command("call SwitchToSzToolView('dbext%s')" %(bufnum) )
        vim.command("noremap <buffer> a 21zh")
        vim.command("noremap <buffer> f 21zl")
        vim.command("setlocal nostartofline")
        listwinnr = str(vim.eval("winnr('#')"))
        vim.command("exec '" + listwinnr + " wincmd w'")

    def queryVisualSQL(self):
        sql = getVisualBlock()
        outBuffer = Dbext.getOutputBuffer()
        append = False
        for index,item in enumerate(sql.split(";")):
            if item.strip() != "" :
                columns,result = dbext.query(item)
                if columns :
                    result = dbext.format(columns,result)
                if index!=0 :
                    append = True
                output(result,outBuffer,append)

    def loadConf(self,path):
        confs = []
        if not os.path.exists(path):
            print "db conf file not existed."
            return None
        file = open(path,"r")
        try :
            for line in file:
                if line.strip() == "" : continue
                if line.strip().startswith("#") : continue
                try :
                    confs.append(eval("dict(" + line + ")"))
                except :
                    pass
            return confs
        except :
            print "db conf file content is not valid."
            return None

    def existsConnParameter(self):
        exists = vim.eval("exists('b:connection_parameter')")
        if exists  ==  "1" :
            return True
        return False

    def promptTempOption(self):
        print "please input parameter as follows: dbtype,host,user,password,database"
        vim.command("let b:connection_parameter = input('')")
        if self.existsConnParameter():
            params = vim.eval("b:connection_parameter").split(",")
            if len(params) < 5 :
                vim.command("unlet b:connection_parameter")
                print "the parameter your enterd is not correct."

    def getTempProfile(self):
        db_profile = {}
        if self.existsConnParameter():
            try :
                params = vim.eval("b:connection_parameter").split(",")
                db_profile["servertype"] = params[0]
                db_profile["host"] = params[1]
                db_profile["user"] = params[2]
                db_profile["password"] = params[3]
                db_profile["database"] = params[4]
            except :
                print "get db temp connection parameter error"
                return {}
        return db_profile

    def promptDbOption(self):
        dbconfs = self.loadConf(os.path.join(getDataHome(), "db.conf"))
        if dbconfs == None :
            return
        for index,item in enumerate(dbconfs):
            if item["servertype"] == "sqlite" :
                print " %s : %s "  % ( str(index) , item["file"] )
            elif item["servertype"] == "oracle" :
                print " %s : %s %s %s " % (str(index), item["host"].ljust(16),item["sid"].ljust(12),item["user"])
            else :
                print " %s : %s %s" % (str(index), item["host"].ljust(16),item["user"])

        vim.command("let b:profile_index = input('please enter a selection')")
        #clear temp connection parameter
        if self.existsConnParameter() :
            vim.command("unlet b:connection_parameter")

        db_profile = self.getDbOption()
        if db_profile == None :  return

        if db_profile["servertype"] == "sqlite" :
            strTemplate = "setl statusline=\ Line:\ %%l/%%L:%%c\ \ File:'%s'\ "
            strValue = (db_profile["file"],)
        elif db_profile["servertype"] == "sqlite" :
            strTemplate = "setl statusline=\ Line:\ %%l/%%L:%%c\ \ Host:'%s'\ \ SID:'%s'"
            strValue = (db_profile["host"],db_profile["sid"])
        else :
            strTemplate = "setl statusline=\ Line:\ %%l/%%L:%%c\ \ Host:'%s'\ \ Database:'%s'"
            strValue = (db_profile["host"],db_profile["user"])

        vim.command(strTemplate % strValue )
        return

    def getDbOption(self):

        if self.existsConnParameter():
            db_profile = self.getTempProfile()
            return db_profile
        dbconfs = self.loadConf(os.path.join(getDataHome(),"db.conf"))
        if dbconfs == None : return
        if (len(dbconfs)==1) :
            db_profile = dbconfs[0]
            return db_profile

        exists = vim.eval("exists('b:profile_index')")
        if exists  ==  "1" :
            selection = vim.eval("b:profile_index")
            db_profile = dbconfs[int(selection)]
            return db_profile

        for index,item in enumerate(dbconfs):
            print str(index) + ": " + item["host"] + ":  " + item["database"]
        vim.command("let b:profile_index = input('please enter a selection')")
        selection = vim.eval("b:profile_index")
        db_profile = dbconfs[int(selection)]
        return db_profile

    def createConn(self,profile):
        conn = None
        server_type = profile["servertype"]
        if server_type == "oracle":
            import cx_Oracle
            codepage = sys.getdefaultencoding()
            if codepage  ==  "gbk" : os.environ['NLS_LANG']="SIMPLIFIED CHINESE_CHINA.ZHS16GBK"
            dns_tns = cx_Oracle.makedsn(profile["host"],1521,profile["sid"])
            conn = cx_Oracle.connect(profile["user"], profile["password"], dns_tns)
        elif server_type == "mssql":
            import pyodbc
            conn = pyodbc.connect(driver = '{SQL Server}',server=profile["host"],uid=profile["user"], \
                pwd = profile["password"] )
        elif server_type == "mysql":
            import MySQLdb
            conn = MySQLdb.connect (host = profile["host"] , user = profile["user"],\
                passwd = profile["password"] )
        elif server_type == "sqlite":
            import sqlite3 as sqlite
            conn = sqlite.connect(profile["file"])
        return conn

    def query(self, sql):

        db_profile = self.getDbOption()
        if db_profile == None :  return (None,"")

        global conn_pool
        bufnum= vim.eval("bufnr('%')")
        conn = conn_pool.get(bufnum)
        if conn == None :
            conn = self.createConn(db_profile)
            conn_pool[bufnum] = conn

        cur = None
        try :
            cur = conn.cursor()
            cur.execute(sql)
        except Exception, reason:
            if cur : 
                cur.close()
            server_type = db_profile["servertype"]
            if server_type == "mssql" :
                return (None,str(reason[1]))
            else :
                return (None,str(reason))
        columns = []
        result = []
        if cur.description:
            for column in cur.description:
                columns.append(column[0])
            result = cur.fetchall()
        else :
            result.append("affected " + str(cur.rowcount) + " rows.")

        conn.commit()
        cur.close()
        return columns,result

    def format(self,columns,rows):
        result = []
        maxlens = []
        for column in columns :
            maxlens.append(0)

        resultset = [columns]
        for row in rows :
            for index,field in enumerate(row):
                field = str(field).rstrip()
                if (len(field)>maxlens[index]):
                    maxlens[index] = len(field)
            resultset.append(row)

        for index,field in enumerate(columns):
            if (len(str(field))>maxlens[index]):
                maxlens[index] = len(str(field))

        headline = ""
        for item in maxlens:
            headline = headline + "+" + ("-"*item) + "--"
        headline = headline+ "+" 

        for rowindex,row in enumerate(resultset):
            line = ""
            for index,field in enumerate(row):
                field = str(field).rstrip().replace("\n","")
                line = line+ "| " + field.ljust(maxlens[index] + 1)
            if rowindex<2: result.append(headline)
            result.append(line + "|")
        result.append(headline)

        return result

    def exit(self):
        bufnum= vim.eval("bufnr('%')")
        bufname = "SzToolView_dbext%s" %(bufnum)
        vim.command("bw! %s" %bufname)
        dbext = None

class SzDbCompletion(object):

    @staticmethod
    def completion(findstart,base):
        if str(findstart) == "1":
            (row,col) = vim.current.window.cursor
            line = vim.current.buffer[row-1]

            index = 0
            for i in range(col-1,-1, -1):
                char = line[i]
                if char in " ;,.'()" :
                    index = i + 1
                    break

            cmd = "let g:SzCompletionIndex = %s" %str(index)
        else:
            result = SzDbCompletion.getCompleList(base)
            liststr = "['" + "','".join(result) + "']"
            cmd = "let g:SzCompletionResult = %s" % liststr
        vim.command(cmd)

    @staticmethod
    def getCompleList(base):
        (currentCurPos,sql,valueExp,parentContext) = SzDbCompletion.getContextInfo()
        tableDefStartPos = sql.find("from")
        match = re.search(r"(\bwhere\b)|(\bgroup\b)|(\border\b)",sql,flags=re.IGNORECASE)
        if (match) :
            tableDefEndPos = match.start()
        else :
            tableDefEndPos = len(sql)

        if tableDefStartPos < 0 :
            completeType = "keyword"
        elif currentCurPos > tableDefStartPos and currentCurPos <= tableDefEndPos :
            completeType = "table"
        else :
            completeType = "column"

        if valueExp :
            completeType = "word"

        if completeType == "keyword" :
            keywords = ["select","update","from","delete","group","order","where"]
            for item in keywords :
                if item.startswith(base):
                    return [item]

        completeList = []
        if completeType == "column":
            fromClause = sql[tableDefStartPos + 4 : tableDefEndPos].split(",")
            tables = []
            contextTable = None
            for item in fromClause:
                alias = ""
                tableDef = item.strip()
                spaceIndex = tableDef.find(" ")
                if spaceIndex > -1 :
                    alias = tableDef[spaceIndex + 1 :].strip()
                    tableDef = tableDef[0:spaceIndex]
                if parentContext :
                    if tableDef == parentContext or alias == parentContext:
                        contextTable = tableDef
                        break
                tables.append(tableDef)
            if contextTable :
                completeList = SzDbCompletion.getColumnList([contextTable])
            else :
                completeList = SzDbCompletion.getColumnList(tables)
        elif completeType == "table" :
            if parentContext :
                completeList = SzDbCompletion.getTableList(parentContext)
            else :
                completeList = QueryUtil.getAllTables()
        else  :
            filebufferText = "\n".join([unicode(line) for line in vim.current.buffer])
            outBufferText = "\n".join([unicode(line) for line in Dbext.getOutputBuffer()])
            bufferText = "%s\n%s" %(filebufferText, outBufferText) 
            pattern = unicode(r"""%s[^\s'"]*""" % base.replace("*","\S+") )
            matches = re.findall(pattern,bufferText)
            completeList = []
            if matches :
                for item in matches :
                    if item not in completeList :
                        completeList.append(item)
            return completeList

        result = SzDbCompletion.filterList(completeList,base)
        return result

    @staticmethod
    def filterList(srcList,exp):
        exp = exp.upper()
        if exp.find("*") > -1 :
            pat = re.compile("^%s$" % exp.replace("*",".*"))
        else:
            pat = re.compile(".*%s.*" %exp)

        result = [str(item) for item in srcList if pat.match(item.upper())]
        return result

    @staticmethod
    def getColumnList(tableList):
        db_profile = dbext.getDbOption()
        if db_profile == None :  return []
        server_type = db_profile["servertype"]
        schema = db_profile["user"]

        if server_type == "oracle":
            conditions = []
            for tableName in tableList :
                if tableName.find(".") > -1: 
                    schema, tableName = tableName.split(".")
                conditions.append("( table_name = '%s' and owner = '%s' )" % ( tableName.upper(),schema.upper()) )
            conditionStr = " or ".join(conditions)
            sql = "select lower(column_name) from all_tab_columns where %s " % conditionStr 

        elif server_type == "mssql":
            tableCon = "','".join(tableList)
            tableCon = "'%s'" % tableCon
            sql = """select syscolumns.name column_name from sysobjects
                join syscolumns on sysobjects.id = syscolumns.id where sysobjects.name in (%s) """ % tableCon

        tmpcolumns,result = dbext.query(sql)
        columns = []
        for dataRow in result :
            columns.append(dataRow[0])
        return  columns

    @staticmethod
    def getTableList(schema):
        db_profile = dbext.getDbOption()
        if db_profile == None :  return []
        server_type = db_profile["servertype"]
        if server_type == "oracle":
            sql = "select lower(table_name) from all_tables where owner = '%s'" % schema.upper()
        elif server_type == "mssql":
            sql = "SELECT lower(name) FROM sysobjects where type = 'U' "

        tmpcolumns,result = dbext.query(sql)
        tables = []
        for dataRow in result :
            tables.append(dataRow[0])
        return  tables
        


    @staticmethod
    def getContextInfo():
        (row,col) = vim.current.window.cursor
        buffer = vim.current.buffer
        startRow = 0
        endRow = len(buffer)

        for i in range(row-1,-1, -1):
            if buffer[i].strip() == "" or buffer[i].endswith(";") :
                startRow = i + 1
                break

        for i in range(row,endRow):
            if buffer[i].strip() == "" or buffer[i].endswith(";") :
                endRow = i
                break

        if endRow == 1 :
            sql = buffer[0]
        else :
            sql = " ".join(buffer[startRow:endRow])

        currentCurPos = 0
        for i in range(startRow, row-1):
            print currentCurPos , len(buffer[i]), i
            currentCurPos = currentCurPos + len(buffer[i])
        currentCurPos = col + currentCurPos

        currentLine = buffer[row-1][0:col]
        if currentLine.find(" ") > -1 :
            lastToken = currentLine[currentLine.rfind(" ") + 1 : ]
        else :
            lastToken = currentLine

        valueExp = False
        if lastToken.find("'") > -1 :
            valueExp = True

        parentContext = None
        if lastToken.find(".") > -1 :
            parentContext = lastToken[0:lastToken.rfind(".")]

        return currentCurPos, sql, valueExp, parentContext

