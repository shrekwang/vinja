package com.google.code.vimsztool.parser;

import java.util.List;

public class MemberInfo {

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
    public String getRtnType() {
    	if (rtnType == null) return "void";
        return this.rtnType;
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
    	return this.memberType.getName();
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

	private String shortName(String typeName) {
		if (typeName == null) return "";
		if (typeName.indexOf(".") < 0 ) return typeName;
		return typeName.substring(typeName.lastIndexOf(".")+1);
	}
}
