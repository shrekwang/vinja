#!/usr/bin/env python
import os
import re
import wx
import time 
import sys
import pprint
import ConfigParser
from threading import Thread
from pysqlite2 import dbapi2 as sqlite

import smtplib
import imaplib
import mimetypes
import email
from email.Header import decode_header
from email.MIMEMultipart import MIMEMultipart
from email.MIMEBase import MIMEBase
from email.MIMEText import MIMEText
from email.MIMEAudio import MIMEAudio
from email.MIMEImage import MIMEImage
from email.Encoders import encode_base64


class Icon(wx.TaskBarIcon):
    """notifier's taskbar icon"""

    def __init__(self, menu):

        wx.TaskBarIcon.__init__(self)

        # menu options
        self.menu = menu

        # event handlers
        self.Bind(wx.EVT_TASKBAR_LEFT_DOWN, self.click)
        self.Bind(wx.EVT_TASKBAR_RIGHT_DOWN, self.click)
        self.Bind(wx.EVT_MENU, self.select)

        iconPath=os.path.join(share_path,"vim.png")

        # icon state
        self.states = {
            "on": self.MakeIcon(iconPath),
            "off": self.MakeIcon(iconPath)
        }
        self.setStatus("off")


    def MakeIcon(self,path):
        img=wx.Image(path, wx.BITMAP_TYPE_PNG)
        if "wxMSW" in wx.PlatformInfo:
            img = img.Scale(16, 16)
        elif "wxGTK" in wx.PlatformInfo:
            img = img.Scale(22, 22)
        # wxMac can be any size upto 128x128, so leave the source img alone....
        icon = wx.IconFromBitmap(img.ConvertToBitmap() )
        return icon

    def click(self, event):
        """shows the menu"""

        menu = wx.Menu()
        for id, item in enumerate(self.menu):
            menu.Append(id, item[0])
        self.PopupMenu(menu)

    def select(self, event):
        """handles menu item selection"""

        self.menu[event.GetId()][1]()

    def setStatus(self, which):
        """sets the icon status"""

        self.SetIcon(self.states[which])

    def close(self):
        """destroys the icon"""

        self.Destroy()

class Popup(wx.Frame):
    """notifier's popup window"""

    def __init__(self):

        wx.Frame.__init__(self, None, -1, style=wx.NO_BORDER|wx.FRAME_NO_TASKBAR)
        self.padding = 12 # padding between edge, icon and text
        self.popped = 0 # the time popup was opened
        self.delay = 4 # time to leave the popup opened

        # platform specific hacks
        lines = 6 
        lineHeight = wx.MemoryDC().GetTextExtent(" ")[1]
        if wx.Platform == "__WXGTK__":
            # use the popup window widget on gtk as the
            # frame widget can't animate outside the screen
            self.popup = wx.PopupWindow(self, -1)
        elif wx.Platform == "__WXMSW__":
            # decrement line height on windows as the text calc below is off otherwise
            self.popup = self
            lineHeight -= 3
        elif wx.Platform == "__WXMAC__":
            # untested
            self.popup = self

        self.popup.SetSize((300, (lineHeight * (lines + 1)) + (self.padding * 2)))
        self.panel = wx.Panel(self.popup, -1, size=self.popup.GetSize())

        # popup's click handler
        self.panel.Bind(wx.EVT_LEFT_DOWN, self.click)

        # popup's logo
        iconPath=os.path.join(share_path,"vim.png")
        self.logo = wx.Bitmap(iconPath)
        wx.StaticBitmap(self.panel, -1, pos=(self.padding, self.padding)).SetBitmap(self.logo)

        # main timer routine
        self.timer = wx.Timer(self, -1)
        self.Bind(wx.EVT_TIMER, self.main, self.timer)
        self.timer.Start(1000 * 60)

    def main(self, event):

        if self.focused():
            # maintain opened state if focused
            self.popped = time.time()
        elif self.opened() and self.popped + self.delay < time.time():
            # hide the popup once delay is reached
            self.hide()

    def click(self, event):
        """handles popup click"""

        self.popped = 0
        self.hide()

    def show(self, text):
        """shows the popup"""

        # create new text
        if hasattr(self, "text"):
            self.text.Destroy()
        popupSize = self.popup.GetSize()
        logoSize = self.logo.GetSize()
        print "logosize is %s" % str(logoSize)
        self.text = wx.StaticText(self.panel, -1, text)
        self.text.Bind(wx.EVT_LEFT_DOWN, self.click)
        self.text.Move((logoSize.width + (self.padding * 2), self.padding))
        self.text.SetSize((
            popupSize.width - logoSize.width - (self.padding * 3),
            popupSize.height - (self.padding * 2)
        ))

        # animate the popup
        screen = wx.GetClientDisplayRect()
        self.popup.Show()
        for i in range(1, popupSize.height + 1):
            self.popup.Move((screen.width - popupSize.width, screen.height - i))
            self.popup.SetTransparent(int(float(240) / popupSize.height * i))
            self.popup.Update()
            self.popup.Refresh()
            time.sleep(0.01)
        self.popped = time.time()

    def hide(self):
        """hides the popup"""

        self.popup.Hide()
        self.popped = 0

    def focused(self):
        """returns true if popup has mouse focus"""

        mouse = wx.GetMousePosition()
        popup = self.popup.GetScreenRect()
        return (
            self.popped and
            mouse.x in range(popup.x, popup.x + popup.width)
            and mouse.y in range(popup.y, popup.y + popup.height)
        )

    def opened(self):
        """returns true if popup is open"""

        return self.popped != 0

class Notifier(wx.App):
    """main notifier app"""

    def __init__(self,log_path):
        #wx.App.__init__(self, redirect=1, filename=log_path)
        wx.App.__init__(self, redirect=0 )

        # menu handlers
        menu = [
            ("Show again", self.again),
            ("Settings", self.settings),
            ("Exit", self.exit),
        ]

        self.mailApp=SzMail()
        self.mailApp.login()
        self.mailApp.syncToDb()

        # main objects
        self.icon = Icon(menu)
        self.popup = Popup()
        #self.reader = Reader(feeds=["http://digg.com/rss/index.xml"])

        # main timer routine
        timer = wx.Timer(self, -1)
        self.Bind(wx.EVT_TIMER, self.main, timer)
        timer.Start(5000)
        self.MainLoop()

    def main(self, event):
        #self.mailApp.syncToDb()
        pass

    def again(self):
        self.popup.show("again")
        print "again"

    def settings(self):
        print "settings"

    def exit(self):

        # close objects and end
        #self.reader.close()
        self.icon.close()
        self.Exit()

class BaseSqliteDb(object):
    def __init__(self, path):
        self.db_path = path

    def update(self,updateSql,parameters):
        conn=sqlite.connect(self.db_path)
        cur=conn.cursor()
        cur.execute(updateSql,parameters)
        conn.commit()
        conn.close()

    def query(self,selectSql,parameters=None):
        conn=sqlite.connect(self.db_path)
        cur=conn.cursor()
        if parameters :
            cur.execute(selectSql,parameters)
        else :
            cur.execute(selectSql)
        rows = cur.fetchall()
        conn.close();
        return rows

class MailDb(BaseSqliteDb):
    mail_folder_ddl = """ create table szmail_folder (
                  uid integer, name varchar(50),
                  msgs inteter, recent integer, unseen integer
                  )
              """
    mail_ddl = """ create table szmail_content (
                 id integer primary key,
                 subject varchar(300),
                 sender  varchar(300),
                 receiver varchar(300),
                 send_date varchar(300),
                 content varchar(50000)
                 ) 
             """


    def __init__(self):
        BaseSqliteDb.__init__(self,"mail.dat")
        self.initDb()

    def initDb(self):
        parent = os.path.dirname(self.db_path)
        if parent !="" and not os.path.exists(parent):
          os.makedirs(parent)

        conn=sqlite.connect(self.db_path)
        cur=conn.cursor()
        cur.execute(self.mail_folder_ddl)
        cur.execute(self.mail_ddl)
        conn.commit()
        conn.close()

    def storeFolder(self,uid, name,msgs,recent,unseen):
        countFolderSql="select count(*) from szmail_folder where uid=?"
        rows=self.query(countFolderSql,(uid,))
        count = rows[0][0]
        if count == 1 :
            updateSql = "update szmail_folder set name=?,msgs=?,recent=?,unseen=? where uid=?"
            self.update(updateSql,(name,msgs,recent,unseen,uid))
        else :
            updateSql = "insert into szmail_folder(uid,name,msgs,recent,unseen) values ( ?,?,?,?,?)"
            self.update(updateSql,(uid,name,msgs,recent,unseen))


class SzMail(object):

    def __init__(self):
        self.server_port=993
        self.list_response_pattern = re.compile(r'\((?P<flags>.*?)\) "(?P<delimiter>.*)" (?P<name>.*)')
        self.list_status_pattern = re.compile(
                r'(?P<MESSAGES>.*?) "(?P<UIDVALIDITY>.*)" (?P<RECENT>.*) ')
        config = ConfigParser.ConfigParser()
        config.read(os.path.join(share_path,"conf/mail.cfg"))
        self.hostname = config.get('server', 'hostname')
        self.username = config.get('account', 'username')
        self.password = config.get('account', 'password')

        self.mailDb = MailDb()
    
    def login(self):
        self.conn = imaplib.IMAP4_SSL(self.hostname,self.server_port)
        self.conn.login(self.username, self.password)

    def logout(self):
        self.conn.logout()


    def _parse_list_response(self,line):
        flags, delimiter, mailbox_name = self.list_response_pattern.match(line).groups()
        mailbox_name = mailbox_name.strip('"')
        return (flags, delimiter, mailbox_name)


    def list_folder_status(self):
        typ, data = self.conn.list()
        result = []
        for line in data:
            flags, delimiter, mailbox_name = self._parse_list_response(line)
            status = self.conn.status(mailbox_name, '(MESSAGES UIDVALIDITY RECENT UNSEEN)')
            # status content : between ()
            content = status[1][0]
            print content
            status_data = content[content.find("(") + 1 : -1].split()
            for index, value in enumerate(status_data) :
                if value == "MESSAGES" :
                    msgs = status_data[index+1]
                elif value == "UIDVALIDITY" :
                    uid = status_data[index+1]
                elif value == "RECENT" :
                    recent = status_data[index+1]
                elif value == "UNSEEN" :
                    unseen = status_data[index+1]
            result.append((mailbox_name,msgs,uid,recent,unseen))
        return result

    def syncToDb(self):
        result = self.list_folder_status()
        for data in result :
            name,msgs,uid,recent,unseen = data
            print data
            self.mailDb.storeFolder(uid,name,msgs,recent,unseen)

    def test(self):
        self.conn.select('INBOX', readonly=True)

        typ, msg_data = self.conn.fetch("52", '(RFC822)')
        for response_part in msg_data:
            if isinstance(response_part, tuple):
                msg = email.message_from_string(response_part[1])

                for item in msg.keys():
                    vals=[]
                    for val,charset in decode_header(msg[item]) :
                        if charset :
                            if charset == "gb2312" :
                                charset = "gbk"
                            val = unicode(val,charset,"replace")
                        vals.append(val)
                    print item ,":     ", ",".join(vals)
                    print '+'*60 

                print "parse result"
                print '+'*60 
                self.parse_mail(msg)

                
                #for header in [ 'subject', 'to', 'from' ]:
                #    d_text=decode_header(msg[header])[0][0]
                #    print '%-8s: %s' % (header.upper(), d_text)

    def search_mail(self, criteria):
            self.conn.select('INBOX', readonly=True)
            typ, msg_ids = self.conn.search(None, criteria)
            print typ,msg_ids
            if typ !='OK' : return
            for id in msg_ids[0].split() :
                if id == "" : continue
                try :
                    typ, msg_data = self.conn.fetch(id, '(RFC822)')
                    for response_part in msg_data:
                        if isinstance(response_part, tuple):
                            msg = email.message_from_string(response_part[1])
                            for header in [ 'subject', 'to', 'from' ]:
                                d_text=decode_header(msg[header])[0][0]
                                print '%-8s: %s' % (header.upper(), d_text)
                            self.parse_mail(msg)
                except Exception , e:
                    print e

    def parse_mail(self,msg):
        for par in msg.walk():
            if par.is_multipart(): continue 
            print '+'*60 
            name = par.get_param("name") 
            if name:
                h = email.Header.Header(name)
                dh = email.Header.decode_header(h)
                fname = dh[0][0]
                print 'attachment:', fname
                data = par.get_payload(decode=True) 
                try:
                    f = open(fname, 'wb') 
                except:
                    print 'invalid char'
                    f = open('aaaa', 'wb')
                f.write(data)
                f.close()
            else:
                print par.get_content_type()
                email_text= par.get_payload(decode=True)
                charset = par.get_content_charset()
                if charset == "gb2312":
                    charset = "gbk"
                if charset :
                    print email_text.decode(charset)
                else :
                    print email_text
        
    def sendMail(subject, text, *attachmentFilePaths):
        gmailUser = 'sadf@gmail.com'
        gmailPassword = 'asdf'
        recipient = 'test@sdf.net'

        msg = MIMEMultipart()
        msg['From'] = gmailUser
        msg['To'] = recipient
        msg['Subject'] = subject
        msg.attach(MIMEText(text))

        for attachmentFilePath in attachmentFilePaths:
            msg.attach(getAttachment(attachmentFilePath))

        mailServer = smtplib.SMTP('smtp.gmail.com', 587)
        mailServer.ehlo()
        mailServer.starttls()
        mailServer.ehlo()
        mailServer.login(gmailUser, gmailPassword)
        mailServer.sendmail(gmailUser, recipient, msg.as_string())
        mailServer.close()

        print('Sent email to %s' % recipient)

    def getAttachment(attachmentFilePath):
        contentType, encoding = mimetypes.guess_type(attachmentFilePath)

        if contentType is None or encoding is not None:
            contentType = 'application/octet-stream'

        mainType, subType = contentType.split('/', 1)
        file = open(attachmentFilePath, 'rb')

        if mainType == 'text':
            attachment = MIMEText(file.read())
        elif mainType == 'message':
            attachment = email.message_from_file(file)
        elif mainType == 'image':
            attachment = MIMEImage(file.read(),_subType=subType)
        elif mainType == 'audio':
            attachment = MIMEAudio(file.read(),_subType=subType)
        else:
            attachment = MIMEBase(mainType, subType)
        attachment.set_payload(file.read())
        encode_base64(attachment)

        file.close()
        attachment.add_header('Content-Disposition', 'attachment',   filename=os.path.basename(attachmentFilePath))
        return attachment

class MailViewer(object):
    def __init__(self):
        pass

    @staticmethod
    def runApp():
        global mailext
        if not "mailext" in  globals() :
            mailext = Notext()
        mailext.showMailFolder()

    def showMailFolder(self):
        vim.command("call SwitchToSzToolView('mailext_dir')" )
        (row, col) = vim.current.window.cursor
        selectSql="select tag_name ,count(*) from tag group by tag_name"
        rows=self.notedb.query(selectSql)
        result=[]
        for index,row in enumerate(rows):
            tag_name=row[0]
            count=row[1]
            result.append("%s(%s)" %(tag_name,count))
        output(result)
        if row > len(vim.current.buffer):
            row=len(vim.current.buffer)
        vim.current.window.cursor=(row,col)

    def showMailList(self):
        (row, col) = vim.current.window.cursor
        tag_name=vim.current.buffer[row-1]
        tag_name=tag_name[0:tag_name.find("(")]
        self.currentTag = tag_name
        self.updateNoteListView()

    def getCurrentDate(self):
        from datetime import datetime
        t=datetime.now()
        return t.strftime("%Y-%m-%d %H:%M")

if __name__ == '__main__':
    
    from optparse import OptionParser 
    import logging

    parser = OptionParser()
    parser.add_option("-p","--sztool-home",action="store", dest="home" )
    (options, args) = parser.parse_args()
    if not options.home :
        print "missing -p argument"
        sys.exit(0)

    globals()["sztool-home"] = options.home
    globals()["share_path"] = os.path.join(options.home,"share")
    globals()["data_path"] = os.path.join(options.home,"data")
    globals()["app_path"] = os.path.join(options.home,"python")

    mail_app_log = os.path.join(data_path,"mail_app.log")
    mail_gui_log = os.path.join(data_path,"mail_gui.log")

    logging.basicConfig(filename=mail_app_log ,level=logging.DEBUG)
    notifier = Notifier(mail_gui_log)
