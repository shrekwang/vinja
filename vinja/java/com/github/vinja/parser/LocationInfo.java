package com.github.vinja.parser;


public class LocationInfo {

    private String filePath;
    private int line=1;
    private int col=0;
    
    private MemberInfo memberInfo;
    
    public void setFilePath(String filePath) {
        this.filePath=filePath;
    }
    public String getFilePath() {
        return this.filePath;
    }

    public void setCol(int col) {
        this.col=col;
    }
    public int getCol() {
        return this.col;
    }


    public void setLine(int line) {
        this.line=line;
    }
    public int getLine() {
        return this.line;
    }
	public MemberInfo getMemberInfo() {
		return memberInfo;
	}
	public void setMemberInfo(MemberInfo memberInfo) {
		this.memberInfo = memberInfo;
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		if (this.filePath != null) {
			sb.append("filePath:").append(this.filePath).append("\n");
			sb.append("line:").append(this.line).append("\n");
			sb.append("col:").append(this.col).append("\n");
		}
		return sb.toString();
	}

}
