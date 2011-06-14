import sqlite3 as sqlite
import vim,sys,os
from time import time

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
        tag_pool = self.load_tag_pool()
        present = self.get_tag_present(tag,comment,tag_pool)
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
                tag=line[4:].strip()
            elif  line.startswith("="):
                continue
            elif line.startswith("comment:"):
                comment=line[8:].strip()
                currentLabel = "comment"
            elif line.startswith("all tags:"):
                alltags = line[9:].strip()
                currentLabel = "alltags"
            else :
                if currentLabel == "alltags" :
                    alltags = alltags + " " + line
                elif currentLabel == "comment" :
                    comment = comment + "\n" + line

        sql_arr = [ "delete from tagext where file_path = ?" ]
        params_arr = [(self.file_path,)]

        if tag != None and tag.strip() !="":
            sql_arr.append("insert into tagext(file_path,tag,comment) values(?,?,?)")
            params_arr.append((self.file_path,tag,comment))
            self.all_buf_tag_info[self.file_path] = (tag,comment)
        else :
            self.all_buf_tag_info[self.file_path] = ("","")

        if alltags == None :
            alltags == ""
        sql_arr.append("update tagpool set all_tags=?")
        params_arr.append((alltags,))
        self.tag_db.batchUpdate(sql_arr, params_arr)

    def load_tag(self):
        buf_tag_info = self.all_buf_tag_info.get(self.file_path)
        if buf_tag_info == None :
            sql = "select tag,comment from tagext where file_path = ? "
            rows = self.tag_db.query(sql,(self.file_path,))
            if rows :
                tag,comment = rows[0]
            else :
                tag,comment = "",""
        else :
            tag,comment = buf_tag_info

        return tag,comment

    def load_tag_pool(self):
        all_tags = set()
        sql = "select tag from tagext"
        rows = self.tag_db.query(sql)
        self.build_all_tags(all_tags,rows)
                
        sql = "select all_tags from tagpool"
        rows = self.tag_db.query(sql)
        self.build_all_tags(all_tags,rows)

        return all_tags

    def build_all_tags(self,all_tags, rows):
        if not rows : 
            return 
        for row in rows :
            items = [item for item in row[0].split(" ") if item !=""]
            for item in items :
                all_tags.add(item)

    def get_tag_present(self,tag, comment, all_tags):
        lines = []
        line = ""
        for item in all_tags :
            line = line + item + " "
            if len(line) > 70 :
                lines.append(line)
                line = ""
        lines.append(line)
        return "tag: %s\nall tags: %s\ncomment: %s" % (tag,"\n".join(lines),comment)

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
                    buf_info.comment = comment
                for item in tag_arr: 
                    all_tag.add(item)
            lines = []
            for tag in all_tag :
                lines.append(tag)
                for buf_info in buf_infos.values() :
                    if buf_info.hasTag(tag):
                        lines.append("    " + str(buf_info))
                lines.append("")
            lines.append("no tag bufs:")
            for buf_info in buf_infos.values() :
                if not buf_info.tags :
                    lines.append("    " + str(buf_info))
            output(lines)
        else :
            output(buffers)
        vim.command("setlocal nomodifiable")

class BufTagInfo(object):

    def __init__(self, bufnr, file_path, attr = "", lineNum=""):
        self.file_path = file_path
        self.bufnr =bufnr
        self.attr = attr
        self.lineNum = lineNum.replace(" ","")
        self.tags = []
        self.comment = ""

    def hasTag(self, name):
        if name in self.tags :
            return True
        return False

    def relpath(self, path):
        if path.startswith(os.getcwd()) :
            return os.path.relpath(path)
        else :
            return path

    def __str__(self):
        rtl_path = self.relpath(self.file_path)
        return "%s %s %s %s (%s)" % (self.bufnr, self.attr, rtl_path, self.lineNum,self.comment)
         
class TagDb(object):

    def __init__(self, data_file_name = None):
        if data_file_name == None :
            data_file_name = "tagext.dat"
        self.data_file_path = os.path.join(getDataHome(), data_file_name)
        if not os.path.exists(self.data_file_path) :
            path=os.path.dirname(self.data_file_path)
            if not os.path.exists(path):
              os.makedirs(path)
            conn=self.getConn()
            cur=conn.cursor()
            initTableSql="create table tagext (id integer primary key ,  \
              file_path varchar(200),tag varchar(200),comment varchar(2000))"
            cur.execute(initTableSql)
            initTableSql="create table tagpool (all_tags varchar(2000))"
            cur.execute(initTableSql)
            initDataSql="insert into tagpool values('')"
            cur.execute(initDataSql)
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

    def batchUpdate(self, sql_arr, parameter_arr):
        conn=self.getConn()
        cur=conn.cursor()
        sql_len = len(sql_arr)
        for i in range(0,sql_len):
            cur.execute(sql_arr[i],parameter_arr[i])
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

