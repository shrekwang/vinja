package com.google.code.vimsztool.parser;

public class LocalVariableInfo {

    private String name;
    private String type;

    private int line; 
    private int col;

    public void setName(String name) {
        this.name=name;
    }
    public String getName() {
        return this.name;
    }


    public void setType(String type) {
        this.type=type;
    }
    public String getType() {
        return this.type;
    }

    public void setLine(int line) {
        this.line=line;
    }
    public int getLine() {
        return this.line;
    }


    public void setCol(int col) {
        this.col=col;
    }
    public int getCol() {
        return this.col;
    }

}
