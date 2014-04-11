package com.github.vinja.omni;

public class ReferenceLocation {
	 public String className;
     public String methodName;
     public String methodDesc;
     public String source;
     public int line;

     public ReferenceLocation(String cName, String mName, String mDesc, String src, int ln) {
         className = cName; 
         methodName = mName; 
         methodDesc = mDesc; 
         source = src; 
         line = ln;
     }
}
