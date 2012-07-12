package com.google.code.vimsztool.parser;


public enum MemberType { 

    FIELD(1), METHOD(2), CONSTRUCTOR(4);

    int value ;

    MemberType(int value) {
        this.value = value;
    }

}
