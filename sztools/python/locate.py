from  shext import LocateCmd
from jde import Parser
import fnmatch
import os

class Prompt(object) :
    def __init__(self, init_prompt_value):
        self.key_strokes = []
        if init_prompt_value :
            self.key_strokes.append(init_prompt_value)

    def get_name(self):
        return "".join(self.key_strokes)

    def append(self,char):
        self.key_strokes.append(char)

    def delete_last(self):
        self.key_strokes = self.key_strokes[0:-1]

    def show(self):
        vim.command("echo '%s%s'" % (">> ", self.get_name() ))

class QuickLocater(object) :
    def __init__(self, init_prompt_value,content_manager) :
        self.content_manager = content_manager
        self.prompt = Prompt(init_prompt_value)
        self.create_tab = False

    def show_matched_result(self):
        pat = self.prompt.get_name() + "*"
        result = []
        #if len(pat) > 2 :
        result = self.content_manager.search_content(pat)
        output(result)
        win_height = len(result)
        vim.command("resize %s" % str(win_height) )

    def save_env(self):
        self.timeoutlen = vim.eval("&timeoutlen")
        self.insertmode = vim.eval("&insertmode")
        self.showcmd = vim.eval("&showcmd")
        self.report = vim.eval("&report")
        self.sidescroll = vim.eval("&sidescroll")
        self.sidescrolloff = vim.eval("&sidescrolloff")
        self.guicursor = vim.eval("&guicursor")
        self.cursor_bg = vim.eval("""synIDattr(synIDtrans(hlID("Cursor")), "bg")""")
        self.last_bufnr = vim.eval('bufnr("$")')
        if self.cursor_bg == None :
            self.cursor_bg = "white"

    def restore_env(self):
        vim.command("set timeoutlen=%s" % self.timeoutlen)

        if self.insertmode == "0" :
            vim.command("set noinsertmode")
        else :
            vim.command("set insertmode")

        if self.showcmd == "0":
            vim.command("set noshowcmd")
        else :
            vim.command("set showcmd")

        vim.command("set report=%s" % self.report )
        vim.command("set sidescroll=%s" % self.sidescroll)
        vim.command("set sidescrolloff=%s" % self.sidescrolloff) 

        vim.command("set guicursor=%s" % self.guicursor)
        vim.command("highlight Cursor guifg=black guibg=%s" % (self.cursor_bg))

    @staticmethod
    def runApp(content_manager , create_tab):
        global quickLocater
        name = vim.eval("expand('<cword>')")
        if name == None :
            name = ""
        quickLocater = QuickLocater(name,content_manager)
        if create_tab != None and create_tab == "true":
            quickLocater.create_tab = True
        quickLocater.create_explorer_buffer()
        if len(name) > 0 :
            quickLocater.prompt.show()
            quickLocater.show_matched_result()

    def create_explorer_buffer(self) :
        self.save_env()
        vim.command("silent! botright 1split explorer_buffer")
        vim.command("setlocal bufhidden=delete")
        vim.command("setlocal buftype=nofile")
        vim.command("setlocal noswapfile")
        vim.command("setlocal nowrap")
        vim.command("setlocal nonumber")
        vim.command("setlocal foldcolumn=0")
        vim.command("setlocal nocursorline")
        vim.command("setlocal nospell")
        vim.command("setlocal nobuflisted")
        vim.command("setlocal textwidth=0")
        vim.command("setlocal noreadonly")
        vim.command("setlocal cursorline")

        vim.command("set timeoutlen=0")
        vim.command("set noinsertmode")
        vim.command("set noshowcmd")
        vim.command("set nolist")
        vim.command("set report=9999")
        vim.command("set sidescroll=0")
        vim.command("set sidescrolloff=0")

        vim.command("set guicursor+=a:blinkon0")
        bg = vim.eval("""synIDattr(synIDtrans(hlID("Normal")), "bg")""")
        if bg :
            vim.command("highlight Cursor guifg=black guibg=%s" % (bg))
        

        printables = """/!"#$%&'()*+,-.0123456789:<=>?#@"ABCDEFGHIJKLMNOPQRSTUVWXYZ[]^_abcdefghijklmnopqrstuvwxyz{}~"""
        mapcmd = "noremap <silent> <buffer>"

        for byte in printables :
            vim.command("%s %s :python quickLocater.on_key_pressed('%s')<CR>" % (mapcmd, byte , byte))

        vim.command("%s  <Tab>    :python quickLocater.on_key_pressed('%s')<cr>" %(mapcmd, "Tab"))
        vim.command("%s  <BS>     :python quickLocater.on_key_pressed('%s')<cr>" %(mapcmd, "BS"))
        vim.command("%s  <Del>    :python quickLocater.on_key_pressed('%s')<cr>" %(mapcmd, "Del"))
        vim.command("%s  <CR>     :python quickLocater.on_key_pressed('%s')<cr>" %(mapcmd, "CR"))
        vim.command("%s  <Esc>    :python quickLocater.on_key_pressed('%s')<cr>" %(mapcmd, "ESC"))
        vim.command("%s  <C-j>    :python quickLocater.on_cursor_move('down')<cr>" %(mapcmd ))
        vim.command("%s  <C-k>    :python quickLocater.on_cursor_move('up')<cr>" %(mapcmd ))
        vim.command("%s  <C-v>    :python quickLocater.on_paste_content()<cr>" %(mapcmd ))

    def on_paste_content(self):
        content = vim.eval("getreg('+')")
        content = content.replace("\n","").strip()
        self.prompt.append(content)
        self.prompt.show()
        self.show_matched_result()

    def on_cursor_move(self, direction) :
        work_buffer = vim.current.buffer
        win = vim.current.window
        row,col = win.cursor
        if direction == "up" :
            if row > 1 : win.cursor = ( row-1 , col)
        else :
            if row < len(work_buffer) : win.cursor = ( row+1 , col)

    def on_key_pressed(self, key):
        if key == "Tab" :
            pass
        elif key == "BS" or key == "Del":
            self.prompt.delete_last()
            self.prompt.show()
            self.show_matched_result()
        elif key == "CR" :
            work_buffer=vim.current.buffer
            row,col = vim.current.window.cursor
            line = work_buffer[row-1]
            self.clean()
            vim.command("exec '%s wincmd w'" % self.last_bufnr)
            self.content_manager.open_content(line)

        elif key == "ESC" :
            self.clean()
        else :
            self.prompt.append(key)
            self.prompt.show()
            self.show_matched_result()

    def clean(self):
        vim.command("bwipeout")
        vim.command("echo ''")
        self.restore_env()

class FileContentManager(object):
    def __init__(self):
        shext_locatedb_path = os.path.join(getDataHome(), "locate.db")
        self.locatecmd = LocateCmd(shext_locatedb_path)

    def search_content(self,search_pat):

        result = self.locatecmd.locateFile(search_pat,startWithAlias=True)
        result = result[0:30]
        rows = []
        self.start_dirs = {}
        for apath,alias,start_dir in result :
            rows.append(apath)
            self.start_dirs[alias] = start_dir
        return rows

    def open_content(self,line):
        fname = line.strip()
        if not fname : return 

        alias = fname[0: fname.find(os.path.sep)]
        rtl_path = fname[fname.find(os.path.sep)+1:]
        fname = os.path.join(self.start_dirs[alias], rtl_path)
        vim.command("edit %s "  %(fname))

class JavaMemberContentManager(object):

    def __init__(self):
        work_buffer=vim.current.buffer
        self.memberInfo = Parser.parseAllMemberInfo(work_buffer)

    def search_content(self,search_pat):
        result = []
        if not search_pat :
            search_pat = "*"
        for name ,mtype,rtntype,lineNum in self.memberInfo :
            if fnmatch.fnmatch(name, search_pat) :
                result.append("\t".join((name,str(lineNum))))
        return result

    def open_content(self,line):
        print line
        name,lineNum = line.split("\t")
        vim.command("normal %sG" % str(lineNum))
        


