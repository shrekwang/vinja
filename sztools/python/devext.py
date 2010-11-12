from pyparsing import *
import re
import vim
from string import Template

elements={}

def parseElementCp(cp,aa):

    if isinstance(cp ,str) :
        aa.append(cp)
    else :
        for item in cp :
            parseElementCp(item[0],aa)

def getParsingRule() :
    # reference : http://www.w3.org/TR/xml11 

    #-------- element decl ---------------
    Name=Word(alphanums+"-")
    subgroup = Forward()
    cp = Group( ( Name.setResultsName("itemName") |  subgroup )  + Optional ( oneOf('? * +') ))
    subgroup <<  Suppress('(') + cp + ZeroOrMore(oneOf('| ,').suppress() + cp) +Suppress(')')

    children = subgroup + Optional ( oneOf('? * +') )

    mixed = '(' + Keyword('#PCDATA')   + Optional( Group(OneOrMore ( '|' +Name ))) +')*' \
            | '(' + Keyword('#PCDATA') + ')'

    contentspec = Group(children.setResultsName("children")  
            | mixed.setResultsName("mixed")  
            | Keyword('EMPTY').setResultsName("empty")  
            | Keyword('ANY').setResultsName("any")  )

    elementdecl = Group('<!ELEMENT' 
            + Name.setResultsName("eleName") 
            + contentspec.setResultsName("content") 
            + '>')

    
    #------------ attlist decl  ------------------
    stringType = Keyword('CDATA') 
    attValue = '"'+Word(alphanums)+'"' | "'" + Word(alphanums) +"'"
    tokenizedType = Keyword('ID') | Keyword('IDREF') | Keyword('IDREFS')  \
        | Keyword('ENTITY') | Keyword('ENTITIES')   \
        | Keyword('NMTOKEN') | Keyword('NMTOKENS') 

    defaultDecl = Keyword('#REQUIRED')  | Keyword('#IMPLIED')  | Group(Optional('#FIXED')+attValue) 
    enumeration = '(' + Name + Optional(Group(OneOrMore('|'+Name))) +')'
    attType = stringType | tokenizedType | enumeration 
    attDef = Group(Name.setResultsName("attName") + attType + defaultDecl )

    attlistDecl = Group('<!ATTLIST' 
            +  Name.setResultsName("eleName") 
            + OneOrMore(attDef).setResultsName("attDef") 
            + '>' )


    #----------- decl set  -----------------------
    markupdecl = elementdecl | attlistDecl 
    comment =  Regex("<!--.*?-->" , re.DOTALL )

    markupdecl.ignore(comment)
    markupdecl.ignore("<?xml" + restOfLine )
    declSet= OneOrMore(markupdecl)

    return declSet

class DtdElement(object):

    def __init__(self) :
        self.attList=[]
        self.contents=[]
        self.name=""
    def __str__(self):
        return self.name + str(self.contents) + str(self.attList)


def parseElement(elementItem) :
    dtdElement=DtdElement()
    

    content=elementItem.content.asDict()
    dtdElement.name=elementItem.eleName

    if content.has_key("children") :
        children= elementItem.content.children
        aa=[]
        for cp in children :
            parseElementCp(cp[0],aa)
        dtdElement.contents=aa

    elif content.has_key("mixed") :
        pass
    elif content.has_key("empty") :
        dtdElement.contents=["empty"]
    elif content.has_key("any") :
        dtdElement.contents=["any"]
    return dtdElement

def parseAttlist(parseResult) :
    global elements

    eleName=parseResult.eleName
    ele=elements.get(eleName)
    if not ele :
        return

    attList=[]
    for item in parseResult.attDef :
        attList.append( item.attName )

    ele.attList = attList
        
def parseDtdString(dtdString):
    global elements 
    declSet=getParsingRule()
    parseResult=declSet.parseString(dtdString)

    for index, item in enumerate(parseResult):
        if item[0] == '<!ELEMENT' :
            ele=parseElement(item)
            elements[ele.name] = ele
        if item[0] == '<!ATTLIST' :
            parseAttlist(item)

def parseDtdDecl():
    bcontent = " ".join(vim.current.buffer)
    group=re.findall("http.*\.dtd",bcontent)
    if len(group) < 1 :
        print "can't not find dtd declaration "
        return

    requestUrl=group[0]
    req=urllib2.Request(requestUrl)
    data=urllib2.urlopen(req).read()

    parseDtdString(data)


def SzDtdCompletion(findstart,base):
    if str(findstart)=="1":
        (row,col)=vim.current.window.cursor
        line=vim.current.buffer[row-1]

        index=0
        for i in range(col-1,-1, -1):
            char=line[i]
            if char == " " or char=="<" :
                index=i+1
                break

        cmd="let g:SzCompletionIndex=%s" %str(index)
    else:
        (row,col)=vim.current.window.cursor
        eleName,isSubEle = getContentInfo()
        result=getCompleList(base,eleName,isSubEle)
        cmd="let g:SzCompletionResult=%s" %str(result)
    vim.command(cmd)

def getCompleList(base,eleName,isSubEle=False):
    eleDef = elements.get(eleName)
    if not eleDef : return []
    compleList=[]
    if isSubEle :
        compleList=[item for item in eleDef.attList if item.find(base) > -1 ]
    else :
        compleList=[item for item in eleDef.attList if item.find(base) > -1 ]

    return compleList

def getContentStr():
    (row,col)=vim.current.window.cursor
    buffer=vim.current.buffer
    content=[]

    for i in range(row,0,-1):
        line=buffer[i-1]
        count=len(line)
        for j in range(count):
            cc.insert(0,line[count-j-1])
        cc.insert(0," ")

    for i in range(row, 0, -1) :
        line=buffer[i-1]
        content.insert(0,line)
        if line.find("<")  > -1 :
            break
    tmpStr = " ".join(content)
    print tmpStr
    return tmpStr

def getContentInfo():
    global tmpStr
    tmpStr=getContentStr()
    eleName = re.findall("<([^ ]+?)>? ",tmpStr)[0]
    isSubEle = False
    if tmpStr.find(">") > -1 :
        isEle = True
    return eleName , isSubEle


options={}

typeMap={ 
    "CHAR":"String",
    "VARCHAR":"String", 
    "VARCHAR2":"String", 
    "NUMERIC":"BigDecimal", 
    "DECIMAL":"BigDecimal",
    "NUMBER":"int", 
    "INTEGER":"int",
    "BIGINT":"long",
    "REAL": "Float",
    "FLOAT": "Double",
    "DOUBLE": "Double",
    "DATE":"Date",
    "TIME":"Date",
    "TIMESTAMP": "Date"
}

daoTemplate="""
    public interface ${entityName}Dao {
        public void add${entityName}(${entityName} ${entityVariable});
        public void update${entityName}(${entityName} ${entityVariable});
        public ${entityName} get${entityName}(String id);
        public void delete${entityName}(String id);
        public List<${entityName}> getAll${entityName}();
    }
    """

def generateCode():
    global options

    vb=vim.eval("GetVisualBlock()")
    lines=vb.split("\n")
    for line in lines:
        try :
            name,value=tuple(line.split("="))
            options[name.strip()]=value.strip()
        except :
            pass
    vim.command("call SwitchToSzToolView('codegen')")

    tableCols=getTableDefinition(options["table"])

    output(getSql(options["table"],tableCols))

    for item in getJava(options["entity"],tableCols) :
        output(item,append=True)
        output("",append=True)

def getSql(tableName,tableCols):

    result=[name for name,type in tableCols]

    insertSql="insert into %s( %s ) values ( %s )" %(tableName, ",".join(result) , ",".join("?"*len(result)) )
    updateSql="update %s set %s where id = ?" %(tableName, "=?,".join(result) )
    return [insertSql,updateSql]

def getJava(entityName,tableCols):
    fieldDeclare = []
    setters=[]
    for colname,sqltype in tableCols:
        name=''.join([word.capitalize() for word in colname.lower().split("_")])
        if sqltype.find("(") > -1 : sqltype=sqltype[0:sqltype.find("(")]
        javatype=typeMap.get(sqltype.strip(),"String")
        field="private %s %s; " %(javatype,name[0].lower()+name[1:])
        fieldDeclare.append(field)


        setter='%s.set%s(rs.get%s("%s"));' %(entityName,name,javatype.capitalize(),colname)
        setters.append(setter)

        d={"entityName":entityName,"entityVariable":entityName[0].lower()+entityName[1:]}
        daoStr=Template(daoTemplate).substitute(d)

    return [fieldDeclare , setters , daoStr]

def getTableDefinition(tableName):

    db_profile=getDbOption()
    if db_profile == None :  return

    server_type=db_profile["servertype"]
    if server_type=="oracle":
        sql="select column_name,data_type from user_tab_columns where table_name = '"+tableName.upper()+"'"
    elif server_type=="mssql":
        sql="""Select col.name as 'column_name', type.name as 'data_type'  
                From syscolumns as col     
                Left Join systypes as type on col.xtype = type.xtype  
                where col.id = (Select id From sysobjects Where name = '"""+tableName+"')"

    conn=createConn(db_profile)
    cur = conn.cursor()
    try :
        cur.execute(sql)
    except Exception , reason:
        print "database error, check if there are any errors in sql"
        print reason
        cur.close()
        conn.close()
        return

    result=[]
    for dataRow in cur.fetchall():
        result.append(dataRow)

    conn.commit()
    cur.close()
    conn.close()

    return result
