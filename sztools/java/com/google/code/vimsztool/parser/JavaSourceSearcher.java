package com.google.code.vimsztool.parser;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.antlr.runtime.tree.CommonTree;

import com.google.code.vimsztool.compiler.CompilerContext;

public class JavaSourceSearcher {
    
    public static final String NULL_TYPE = "NULL";

    private boolean hasFound = false;
    private List<LocalVariableInfo> visibleVars = null;
    private ParseResult parseResult = null;
    private CompilerContext ctx  = null;
    private String currentFileName;
    
    private List<String> importedNames = new ArrayList<String>();
    private List<MemberInfo> memberInfos = new ArrayList<MemberInfo>();
    
    public List<MemberInfo> getMemberInfos() {
    	return this.memberInfos;
    }
    public List<String> getImportedNames() {
    	return this.importedNames;
    }

    public JavaSourceSearcher(String filename, CompilerContext ctx) {
    	this.ctx = ctx;
    	this.currentFileName = filename;
    	
        parseResult = AstTreeFactory.getJavaSourceAst(filename);
        CommonTree tree = parseResult.getTree();
        readClassInfo(tree);
        
    }
    
    
   

    public LocationInfo searchDefLocation(int line, int col) {
        CommonTree tree = parseResult.getTree();
        CommonTree node = searchMatchedNode(tree,line,col);
        if (node == null ) {
            System.out.println("can't find node here");
            return null;
        }
        visibleVars = parseAllVisibleVar(node);
        LocationInfo info = searchNodeDefLocation(node);
        return info;
    }

    private String getClassFilePath(String className) {
    	String path = ctx.findSourceClass(className);
    	
    	//try same package class
    	if (path.equals("None")) {
	    	String classFullName  = ctx.buildClassName(this.currentFileName);
	    	if (classFullName.lastIndexOf(".") > -1 ) {
	    		String packageName = classFullName.substring(0,classFullName.lastIndexOf("."));
	    		path = ctx.findSourceClass(packageName+"."+className);
	    	}
    	}
    	
    	//try class under the java.lang package
    	if (path.equals("None")) {
    		className = "java.lang." + className;
    		path = ctx.findSourceClass(className);
    	}
        return path;
    }


    public LocationInfo searchNodeDefLocation(CommonTree node) {
        LocationInfo info = new LocationInfo();

        if (node.getType() == JavaParser.IDENT) {
            CommonTree parent = (CommonTree)node.getParent();
            if (parent.getType() == JavaParser.METHOD_CALL) {
                String methodName = parent.getChild(0).getText();
                List<String> typenameList = parseArgumentTypenameList((CommonTree)parent.getChild(1));
                StringBuilder sb = new StringBuilder(methodName);
                MemberInfo memberInfo = findMatchedMethod(methodName, typenameList,this.memberInfos); 

                info.setLine(memberInfo.getLineNum());
                info.setCol(memberInfo.getColumn());
                info.setFilePath(currentFileName);
                
            } else if (parent.getType() == JavaParser.DOT) {
                CommonTree leftNode = (CommonTree)parent.getChild(0);
                String leftNodeTypeName = parseNodeTypeName(leftNode);
                String leftNodeFilePath = getClassFilePath(leftNodeTypeName);
                info.setFilePath(leftNodeFilePath);

                JavaSourceSearcher searcher = new JavaSourceSearcher(leftNodeFilePath, ctx);
                List<MemberInfo> leftClassMembers = searcher.getMemberInfos();
                
                CommonTree rightNode = (CommonTree)parent.getChild(1);
                CommonTree pparent = (CommonTree)parent.getParent();
                if (pparent.getType() == JavaParser.METHOD_CALL) {
                    List<String> typenameList = parseArgumentTypenameList((CommonTree)pparent.getChild(1));
                    MemberInfo memberInfo = findMatchedMethod(rightNode.getText(), typenameList,leftClassMembers);
                    info.setLine(memberInfo.getLineNum());
                    info.setCol(memberInfo.getColumn());
                } else {
                    MemberInfo memberInfo = findMatchedField(rightNode.getText(), leftClassMembers);
                    info.setLine(memberInfo.getLineNum());
                    info.setCol(memberInfo.getColumn());
                }

            } else {
            	//todo 查找变量的顺序
            	//1: 本地变量  (done)
            	//2: 父类变量
            	//3: 导入的类名
            	boolean found = false;
                for (LocalVariableInfo var : visibleVars) {
                    if (var.getName().equals(node.getText())) {
                        info.setCol(var.getCol());
                        info.setLine(var.getLine());
		                info.setFilePath(currentFileName);
		                found = true;
                        break;
                    }
                }
                if (!found ) {
                	for (String importedName: this.importedNames) {
                		String lastName = importedName.substring(importedName.lastIndexOf(".")+1);
                		if (node.getText().equals(lastName)) {
                			info.setFilePath(this.getClassFilePath(importedName));
                			info.setLine(1);
                			info.setCol(1);
                		}
                	}
                }
            }
        }
        return info;
    }

    public void readClassInfo(CommonTree t) {
        if ( t != null ) {
            if (t.getType() == JavaParser.CLASS_TOP_LEVEL_SCOPE) {
                int count = t.getChildCount();
                for (int i=0; i< count; i++) {
                    CommonTree child =(CommonTree)t.getChild(i);
                    MemberInfo info = null;
                    if (child.getType() == JavaParser.VOID_METHOD_DECL
                            || child.getType() == JavaParser.FUNCTION_METHOD_DECL) {
                        info = parseFuncDecl(child, MemberType.METHOD); 
                    } else if (child.getType() == JavaParser.CONSTRUCTOR_DECL) {
                        info = parseFuncDecl(child,MemberType.CONSTRUCTOR); 
                    } else if (child.getType() == JavaParser.VAR_DECLARATION) {
                        info = parseFieldDecl(child); 
                    }
                    this.memberInfos.add(info);
                }
            }
            if (t.getType() ==  JavaParser.IMPORT) {
                StringBuilder sb = new StringBuilder();
                buildImportStr((CommonTree)t.getChild(0),sb);
                sb.deleteCharAt(0);
                importedNames.add(sb.toString());
	    	}
            for ( int i = 0; i < t.getChildCount(); i++ ) {
            	readClassInfo((CommonTree)t.getChild(i));
            }
        }
    }
    
    private void buildImportStr(CommonTree t, StringBuilder b) {
        if (t.getType() == JavaParser.IDENT) {
            b.append(".").append(t.getText());
        } else if (t.getType() == JavaParser.DOT) {
            for (int i=0; i<t.getChildCount(); i++) {
                buildImportStr((CommonTree)t.getChild(i), b);
            }
        }
    }

    private List<String> parseArgumentTypenameList(CommonTree tree) {
        List<String> typenameList = new ArrayList<String>();
        for (int i=0; i<tree.getChildCount(); i++) {
            CommonTree arguNode = (CommonTree) tree.getChild(i);
            String typeName = parseNodeTypeName(arguNode);
            typenameList.add(typeName);
        }
        return typenameList;
    }

    private String parseNodeTypeName(CommonTree node) {
        if (node.getType() == JavaParser.EXPR) {
            node = (CommonTree)node.getChild(0);
        }
        String typename = "";

        switch (node.getType()) {
            case JavaParser.PLUS :
                typename = parseNodeTypeName((CommonTree)node.getChild(0));
                break;
            case JavaParser.IDENT:
                typename = findvarType(node.getText());
                break;
            case JavaParser.DECIMAL_LITERAL :
            case JavaParser.HEX_LITERAL :
            case JavaParser.OCTAL_LITERAL:
                typename = "int";
                break;
            case JavaParser.STRING_LITERAL :
                typename = "String";
                break;
            case JavaParser.FLOATING_POINT_LITERAL :
                typename = "float";
                break;
            case JavaParser.METHOD_CALL:
                String methodName = node.getChild(0).getText();
                List<String> typenameList = parseArgumentTypenameList((CommonTree)node.getChild(1));
                MemberInfo memberInfo = findMatchedMethod(methodName, typenameList,this.memberInfos);
                if (memberInfo != null )  {
                    typename = memberInfo.getRtnType();
                } else {
                    typename = "unknow";
                }
                break;
            case JavaParser.THIS:
                typename = "this";
                break;
            case JavaParser.NULL:
                typename = NULL_TYPE;
                break;
            default :
                typename = "unknown";
        }
        return convertTypeName(typename);


    }
    
    private String convertTypeName(String typeName) {
    	if (typeName.equals("this")) {
    		return ctx.buildClassName(this.currentFileName);
    	} 
    	for (String importedName: this.importedNames) {
    		String lastName = importedName.substring(importedName.lastIndexOf(".")+1);
    		if (typeName.equals(lastName)) {
    			return importedName;
    		}
    	}
    	return typeName;
    }

    private MemberInfo findMatchedField(String fieldName,  List<MemberInfo> memberInfos) {
        for (MemberInfo member: memberInfos) { 
            if (member.getName().equals(fieldName) ) {
                return member;
            }
        }
        return null;
    }

    private MemberInfo findMatchedMethod(String methodName, List<String> argTypes, 
            List<MemberInfo> memberInfos) {
        for (MemberInfo member: memberInfos) { 
            if (member.getName().equals(methodName) 
                    && member.getParamList() != null 
                    && member.getParamList().size() == argTypes.size()) {
                List<String[]> memberParamList = member.getParamList();
                boolean noMatch = false;
                for (int i=0; i<argTypes.size(); i++) {
                    String actTypeName = argTypes.get(i);
                    String defTypeName = memberParamList.get(i)[0];
                    if (!arguMatch(defTypeName, actTypeName)) {
                        noMatch = true;
                        break;
                    }
                }
                if (! noMatch) return member;
            }
        }
        return null;
    }

    private boolean arguMatch(String defTypeName, String actTypeName) {
        if (defTypeName.equals(actTypeName)) return true;
        if (actTypeName.equals(NULL_TYPE)) return true;
        return false;
    }

    private String findvarType(String varName ) {
        //TODO : search in super class
        for (LocalVariableInfo var : visibleVars) {
            if (var.getName().equals(varName)) {
                return var.getType();
            }
        }
        char s = varName.charAt(0);
        if ( s >= 'A' && s <= 'Z' ) {
            return varName;
        }
        return "unknown";
    }

    private CommonTree searchMatchedNode(CommonTree tree, int line, int col) {
          LinkedList<CommonTree> nodes = new LinkedList<CommonTree>();
          searchNodeAtLine(tree,nodes,line);
          CommonTree matchedNode = null;
          for (CommonTree t : nodes) {
              if (col > t.getCharPositionInLine() ) {
                  matchedNode = t;
                  break;
              }
          }
          return matchedNode;
    }


    private void searchNodeAtLine(CommonTree t, LinkedList<CommonTree> list, int line) {
        if (t.getLine() == line)  {
            if (t.getChildCount() == 0 ) {
                list.add(0,t);
            }
        }
        for (int i=0; i<t.getChildCount(); i++) {
            CommonTree c = (CommonTree)t.getChild(i);
            searchNodeAtLine(c,list,line);
        }
    }
    
    private void searchTreeForVar(CommonTree t, int line,List<MemberInfo> memberList) {
    	if (hasFound) return;
        if (t.getLine() == line) {
        	parseAllVisibleVar(t);
        	hasFound = true;
        }
        for (int i=0; i<t.getChildCount(); i++) {
            CommonTree c = (CommonTree)t.getChild(i);
            searchTreeForVar(c,line,memberList);
        }
    }

    
    private List<LocalVariableInfo> parseAllVisibleVar(CommonTree t) {
        List<LocalVariableInfo> infoList = new ArrayList<LocalVariableInfo>();
    	CommonTree parent = t;
    	while (true) {
	    	parent = (CommonTree)parent.getParent();
	    	if (parent == null) break;
	    	for (int i=0; i < parent.getChildCount(); i++) {
	    		CommonTree child = (CommonTree) parent.getChild(i);
	    		if (child.getType() == JavaParser.VAR_DECLARATION) {
                    MemberInfo membeInfo = parseFieldDecl(child);
                    LocalVariableInfo info = new LocalVariableInfo();
                    info.setName(membeInfo.getName());
                    info.setType(membeInfo.getRtnType());
                    info.setLine(membeInfo.getLineNum());
                    info.setCol(membeInfo.getColumn());
                    infoList.add(info);
	    		}
                if (child.getType() == JavaParser.FORMAL_PARAM_LIST) {
                    infoList.addAll(parseParamlist(child));
                }
	    	}
    	}
        return infoList;
    }

    

    private List<LocalVariableInfo> parseParamlist(CommonTree t) {
        List<LocalVariableInfo> result = new ArrayList<LocalVariableInfo>();
        for (int i=0; i< t.getChildCount(); i++) {
            CommonTree c = (CommonTree)t.getChild(i);
            if (c.getType() == JavaParser.FORMAL_PARAM_STD_DECL) {

                LocalVariableInfo info = new LocalVariableInfo();

                for (int j=0; j< c.getChildCount(); j++) {
                    CommonTree part = (CommonTree)c.getChild(j);
                    if (part.getType() == JavaParser.LOCAL_MODIFIER_LIST) continue;
                    if (part.getType() == JavaParser.TYPE) {
                        info.setType(parseType(part));
                    } else {
                        info.setCol(part.getCharPositionInLine());
                        info.setLine(part.getLine());
                        info.setName(part.getText());
                    }
                }
                result.add(info);
            }
        }
        return result;
    }

    private MemberInfo parseFieldDecl(CommonTree t) {
        MemberInfo info = new MemberInfo();
        info.setMemberType(MemberType.FIELD);

        for (int i=0; i< t.getChildCount(); i++) {
            CommonTree c = (CommonTree)t.getChild(i);
            if (c.getType() == JavaParser.VAR_DECLARATOR_LIST) {
                CommonTree variableDeclaratorId = (CommonTree)((CommonTree)c.getChild(0)).getChild(0);

                info.setLineNum(variableDeclaratorId.getLine());
                info.setColumn(variableDeclaratorId.getCharPositionInLine());
                info.setName(variableDeclaratorId.getText());
                if (info.getName().equals("age")) {
                    int a = 10;
                }
            } else if (c.getType() == JavaParser.MODIFIER_LIST) {
                info.setModifierList( parseModifierList(c));
            //} else if (c.getType() == JavaParser.FORMAL_PARAM_LIST) {
            //    info.setParamList(parseFormalParamList(c));
            } else if (c.getType() == JavaParser.TYPE) {
                info.setRtnType(parseType(c));
            }
        }
        return info;
    }


    private MemberInfo parseFuncDecl(CommonTree t,MemberType memberType ) {
        MemberInfo info = new MemberInfo();
        info.setLineNum(t.getLine());
        info.setMemberType(memberType);

        for (int i=0; i< t.getChildCount(); i++) {
            CommonTree c = (CommonTree)t.getChild(i);
            if (c.getType() == JavaParser.IDENT) {
            	info.setColumn(c.getCharPositionInLine());
                info.setName(c.getText());
            } else if (c.getType() == JavaParser.MODIFIER_LIST) {
                info.setModifierList( parseModifierList(c));
            } else if (c.getType() == JavaParser.FORMAL_PARAM_LIST) {
                info.setParamList(parseFormalParamList(c));
            } else if (c.getType() == JavaParser.TYPE) {
                info.setRtnType(parseType(c));
            }
        }
        return info;
    }
    
    private String parseType(CommonTree t) {
        StringBuilder sb = new StringBuilder();
        for (int i=0; i<t.getChildCount(); i++) {
            CommonTree c = (CommonTree) t.getChild(i);
            if (c.getType() == JavaParser.QUALIFIED_TYPE_IDENT) {
                sb.append(parseQualifiedTypeIdent(c));
            } else if (c.getType() == JavaParser.ARRAY_DECLARATOR_LIST){
            	 sb.append(parseArrayDeclaratorList(c));
            } else {
                sb.append(c.getText());
            }
        }
        return convertTypeName(sb.toString());
    }
    
    private String parseArrayDeclaratorList(CommonTree t) {
        StringBuilder sb = new StringBuilder();
        for (int i=0; i<t.getChildCount(); i++) {
            CommonTree c = (CommonTree) t.getChild(i);
            sb.append("[]");
        }
        return sb.toString();
    }


    private String parseQualifiedTypeIdent(CommonTree t) {
        StringBuilder sb = new StringBuilder();
        for (int i=0; i<t.getChildCount(); i++) {
            CommonTree c = (CommonTree) t.getChild(i);
            sb.append(c.getText());
        }
        return sb.toString();
    }

    private List<String> parseModifierList(CommonTree t) {
        List<String> result = new ArrayList<String>();
        for (int i=0; i<t.getChildCount(); i++) {
            CommonTree c = (CommonTree) t.getChild(i);
            if (c.getType() == JavaParser.AT) {
            	continue;
            }
            result.add(c.getText());
        }
        return result;
    }

    private List<String[]> parseFormalParamList(CommonTree t) {
        List<String[]> paramList = new ArrayList<String[]>();
        for (int i=0; i<t.getChildCount(); i++) {
            CommonTree c = (CommonTree) t.getChild(i);
            String[] param = new String[2];
            if (c.getType() == JavaParser.FORMAL_PARAM_STD_DECL) {
                param = parseFormalParamStdDecl(c);
            }
            paramList.add(param);
        }
        return paramList;
    }
    
    private String[] parseFormalParamStdDecl(CommonTree t) {
        String[] param = new String[2];
        for (int i=0; i<t.getChildCount(); i++) {
            CommonTree c = (CommonTree) t.getChild(i);
            if (c.getType() == JavaParser.LOCAL_MODIFIER_LIST) continue;
            if (c.getType() == JavaParser.TYPE) {
                param[0] = parseType(c);
            } else {
                param[1] = c.getText();
            }
        }
        return param;
    }


}
