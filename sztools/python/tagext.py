import sqlite3 as sqlite
import vim,sys,os

class TagExt(object):

    @staticmethod
    def runApp():
        global tagext
        if not "tagext" in  globals() :
            tagext = TagExt()

    def __init__(self):
        self.tag_db = TagDb()
        self.all_buf_tag_info = {}

    def edit_tag(self):
        self.file_path = vim.current.buffer.name
        vim.command("call SwitchToSzToolView('tagedit')")    
        vim.command("set bufhidden=delete")
        vim.command("autocmd BufLeave <buffer>  python tagext.save_tag()")
        tag, comment = self.load_tag()
        #tag_pool = self.load_tag_pool()
        present = self.get_tag_present(tag,comment)
        output(present)

    def save_tag(self):
        codepage=sys.getdefaultencoding()
        cur_buf = vim.current.buffer
        tag = None
        comment = None
        for line in cur_buf:
            line = line.strip()
            line=line.decode(codepage)
            if line[0:4]=="tag:" :
                tag=line[4:]
            elif  line.startswith("="):
                continue
            elif line.startswith("comment:"):
                comment=line[8:]
        sql1 = "delete from tagext where file_path = ?"
        sql2 = "insert into tagext(file_path,tag,comment) values(?,?,?)"
        self.tag_db.update(sql1,(self.file_path,))
        if tag != None and tag.strip() !="":
            self.tag_db.update(sql2,(self.file_path,tag,comment))
            self.all_buf_tag_info[self.file_path] = (tag,comment)
        else :
            self.all_buf_tag_info[self.file_path] = ("","")

    def load_tag(self):
        buf_tag_info = self.all_buf_tag_info.get(self.file_path)
        if buf_tag_info == None :
            sql = "select tag,comment from tagext where file_path = ? "
            rows = self.tag_db.query(sql,(self.file_path,))
            if len(rows) > 0 :
                tag,comment = rows[0]
            else :
                tag,comment = "",""
        else :
            tag,comment = buf_tag_info

        return tag,comment

    def load_tag_pool(self):
        return "aa bb cc dd ee"

    def get_tag_present(self,tag=None, comment=None):
        return "tag:%s\ncomment:%s" % (tag,comment)

    def open_buf(self):
        row,col = vim.current.window.cursor
        line = vim.current.buffer[row-1]
        pat = re.compile("^\s+(?P<bufnr>\d+)\s+.*")
        result = pat.search(line)
        if result :
            bufnr = result.group("bufnr")
            vim.command("bw!")
            vim.command("buffer %s" % bufnr) 

    def list_buf(self):
        buffers = vim.eval("GetBufList()")
        vim.command("call SwitchToSzToolView('taglist')")    
        vim.command("set bufhidden=delete")
        vim.command("setlocal cursorline")
        vim.command("nnoremap <buffer><silent><cr>   :python tagext.open_buf()<cr>")
        buf_infos = {}
        for buf in buffers.split("\n") :
            if buf.strip() == "" :
                continue
            bufnr_attr, file_name, lineNum = buf.split('"')
            file_path = os.path.join(os.getcwd(),file_name)
            bufnr, attr =[item for item in  bufnr_attr.split(" ") if item !=""]
            buf_info = BufTagInfo(bufnr, file_path, attr, lineNum)
            buf_infos[file_path] = buf_info
        path_params = "','".join([file_path for file_path in buf_infos])
        sql = "select tag,comment,file_path from tagext where file_path in ('%s') " % path_params
        rows = self.tag_db.query(sql)
        if (len(rows) > 0) :
            all_tag = set()
            for tag,comment, file_path in rows :
                tag_arr = [item.strip() for item in tag.split(" ") if item.strip() !=""]
                buf_info = buf_infos.get(file_path) 
                if buf_info != None :
                    buf_info.tags = tag_arr
                for item in tag_arr: 
                    all_tag.add(item)
            lines = []
            for tag in all_tag :
                lines.append(tag)
                for buf_info in buf_infos.values() :
                    if buf_info.hasTag(tag):
                        lines.append("    " + str(buf_info))
                lines.append("")
            output(lines)
        else :
            output(buffers)
        vim.command("setlocal nomodifiable")

class BufTagInfo(object):

    def __init__(self, bufnr, file_path, attr = "", lineNum=""):
        self.file_path = file_path
        self.bufnr =bufnr
        self.attr = attr
        self.lineNum = lineNum
        self.tags = []

    def setTags(self,values):
        self.tags = values

    def hasTag(self, name):
        if name in self.tags :
            return True
        return False

    def __str__(self):
        return "%s %s %s %s " % (self.bufnr, self.attr, self.file_path, self.lineNum)
         
class TagDb(object):

    def __init__(self, data_file_name = None):
        if data_file_name == None :
            data_file_name = "tagext.dat"
        self.data_file_path = os.path.join(getDataHome(), data_file_name)
        if not os.path.exists(self.data_file_path) :
            initTableSql="create table tagext (id integer primary key ,  \
              file_path varchar(200),tag varchar(200),comment varchar(2000))"
            path=os.path.dirname(self.data_file_path)
            if not os.path.exists(path):
              os.makedirs(path)
            conn=self.getConn()
            cur=conn.cursor()
            cur.execute(initTableSql)
            conn.commit()
            conn.close()

    def getConn(self):
        conn=sqlite.connect(self.data_file_path)
        return conn

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

