package com.github.vinja.parser;


public enum MemberType { 

    FIELD(1), METHOD(2), CONSTRUCTOR(4), ENUM(8), SUBCLASS(16);
    

    int value ;
    
    MemberType(int value) {
        this.value = value;
    }
    
    public String getName() {
    	if (this.value == 1) return "field";
    	if (this.value == 2) return "method";
    	if (this.value == 4) return "constructor";
    	if (this.value == 8) return "enum";
    	if (this.value == 16) return "class";
    	return "";
    }

}
