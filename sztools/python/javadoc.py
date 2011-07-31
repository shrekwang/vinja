from BeautifulSoup import *
import re,os, os.path, urllib2 

class SqliteDbManager(object):

    def __init__(self,db_path):
        self.db_path = db_path

    def get_conn(self):
        conn=sqlite.connect(self.db_path)
        return conn


    def  update(self, sql, params = None):
        conn=self.get_conn()
        cur=conn.cursor()
        if isinstance(sql,list) :
            for item in sql :
                cur.execute(item)
        elif isinstance(sql, basestring):
            if params :
                if isinstance(params, basestring):
                    cur.execute(sql,(params,))
                elif isinstance(params[0], list) or isinstance(params[0],tuple):
                    for param in params :
                        cur.execute(sql,param)
                else :
                    cur.execute(sql,params)
            else :
                cur.execute(sql)
        conn.commit()
        conn.close()


    def query(self,selectSql,parameters=None):
        conn=self.get_conn()
        cur=conn.cursor()
        if parameters :
            if isinstance(parameters , basestring):
                cur.execute(selectSql,(parameters,))
            else :
                cur.execute(selectSql,parameters)
        else :
            cur.execute(selectSql)
        rows = cur.fetchall()
        conn.close();
        return rows

class PageCache(object):
    def __init__(self):
        self.jdoc_db_path=os.path.join(SzToolsConfig.getDataHome(), "jdoc.dat")
        self.dbm = SqliteDbManager(self.jdoc_db_path )
        if not os.path.exists(self.jdoc_db_path):
            self.initDb()

    def initDb(self):
        path=os.path.dirname(self.jdoc_db_path)
        if not os.path.exists(path):
          os.makedirs(path)
        create_jdoc_sql="create table jdoc(class_name varchar(100), package varchar(200), homeid int )"
        create_links_sql="create table homelinks( id INTEGER PRIMARY KEY, url varchar(200))"
        self.dbm.update([create_jdoc_sql, create_links_sql])

    def saveHomeLink(self, homelink):
        self.dbm.update("insert into homelinks(url) values (?)", homelink)
        row=self.dbm.query("select max(id) from homelinks where url = ? " , homelink)
        return row[0][0]

    def existsHomeLink(self, homelink):
        row=self.dbm.query("select count(*) from homelinks where url = ? " , homelink)
        if row[0][0] == 0 :
            return False
        return True

    def saveJdocData(self, batch_data):
        self.dbm.update("insert into jdoc(class_name, package,homeid) values (?,?,?)" , batch_data)

    def getAllJdocData(self ) :
        sql ="select  package ,class_name from jdoc order by package "
        rows=self.dbm.query(sql)
        return rows

    def searchUrl(self, cname,pname):
        sql ="""select b.url, a.package, a.class_name from jdoc a, homelinks b  
            where a.homeid =b.id and a.class_name = ? and a.package = ? """
        rows=self.dbm.query(sql, (cname,pname))
        urls = []
        for row in rows :
            home = row[0]
            package = row[1]
            class_name = row[2]
            relativeUrl= package.replace(".","/")+"/"+class_name + ".html"
            url = "%s%s" %(  home.replace("\n","") , relativeUrl )
            urls.append(url)
        return urls

class Javadoc(object):

    def __init__(self) :
        self.cache = PageCache()
        print "cacheing class list"
        self.cacheClassList()
        self.updateIndexView()

    @staticmethod
    def runApp():
        global jdocviewer
        if not "jdocviewer" in  globals() :
            jdocviewer = Javadoc()

    def exit(self):
        vim.command("bw! SzToolView_jdoc_index")
        vim.command("bw! SzToolView_jdoc_content")
        jdocviewer = None

    def wrapLine(self ,texts, columnWidth = 80, prefix ="" ) :
        result = []
        tempStr = ""
        for item in texts:
            if item.find("\n") > -1 :
                result.append(prefix+tempStr+item.replace("\n",""))
                tempStr = ""
            elif len(tempStr + item) > 80  :
                result.append(prefix+tempStr)
                tempStr = item
            else :
                tempStr = tempStr + " " + item
        result.append(tempStr)
        return "\n".join(result)

    def replaceEntity(self,value):
        value = value.replace("&nbsp;"," ")
        value = value.replace("&gt;",">")
        value = value.replace("&lt;","<")
        return value

    def extractText(self, texts,tag):
        if  isinstance(tag, unicode) :
            if tag.strip() != "" :
                texts.append(self.replaceEntity(tag))

    def recursiveExtractText(self, texts,node):
        if hasattr(node,"name") and node.name=="br" : 
            texts.append("<br>")
        elif hasattr(node,"name") and node.name=="hr" : 
            texts.append("-"*80 +"<br>")
        for item in node.contents:
            if isinstance(item, unicode) :
                if item.strip() != "" :
                    texts.append(self.replaceEntity(item))
            else :
                self.recursiveExtractText(texts,item)

    def extractDD(self, tag):
        texts = []
        self.recursiveExtractText(texts,tag)
        result =[]
        tempStr = ""
        for item in texts:
            if len(tempStr + item) > 80 and len(item.strip()) > 2 :
                result.append("    "+tempStr)
                tempStr = item
            else :
                tempStr = tempStr + item
        result.append(tempStr)
        return "\n".join(result)

    def extractTR(self,tag):
        texts = []
        # table head row
        pre = post = ""
        if hasattr(tag, "class" ) and tag.get("class") == "TableHeadingColor" :
            pre = post = "\n"+"="*80+"\n"
        else :
            pre = ""
            post = "\n"+"-"*80 +"\n"

        self.recursiveExtractText(texts,tag)
        result = []
        for item in texts :
            item = item.replace("\n", " ")
            item = item.replace("<br>", "\n")
            result.append(item)

        trtokens = " ".join(result).split(" ")
        lines=self.wrapLine(trtokens) 
        return pre + lines + post


    def parseJavaDoc(self,classname,package) :
        vim.command("echo 'parsing java doc page.....'")
        urls = self.cache.searchUrl(classname,package)
        if not urls : 
            output("not result")
            return 
        page = urllib2.urlopen(urls[0])
        soup = ICantBelieveItsBeautifulSoup(page)
        node = soup.find("html")
        classdata=[]
        start_class_data = False


        while True :
            if node == None : break
            if isinstance( node, Comment) :
                if node.find("START OF CLASS DATA") > -1 :
                    start_class_data = True
                elif node.find("END OF CLASS DATA") > -1 :
                    start_class_data = False
                node = node.next

            if  start_class_data :
                tagname=getattr(node,"name","")
                if tagname == "dd" :
                    classdata.append("\n    ")
                    classdata.append(self.extractDD(node))
                    if node.nextSibling :
                        node = node.nextSibling
                    else :
                        node = node.next
                elif tagname == "tr" :
                    classdata.append(self.extractTR(node))
                    node = node.nextSibling
                elif tagname in ("p","dt") :
                    classdata.append("\n")
                    node = node.next
                elif tagname == "hr" :
                    classdata.append("\n"+"-"*80 +"\n")
                    node = node.next
                else :
                    self.extractText(classdata, node)
                    node = node.next
            else :
                node = node.next
        result = " ".join([item.replace("&nbsp;"," ") for item in classdata])
        result = self.squeeze(result)
        output(result)

    def showJavaDoc(self):
        (row, col) = vim.current.window.cursor
        class_name=vim.current.buffer[row-1]
        package = ""
        for row_num in range(row-1, -1, -1 ) :
            if not vim.current.buffer[row_num].startswith(" ") :
                package = vim.current.buffer[row_num]
                break
        vim.command("call SwitchToSzToolView('jdoc_content')" )
        vim.command("setlocal nonumber")
        vim.command("setlocal wrap")
        self.parseJavaDoc(class_name.strip(), package.strip())

    def squeeze(self,content):
        pattern = re.compile(r"(^\s$\n)+", re.MULTILINE)
        result= pattern.sub("\n",content)
        return result

    def cacheClassList(self):
        docListFile=os.path.join(SzToolsConfig.getShareHome(),"conf/javadoc.cfg")
        urls = open(docListFile).readlines()

        for url in urls :
            batch_data = [] 
            if url.startswith("#") :
                continue 
            if self.cache.existsHomeLink(url) :
                continue
            homeid = self.cache.saveHomeLink(url)
            url = "%s%s" % ( url.replace("\n",""), "allclasses-frame.html")
            page = urllib2.urlopen(url)
            soup = BeautifulSoup(page)
            for link in soup('a') :
                packagename=link.get("href").replace(".html","").replace("/",".")
                #the last part of href is classname, get rid of it.
                packagename = packagename[0: packagename.rfind(".")]
                classname=link.text
                batch_data.append( (classname, packagename, homeid) )
            self.cache.saveJdocData(batch_data)


    def updateIndexView(self):

        vim.command("call SwitchToSzToolView('jdoc_index')" )
        jdocData = self.cache.getAllJdocData()
        result = []
        current_package = "" 
        package_start_index= 1
        foldcmds = []
        for jdoc in jdocData:
            package = jdoc[0]
            class_name = jdoc[1]
            if package != current_package :
                if len(result) != 0  :
                    foldcmds.append("%s,%sfold" % (str(package_start_index), str(len(result))))
                    package_start_index = len(result) + 1
                result.append(package)
                result.append("   "+class_name)
                current_package = package

            else :
                result.append("   "+class_name)

        foldcmds.append("%s,%sfold" % (str(package_start_index), str(len(result))))

        output(result)
        for cmd in foldcmds :
            vim.command(cmd)
