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
        return this.rtnType;
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

    private String formatParamList() {
        StringBuilder sb = new StringBuilder();
        for(String[] param : this.paramList) {
            sb.append(param[0]).append(" ").append(param[1]);
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

}
