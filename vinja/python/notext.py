#--encoding:gbk--
import sqlite3 as sqlite
import vim,sys,os

class NoteDb(object):
    def getConn(self):
        conn=sqlite.connect(os.path.join(VinjaConf.getDataHome(), "note.dat"))
        return conn

    def initDb(self):
        createNoteTableSql="create table sznote (id integer primary key ,  \
          create_date varchar(10),status char(1),title varchar(200), content varchar(5000))"
        createTagTableSql="create table tag(tag_name varchar(30), note_id integer)"
        note_db_path = os.path.join(VinjaConf.getDataHome(), "note.dat")
        path=os.path.dirname(note_db_path)
        if not os.path.exists(path):
          os.makedirs(path)
        conn=self.getConn()
        cur=conn.cursor()
        cur.execute(createNoteTableSql)
        cur.execute(createTagTableSql)
        conn.commit()
        conn.close()

    def update(self,updateSql,parameters):
        conn=self.getConn()
        cur=conn.cursor()
        cur.execute(updateSql,parameters)
        conn.commit()
        conn.close()

    def query(self,selectSql,parameters=None):
        conn=self.getConn()
        cur=conn.cursor()
        if parameters :
            cur.execute(selectSql,parameters)
        else :
            cur.execute(selectSql)
        rows = cur.fetchall()
        conn.close();
        return rows
        
class Notext(object) :

    @staticmethod
    def runApp():
        global notext
        if not "notext" in  globals() :
            notext = Notext()
        notext.updateTagListView()

    @staticmethod
    def saveBufContent():
        global notext
        if not "notext" in  globals() :
            notext = Notext()

        data=vim.current.buffer[:]
        noteItem=notext.loadData(data)
        if noteItem.id.strip() == "" :
            noteItem.create_date=notext.getCurrentDate()
            maxId=notext.addItem(noteItem)
            vim.command("let b:currentNoteId=%s" % maxId)
        else:
            notext.updateItem(noteItem)

        tagListBuf = VimUtil.getOutputBuffer("tag_list")
        if tagListBuf != None :
            notext.updateTagListView()
        noteListBuf = VimUtil.getOutputBuffer("note_list")
        if noteListBuf != None :
            notext.updateNoteListView()

        print "note has been saved"

    def saveNoteItem(self):
        data=vim.current.buffer[:]
        noteItem=self.loadData(data)
        if noteItem.id.strip() == "" :
            noteItem.create_date=self.getCurrentDate()
            maxId=self.addItem(noteItem)
            vim.command("let b:currentNoteId=%s" % maxId)
        else:
            self.updateItem(noteItem)
        self.updateTagListView()
        self.updateNoteListView()
        print "note has been saved"

    @staticmethod
    def makeTemplate():
        template = "=" * 50 + "\n" \
              + "title:" +"\n" \
              + "tags:" +"\n" \
              + "=" * 50 + "\n"
        vim.command("call SwitchToVinjaView('note_edit')")
        vim.command("call NoteItemSyntax()")
        output(template)

    def __init__(self ):
        self.notedb=NoteDb()
        self.lineInfo = {}
        self.currentTag = None

    def getCurrentDate(self):
        from datetime import datetime
        t=datetime.now()
        return t.strftime("%Y-%m-%d %H:%M")

    def updateItemStatus(self,status):
        (row, col) = vim.current.window.cursor
        line = vim.current.buffer[row-1]
        id=line[0:line.find(".")]
        updateSql="update sznote set status = ? where id=?"
        parameters=(status,id)
        self.updateTagListView()
        self.updateNoteListView()
        vim.current.window.cursor=(row,col)

    def removeNoteItem(self):
        (row, col) = vim.current.window.cursor
        id=self.lineInfo[row-1]
        updateTagSql="delete from  tag  where note_id=?"
        updateSql="delete from  sznote  where id=?"
        self.notedb.update(updateTagSql,(id,))
        self.notedb.update(updateSql,(id,))
        self.updateTagListView()
        self.updateNoteListView()
        if row > len(vim.current.buffer):
            row=len(vim.current.buffer)
        vim.current.window.cursor=(row,col)

    def loadData(self,lines):
        item=NoteItem()
        exists=vim.eval("exists('b:currentNoteId')")
        if exists == "1" :
            item.id=vim.eval("b:currentNoteId")
        else :
            item.id=""
        content=""
        codepage=sys.getdefaultencoding()
        for line in lines:
            line=line.decode(codepage)
            if line[0:5]=="tags:" :
                tags=line[5:].split()
                item.tags=tags
            elif  line.startswith("="):
                continue
            elif line.startswith("title:"):
                item.title=line[6:]
            elif line.startswith("create_date:"):
                continue
            else :
                content=content+line+"\n"
        item.content=content[:-1]
        return item

    def addItem(self,item):
        insertNoteSql="insert into sznote(id ,create_date,status,content,title) values (?,?,?,?,?)"
        insertTagSql = "insert into tag(tag_name, note_id) values (?,?)"
        maxId=self.getMaxNoteId()
        values=(maxId, item.create_date,item.status,item.content,item.title)
        self.notedb.update(insertNoteSql, values)
        for tag in item.tags:
            values=(tag,maxId)
            self.notedb.update(insertTagSql, values)
        return maxId

    def updateItem(self,item):
        updateSql="update sznote set title=?, status=?,content=? where id=?"
        insertTagSql = "insert into tag(tag_name, note_id) values (?,?)"
        deleteTagSql = "delete from tag where note_id = ?"
        values=(item.title,item.status,item.content,item.id)
        self.notedb.update(deleteTagSql,(item.id,))
        self.notedb.update(updateSql,values)
        for tag in item.tags:
            values=(tag,item.id)
            self.notedb.update(insertTagSql,values)

    def getMaxNoteId(self):
        selectSql="select max(id) from sznote"
        rows=self.notedb.query(selectSql)
        row = rows[0]
        try :
            maxId=row[0] + 1
        except :
            maxId = 1
        return maxId

    def updateTagListView(self):
        vim.command("call SwitchToVinjaView('tag_list')" )
        (row, col) = vim.current.window.cursor
        note_db_path=os.path.join(VinjaConf.getDataHome(), "note.dat")
        if not os.path.exists(note_db_path):
            initDb()
        selectSql="select  tag_name ,count(*) from tag group by tag_name"
        rows=self.notedb.query(selectSql)
        result=["All"]
        for index,row in enumerate(rows):
            tag_name=row[0]
            count=row[1]
            result.append("%s(%s)" %(tag_name,count))
        output(result)
        if row > len(vim.current.buffer):
            row=len(vim.current.buffer)
        vim.current.window.cursor=(row,col)

    def listCurrentTagItems(self):
        (row, col) = vim.current.window.cursor
        tag_name=vim.current.buffer[row-1]
        if tag_name == "All" :
            self.currentTag = "All"
        else :
            tag_name=tag_name[0:tag_name.find("(")]
            self.currentTag = tag_name
        self.updateNoteListView()

    def updateNoteListView(self):
        vim.command("call SwitchToVinjaView('note_list')" )
        (row, col) = vim.current.window.cursor
        vim.command("exec 'setlocal buftype=nofile'") 
        vim.command("exec 'setlocal noswapfile'")
        vim.command("call NoteBufferSetting()")
        vim.current.buffer[:] = None

        selectSql="select create_date,title,id from sznote a " 
        if self.currentTag != "All" :
            selectSql = "%s %s" %(selectSql , ", tag b where a.id=b.note_id and b.tag_name = ? ")
        selectSql = selectSql + " order by create_date desc "
        parameters=None
        if self.currentTag != "All"  :
            codepage=sys.getdefaultencoding()
            tagName = unicode(self.currentTag, codepage)
            parameters=(tagName,)

        rows=self.notedb.query(selectSql,parameters)
        result=[]
        for index,row in enumerate(rows):
            create_date=str(row[0]).ljust(18)
            title=(unicode(row[1])[0:15]).replace("\n"," ")
            #hack for chinese characters , they are double-width characters.
            #and coincidence is, a gbk encoded chinese char takes two bytes to store
            #so encode to gbk first can make the line display width more accurate
            title_display_len=len(title.encode("gbk"))
            title=title+(30- title_display_len)*" "+create_date
            self.lineInfo[index]=row[2]
            result.append(title)

        output(result)
        if row > len(vim.current.buffer):
            row=len(vim.current.buffer)
        vim.current.window.cursor=(row,col)

    def exit(self):
        vim.command("bw! VinjaView_note_edit")
        vim.command("bw! VinjaView_note_list")
        vim.command("bw! VinjaView_tag_list")
        notext = None

    def queryDetail(self):
        (row, col) = vim.current.window.cursor
        id=self.lineInfo[row-1]

        selectSql="select id,create_date,content,status,title from sznote where id=?"
        dataRow=self.notedb.query(selectSql,(id,))[0]
        noteItem=NoteItem()
        noteItem.id=dataRow[0]
        noteItem.create_date=unicode(dataRow[1])
        noteItem.content=unicode(dataRow[2])
        noteItem.status=unicode(dataRow[3])
        noteItem.title=unicode(dataRow[4])

        selectTagSql="select tag_name from tag where note_id=?"
        rows=self.notedb.query(selectTagSql,(id,))
        tags=[]
        for dataRow in rows:
            tags.append(dataRow[0])
        noteItem.tags=tags

        vim.command("call SwitchToVinjaView('note_edit')")
        vim.command("call NoteItemSyntax()")
        vim.command("let b:currentNoteId=%s" % id)
        output(str(noteItem))

class NoteItem(object):
    def __init__(self,id="",content="",create_date="",status="",title="",tags=[]):
        self.id=id
        self.create_date=create_date
        self.status=status
        self.content=content
        self.title=title
        self.tags=tags

    def __str__(self):
        return "=" * 50 + "\n" \
                + "title: " + self.title +"\n" \
                + "tags: " + " ".join(self.tags) +"\n" \
                + "create_date: " + self.create_date + "\n" \
                + "=" * 50 + "\n" \
                + self.content

