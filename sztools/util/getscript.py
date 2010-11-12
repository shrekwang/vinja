import urllib2
from BeautifulSoup import BeautifulSoup
import os
import tarfile
import zipfile
import gzip
import re
import shutil

scripts = {
        "NERD tree":"1658",
        "taglist"  :"273",
        "bufexplorer" : "42",
        "snipMate" : "2540",
        "xmledit" : "301",
        "ShowMarks" :"152",
        "SuperTab continued" : "1643",
        "matchit" : "39",
        "vimwiki" : "2226",
        "pydoc" : "910", 
        "CRefVim" : "614",
        "Drawit"  : "40" ,
        "gundo"   : "3304",
        "pathogen" : "2332"
    }

def download_script(script_id,save_to):
    vimhome="http://www.vim.org/scripts/"
    data=urllib2.urlopen(vimhome+"/script.php?script_id="+script_id)
    soup = BeautifulSoup(data)
    #the first row of download link table
    a_tag=soup.first('td', {'class' : 'rowodd'}).find('a')

    download_link=a_tag["href"]
    download_filename=a_tag.text

    src_data=urllib2.urlopen(vimhome+download_link).read()
    dst_path=os.path.join(save_to, download_filename)
    dst_fileobj=open(dst_path, "wb")
    dst_fileobj.write(src_data)
    dst_fileobj.close()

def download_all(save_to):
    for script_name in scripts :
        print "downlading %s " % script_name
        download_script( scripts[script_name], save_to)

def ungzip(path):
    src_file = gzip.open(path, 'rb')
    file_content = src_file.read()
    dst_file=open(os.path.basename(path)[0:-3],"wb")
    dst_file.write(file_content)
    src_file.close()
    dst_file.close()
    os.remove(path)
    return 

def extract_file(path):

    if path.endswith(".vba.gz") :
        ungzip(path)
        path = os.path.abspath(path)[0:-3]

    if path.endswith(".vba"):
        if not os.path.isabs(path):
            path = os.path.abspath(path)
        dst_dir = os.path.splitext(path)[0]
        if not os.path.exists(dst_dir):
            os.mkdir(dst_dir)
        os.chdir(dst_dir)
        extract_vba(path)
        os.chdir("..")
        return

    if path.endswith('.zip'):
        opener, mode = zipfile.ZipFile, 'r'
    elif path.endswith('.tar.gz') or path.endswith('.tgz'):
        opener, mode = tarfile.open, 'r:gz'
    elif path.endswith('.tar.bz2') or path.endswith('.tbz'):
        opener, mode = tarfile.open, 'r:bz2'
    else: 
        return 

    try :
        file = opener(path, mode)
        namelist = file.namelist()
        if  needs_mkdir(namelist) :
            to_dir=os.path.splitext(os.path.basename(path))[0]
            if not os.path.exists(to_dir):
                os.mkdir(to_dir)
        else :
            to_dir = "."

        file.extractall(path=to_dir)
    finally: 
        file.close()
    os.remove(path)

def needs_mkdir(names):
    vim_stand_dir = ["colors","autoload","after","plugin","ftplugin","syntax"]     
    # stand dir name in start of the name
    pats = [ re.compile(r"^%s[\/\\].*" % name) for name in vim_stand_dir ]
    for name in names :
        hasMatch = any([ pat.match(name) for pat in pats]) 
        if hasMatch :
            return True
    return False

def extract_vba(name):
    file_content = open(name).readlines()
    entry_info = {}
    for row_num,line in enumerate(file_content) :
        line = line.replace("\n","")
        if line.endswith("[[[1") :
            item_name = line[0: line.find("\t")]
            item_name = item_name.replace("\\",os.path.sep)
            item_name = item_name.replace("/",os.path.sep)
            item_row_cnt = int(file_content[row_num+1].strip())
            # start row , and total row count
            entry_info[item_name] = (row_num+2, item_row_cnt)
    for item in entry_info :
        entry_dir = os.path.dirname(item)
        if entry_dir and ( not os.path.exists(entry_dir)):
            os.makedirs(entry_dir)
        entry_file = open(item,"w")
        start_row, row_cnt = entry_info[item]
        for line in file_content[start_row:start_row+row_cnt] :
            entry_file.write(line)
        entry_file.close()
    os.remove(name)


if __name__ == "__main__" :
    if not os.path.exists("bundle"):
        os.mkdir("bundle")
    os.chdir("bundle")
    download_all(".")
    for file_name in os.listdir("."):
        extract_file(file_name)
    os.mkdir("misc")
    for file_name in os.listdir("."):
        if file_name.endswith(".vim") :
            shutil.move(file_name, "misc")
    os.chdir("..")
