from BeautifulSoup import BeautifulSoup
import sqlite3 as sqlite  
import re , os , os.path
import urllib2 ,random, vim

class ClassicReader(object):

    @staticmethod
    def runApp():
        global classicReader
        classicReader=ClassicReader()
        classicReader.initBookIndex()
        classicReader.updateIndexView()

    def __init__(self):
        self.content_cache_path = os.path.join(getDataHome(), "classicBook")
        if not os.path.exists(self.content_cache_path) :
            os.makedirs(self.content_cache_path)
        self.db_path = os.path.join(getDataHome(), "classic_reader.dat")
        self.url_dict={}
        self.current_book_url=None
        self.select_areas=[]

    def getBookContent(self, url="http://www.classicreader.com/book/165/1/"):
        page = urllib2.urlopen(url)
        soup = BeautifulSoup(page)

        def extractText(texts,tag):
            for node in tag.contents:
                if isinstance(node, unicode) :
                    texts.append(node.replace("\r\n","\n"))
                else :
                    extractText(texts,node)

        contents=soup.first('td', {'class' : 'chapter-text'})
        paragraphs=contents.findAll("p")
        alltext=[]
        for para in paragraphs :
            ptext=[]
            extractText(ptext,para)
            alltext.append("".join(ptext))
            alltext.append("")

        return alltext

    def fetchBookIndex(self,url):
        page = urllib2.urlopen(url)
        soup = BeautifulSoup(page)
        contents=soup.find('tr', {'id' : 'main-panel'}).find('table',{'class':'content'})
        currentAuthor=""
        infos=[]
        for tr in  contents.findAll("tr") :
            cssClass=tr.td.get("class")
            if cssClass == "browse-header" :
                currentAuthor = tr.td.text
            elif cssClass == "browse-titles":
                for td in tr.findAll("td"):
                    for li in td.ul.findAll("li"):
                        #(genre,author,title,url)
                        bookinfo=("short stories",currentAuthor, li.a.text,li.a.get("href"))
                        infos.append(bookinfo)
        return infos
                        
    def storeBookIndex(self, infos):
        insertSql="insert into book_index(genre,author,title,url) values (?,?,?,?)"
        conn=sqlite.connect(self.db_path)
        cur=conn.cursor()
        for info in infos :
            cur.execute(insertSql,info)
        conn.commit()
        conn.close()
        
    def createDb(self):
        from sqlite3 import dbapi2 as sqlite
        ddlSql="""create table book_index(id integer primary key ,  
          genre varchar(30), author varchar(50), title varchar(200),
          url varchar(300) , sel_area varchar(2000) ) """
        parentDir=os.path.dirname(self.db_path)
        if not os.path.exists(parentDir):
          os.makedirs(parentDir)
        conn=sqlite.connect(self.db_path)
        cur=conn.cursor()
        cur.execute(ddlSql)
        conn.commit()
        conn.close()

    def initBookIndex(self):
        if (os.path.exists(self.db_path)) : return
        self.createDb()
        print "reading book index from www.classicreader.com "
        initials="abcdefghijklmnopqrstvwz"
        for item in initials :
            url="http://www.classicreader.com/browse/6/%s/" %item
            bookinfos=self.fetchBookIndex(url)
            self.storeBookIndex(bookinfos)
        print "finished reading."

    def updateIndexView(self):
        vim.command("call SwitchToSzToolView('book_index')" )
        selectSql="select distinct author from book_index "
        conn=sqlite.connect(self.db_path)
        cur=conn.cursor()
        cur.execute(selectSql)
        rows = cur.fetchall()
        result=[]
        count = 0
        for row in rows:
            count += 1
            result.append(row[0])
            selectSql="select title , url  from book_index where author = ?"
            cur=conn.cursor()
            cur.execute(selectSql,(row[0],))
            for infos in cur.fetchall():
                count += 1
                title=infos[0]
                url=infos[1]
                self.url_dict[count]=url
                result.append("  "+title)
        output(result)

    def updateContentView(self):
        (row, col) = vim.current.window.cursor
        url = self.url_dict.get(row)
        self.current_book_url = url
        match=re.search("\d+",url)
        self.content_cache_path = os.path.join(getDataHome(), "classicBook")
        book_int_index= match.group()
        book_file=os.path.join(self.content_cache_path,str(book_int_index)+".txt")
        if os.path.exists(book_file) :
            content=[line.replace("\n","") for line in open(book_file).readlines()]
        else :
            fullurl="http://www.classicreader.com" + url 
            content=self.getBookContent(fullurl)
            file_obj=open(book_file,"w")
            for line in content :
                file_obj.write(line+"\n")
            file_obj.close()
        vim.command("call SwitchToSzToolView('book_content')" )
        vim.command("exec 'setlocal buftype=nofile'") 
        vim.command("exec 'setlocal cursorline'") 
        vim.command("exec 'setlocal noswapfile'")

        vim.command("syntax clear")
        initHightLightScheme()
        self.initBookMarks(url)
        vim.command("vmap <silent><buffer><leader>za :python classicReader.markBook()<cr>")
        #vim.command("map  <silent><buffer><leader>zb :python classicReader.unMark()<cr>")
        output(content)

    def initBookMarks(self,url) :
        selectSql="select sel_area from book_index where url = ?"
        conn=sqlite.connect(self.db_path)
        cur=conn.cursor()
        cur.execute(selectSql,(url,))
        sel_area=cur.fetchone()[0]
        if sel_area :
            self.select_areas = sel_area.split(";")
            for area_str in self.select_areas :
                area=[int(item) for item in area_str.split(",")]
                visualSynCmds=getVisualSynCmd(area)
                for vimCmd in visualSynCmds:
                    vim.command(vimCmd)
        else :
            self.select_areas = []

    def markBook(self):
        area=getVisualArea()
        visualSynCmds=getVisualSynCmd(area)
        for vimCmd in visualSynCmds:
            vim.command(vimCmd)
        area_str=",".join([str(item) for item in area])
        self.select_areas.append(area_str)
        sel_area=";".join(self.select_areas)
        updateSql="update book_index set sel_area = ? where url = ?"
        conn=sqlite.connect(self.db_path)
        cur=conn.cursor()
        cur.execute(updateSql,(sel_area,self.current_book_url))
        conn.commit()
        conn.close()

    def unMark(self):
        row,col=vim.current.window.cursor
        if not self.select_areas or len(self.select_areas) == 0 :
            return 
        need_redraw=False
        for area_str in self.select_areas :
            area=[int(item) for item in area_str.split(",")]
            startCol,endCol,startLine,endLine=area
            if row >= startLine and col >= startCol and row <= endLine and col <= endCol :
                self.select_areas.remove(area_str)
                need_redraw=True
        if not need_redraw : 
            return 

        vim.command("syntax clear")
        for area_str in self.select_areas :
            area=[int(item) for item in area_str.split(",")]
            visualSynCmds=getVisualSynCmd(area)
            for vimCmd in visualSynCmds:
                vim.command(vimCmd)



class Recite(object):
    wordsList=[]

    @staticmethod
    def runApp():
        global recite
        recite_work_dir=os.path.join(getDataHome(), "recite")
        recite_words_file=os.path.join(getDataHome(),"recite/tofel.txt")
        recite=Recite(recite_work_dir,recite_words_file)
        recite.listWords(20)

    def __init__(self,workdir,wordsfile):
        self.workdir=workdir
        self.wordsfile=wordsfile
        self.datafile=os.path.join(workdir,"recite.db")
        if not os.path.exists(self.datafile):
            self.makeDictDb()

        selectSql="select eng_word,chs_trans from recite_data"
        con=sqlite.connect(self.datafile)
        cur=con.cursor()
        
        cur.execute(selectSql)  

        for row in cur.fetchall():
            self.wordsList.append(unicode(row[0])+"  "+unicode(row[1]))
        cur.close()
        con.close()


    def makeDictDb(self):
        self.initDb()
        wordsFile=open(self.wordsfile,"r")
        for index,line in enumerate(wordsFile):
            spaceIndex=line.find(" ")
            eng=line[:spaceIndex]
            chs=unicode(line[spaceIndex:].strip())
            self.addItem(eng,chs)
        wordsFile.close()

    def initDb(self):  
        createSql="create table recite_data(id integer primary key , eng_word char(50), chs_trans varchar(200), \
          test_count integer,correct_count integer,read_count integer, detail_explain varchar(1000) )"
        path=os.path.dirname(self.datafile)
        if not os.path.exists(path):  
          os.makedirs(path)  
        con=sqlite.connect(self.datafile)
        cur=con.cursor()  
        cur.execute(createSql)  
        con.commit()  
        con.close()  

    def addItem(self,engword,chstrans):    
        insertSql="insert into recite_data(eng_word,chs_trans) values (?,?)"    
        con=sqlite.connect(self.datafile)
        cur=con.cursor()    
        values=(engword,chstrans)
        cur.execute(insertSql,values)    
        con.commit()    
        con.close()    

    def listWords(self, count):
        readingList=[ random.choice(self.wordsList) for i in range(count)]
        output(readingList)

    def readDetail(self,word):
        selectSql="select detail_explain from recite_data where eng_word = ?"    
        con=sqlite.connect(self.datafile)
        cur=con.cursor()    
        values=(word,)
        cur.execute(selectSql,values)    

        detailExplain=None
        for dataRow in cur:        
            detailExplain=dataRow[0]

        con.commit()    
        con.close()    
        return detailExplain

    def updateWordItem(self,word,detailExplain):
        updateSql="update recite_data set detail_explain = ? where eng_word = ?"    
        con=sqlite.connect(self.datafile)
        cur=con.cursor()    
        values=(detailExplain , word)
        cur.execute(updateSql,values)    
        con.commit()    
        con.close()    

    def getDetailList(self,count):
        selectSql="select eng_word, detail_explain from recite_data where detail_explain is not null"
        con=sqlite.connect(self.datafile)
        cur=con.cursor()    
        cur.execute(selectSql)    

        result=[]
        for dataRow in cur:        
            engWord=dataRow[0]
            detailExplain=dataRow[1]
            for line in detailExplain.split("\n"):
                if line.find(engWord) != -1 :
                    result.append(line.replace(engWord,"_____")+" "*100+engWord)

        con.commit()    
        con.close()    
        detailList=[ random.choice(result) for i in range(count)]
        return detailList
   

    def wordDetail(self):
        (row, col) = vim.current.window.cursor  
        line = vim.current.buffer[row-1]  
        spaceIndex=line.find(" ")
        word=line[:spaceIndex]

        detailExplain=self.readDetail(word)

        if detailExplain == None :
            vim.command("Dict "+word)
            detailExplain="\n".join(vim.current.buffer)
            self.updateWordItem(word,unicode(detailExplain))
            listwinnr=str(vim.eval("winnr('#')"))
            vim.command("exec '"+listwinnr+" wincmd w'")  
        else :
            vim.command("call SwitchToSzToolView('dict')")    
            output(detailExplain)
            listwinnr=str(vim.eval("winnr('#')"))
            vim.command("exec '"+listwinnr+" wincmd w'")  

    def trainning(self):
        content=self.getDetailList(20)
        output(content)


