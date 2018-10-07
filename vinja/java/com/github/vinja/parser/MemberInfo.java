package com.github.vinja.parser;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.AnnotationMemberDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.EnumConstantDeclaration;
import com.github.javaparser.ast.body.EnumDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.body.VariableDeclarator;

public class MemberInfo {

	private static Pattern regex = Pattern.compile("([a-zA-Z1-9]+)(\\.[a-zA-Z1-9]+)*\\.([a-zA-Z1-9]+)");
    private String name;
    private String rtnType;
    private MemberType memberType;

    private int lineNum; 
    private int column;

    private List<String[]> paramList;
    private List<String> modifierList;
    
    //for inner enum,class etc.
    private List<MemberInfo> subMemberList;

    public void setName(String name) {
        this.name=name;
    }
    public String getName() {
        return this.name;
    }


    public void setRtnType(String rtnType) {
        this.rtnType=rtnType;
    }

    public List<String[]> getGenericInfoErasedParamList() {
    	if (paramList == null || paramList.size() == 0 )  return paramList;
    	List<String[]> result = new ArrayList<String[]>();
    	for (String[] paramPair : paramList) {
    		String[] newPair = new String[] {paramPair[0] , unstripGegericInfo(paramPair[1])};
    		result.add(newPair);
    	}
    	return result;
    }

    public String getGenericInfoErasedRtnType() {
		if (rtnType == null) return "void";
		return unstripGegericInfo(rtnType);
	}
    
    private String unstripGegericInfo(String type) {
		if (type.indexOf("<") < 0 ) return type;
        String tempType = type.substring(0, type.indexOf("<"));
        if ( type.lastIndexOf(">")  < type.length() - 1 ) {
        	tempType = tempType + type.substring(type.lastIndexOf(">")+1);
        }
        return tempType;
    }

    public String getRtnType() {
    	if (rtnType == null) return "void";
    	return rtnType;
	}

    public String getShortRtnType() {
    	if (rtnType == null) return "void";
        return shortName(this.rtnType);
    }


    public void setLineNum(int lineNum) {
        this.lineNum=lineNum;
    }
    public int getLineNum() {
        return this.lineNum;
    }


    public void setModifierList(List<String> modifierList) {
        this.modifierList=modifierList;
    }
    public List<String> getModifierList() {
        return this.modifierList;
    }

    public String getModifierDesc() {
    	if (this.modifierList == null || this.modifierList.size() ==0 ) return "";
    	StringBuilder sb = new StringBuilder();
    	for (String mod: this.modifierList) {
    		sb.append(mod).append(" ");
    	}
    	return sb.toString();
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        if (modifierList != null) {
	        for (String str : modifierList) {
	            sb.append(str).append(" ");
	        }
        }
        if (memberType == MemberType.METHOD) {
            sb.append(" " + rtnType + " " + name + "(" + formatParamList() + ")" + "  : " + lineNum);
        } else if (memberType == MemberType.CONSTRUCTOR) {
            sb.append(" " + name + "(" + formatParamList() + ")" + "  : " + lineNum);
        } else if (memberType == MemberType.FIELD) {
            sb.append(rtnType + " " + name + "  : " + lineNum);
        }
        return sb.toString();
    }

    public void setMemberType(MemberType memberType) {
        this.memberType=memberType;
    }
    public MemberType getMemberType() {
        return this.memberType;
    }
    
    public String getMemeberTypeDesc() {
    	if (memberType == null) return "";
    	return memberType.getName();
    }

    public String formatParamList() {
    	if (this.paramList == null || this.paramList.size() == 0 ) return "";
        StringBuilder sb = new StringBuilder();
        for(String[] param : this.paramList) {
            sb.append(shortName(param[0])).append(" ").append(param[1]);
            sb.append(",");
        }
        if (sb.length() > 0) sb.deleteCharAt(sb.length()-1);
        return sb.toString();
    }

    public void setParamList(List<String[]> paramList) {
        this.paramList=paramList;
    }
    public List<String[]> getParamList() {
        return this.paramList;
    }

    public void setColumn(int column) {
        this.column=column;
    }
    public int getColumn() {
        return this.column;
    }
	public List<MemberInfo> getSubMemberList() {
		return subMemberList;
	}
	public void setSubMemberList(List<MemberInfo> subMemberList) {
		this.subMemberList = subMemberList;
	}

	private synchronized String shortName(String typeName) {
		if (typeName == null) return "";
		if (typeName.indexOf(".") < 0 ) return typeName;
		Matcher regexMatcher = regex.matcher(typeName);
        String resultString = regexMatcher.replaceAll("$3");
		return resultString;
	}

    public static MemberInfo from(VinjaJavaSourceSearcher searcher, AnnotationMemberDeclaration annoMethod) {
        MemberInfo info = new MemberInfo();

        List<String> modifierList = new ArrayList<String>();
        for (Iterator<Modifier> it = annoMethod.getModifiers().iterator(); it.hasNext();) {
            modifierList.add(it.next().asString());
        }
        info.setModifierList(modifierList);

        info.setName(annoMethod.getNameAsString());
        ClassLocInfo classLocInfo = searcher.findTypeSourceLocInfo(annoMethod.getType());
        info.setRtnType(classLocInfo.getClassName());
        info.setLineNum(annoMethod.getName().getRange().get().begin.line);
        info.setColumn(annoMethod.getName().getRange().get().begin.column);
        info.setMemberType(MemberType.METHOD);
        List<String[]> paramList = new ArrayList<String[]>();
        info.setParamList(paramList);
        return info;
    }
    
    public static MemberInfo from(VinjaJavaSourceSearcher searcher, ConstructorDeclaration consDeclare, String typeName) {
		MemberInfo info = new MemberInfo();

		List<String> modifierList = new ArrayList<String>();
		for (Iterator<Modifier> it = consDeclare.getModifiers().iterator(); it.hasNext();) {
			modifierList.add(it.next().asString());
		}
		info.setModifierList(modifierList);

		info.setName(consDeclare.getNameAsString());
		info.setRtnType(typeName);
		info.setLineNum(consDeclare.getName().getRange().get().begin.line);
		info.setColumn(consDeclare.getName().getRange().get().begin.column);
		info.setMemberType(MemberType.CONSTRUCTOR);
		NodeList<Parameter> parameters = consDeclare.getParameters();
		List<String[]> paramList = new ArrayList<String[]>();
		for (Parameter param : parameters) {
			ClassLocInfo classInfo = searcher.findTypeSourceLocInfo(param.getType());
			if (classInfo == null) {
				System.out.println("parma type null" + param);
			}
			String paramJavaType = classInfo.getClassName();
			String paramName = param.getNameAsString();
			String[] methodParam = new String[] { paramJavaType, paramName };
			paramList.add(methodParam);
		}
		info.setParamList(paramList);
		return info;
    }
    
    public static MemberInfo from(VinjaJavaSourceSearcher searcher, MethodDeclaration methodDeclare) {

		MemberInfo info = new MemberInfo();
		List<String> modifierList = new ArrayList<String>();
		for (Iterator<Modifier> it = methodDeclare.getModifiers().iterator(); it.hasNext();) {
			modifierList.add(it.next().asString());
		}
		info.setModifierList(modifierList);

		info.setName(methodDeclare.getNameAsString());
		ClassLocInfo classLocInfo = searcher.findTypeSourceLocInfo(methodDeclare.getType());
		info.setRtnType(classLocInfo.getClassName());
		info.setLineNum(methodDeclare.getName().getRange().get().begin.line);
		info.setColumn(methodDeclare.getName().getRange().get().begin.column);
		info.setMemberType(MemberType.METHOD);
		NodeList<Parameter> parameters = methodDeclare.getParameters();
		List<String[]> paramList = new ArrayList<String[]>();
		for (Parameter param : parameters) {
			ClassLocInfo classInfo = searcher.findTypeSourceLocInfo(param.getType());
			if (classInfo == null) {
				System.out.println("parma type null" + param);
			}
			String paramJavaType = classInfo.getClassName();
			String paramName = param.getNameAsString();
			String[] methodParam = new String[] { paramJavaType, paramName };
			paramList.add(methodParam);
		}
		info.setParamList(paramList);
		return info;
    }
    
    public static List<MemberInfo> from(VinjaJavaSourceSearcher searcher, FieldDeclaration field) {

    	List<MemberInfo> infos = new ArrayList<>();
    	NodeList<VariableDeclarator> variables = field.getVariables();
		for (VariableDeclarator var : variables) {
			MemberInfo info = new MemberInfo();

			List<String> modifierList = new ArrayList<String>();
			for (Iterator<Modifier> it = field.getModifiers().iterator(); it.hasNext();) {
				modifierList.add(it.next().asString());
			}
			info.setModifierList(modifierList);
			
			ClassLocInfo classLocInfo = searcher.findTypeSourceLocInfo(var.getType());
			info.setRtnType(classLocInfo.getClassName());

			info.setMemberType(MemberType.FIELD);
			info.setName(var.getNameAsString());
			info.setLineNum(var.getName().getRange().get().begin.line);
			info.setColumn(var.getName().getRange().get().begin.column);
			infos.add(info);
		}
		return infos;
    }
    
    public static MemberInfo from(VinjaJavaSourceSearcher searcher, EnumConstantDeclaration enumDeclaration, String typeName) {
    	MemberInfo info = new MemberInfo();
		info.setMemberType(MemberType.FIELD);
		info.setName(enumDeclaration.getNameAsString());
		info.setLineNum(enumDeclaration.getName().getRange().get().begin.line);
		info.setRtnType(typeName);
		info.setColumn(enumDeclaration.getName().getRange().get().begin.column);
		return info;
    }
    
    public static MemberInfo from(VinjaJavaSourceSearcher searcher, ClassOrInterfaceDeclaration subTypeDeclare, String typeName) {
    	MemberInfo info = new MemberInfo();
		info.setMemberType(MemberType.SUBCLASS);
		info.setName(subTypeDeclare.getNameAsString());
		info.setLineNum(subTypeDeclare.getRange().get().begin.line);
		info.setColumn(subTypeDeclare.getRange().get().begin.column);
		info.setRtnType(typeName);
		return info;
    }
    
    public static MemberInfo from(VinjaJavaSourceSearcher searcher, EnumDeclaration enumDec) {
		MemberInfo info = new MemberInfo();
		info.setMemberType(MemberType.SUBCLASS);
		info.setName(enumDec.getNameAsString());
		info.setLineNum(enumDec.getRange().get().begin.line);
		info.setColumn(enumDec.getRange().get().begin.column);
		return info;
    }
}
