package com.google.code.vimsztool.parser;


public enum MemberType { 

    FIELD(1), METHOD(2), CONSTRUCTOR(4), ENUM(8), SUBCLASS(16);

    int value ;

    MemberType(int value) {
        this.value = value;
    }

}
