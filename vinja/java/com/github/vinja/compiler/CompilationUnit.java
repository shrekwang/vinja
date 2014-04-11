
package com.github.vinja.compiler;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.StringTokenizer;

import org.eclipse.jdt.internal.compiler.env.ICompilationUnit;

public class CompilationUnit implements ICompilationUnit {
	

    String className;
    String sourceFile;
    String encoding;
    String sourceText;
    

    CompilationUnit(String sourceFile, String className,String encoding) {
        this.className = className;
        this.sourceFile = sourceFile;
        this.encoding=encoding;
    }

    public char[] getFileName() {
        return sourceFile.toCharArray();
    }
    
    public char[] getContents() {
        char[] result = null;
        FileInputStream is = null;
        try {
            is = new FileInputStream(sourceFile);
            Reader reader = 
                new BufferedReader(new InputStreamReader(is, encoding));
            if (reader != null) {
                char[] chars = new char[8192];
                StringBuffer buf = new StringBuffer();
                int count;
                while ((count = reader.read(chars, 0, 
                                            chars.length)) > 0) {
                    buf.append(chars, 0, count);
                }
                result = new char[buf.length()];
                buf.getChars(0, result.length, result, 0);
            }
        } catch (IOException e) {
        	e.printStackTrace();
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException exc) {
                    // Ignore
                }
            }
        }
        this.sourceText = new String(result);
        return result;
    }
    
    public char[] getMainTypeName() {
        int dot = className.lastIndexOf('.');
        if (dot > 0) {
            return className.substring(dot + 1).toCharArray();
        }
        return className.toCharArray();
    }
    
    public char[][] getPackageName() {
        StringTokenizer izer = 
            new StringTokenizer(className, ".");
        char[][] result = new char[izer.countTokens()-1][];
        for (int i = 0; i < result.length; i++) {
            String tok = izer.nextToken();
            result[i] = tok.toCharArray();
        }
        return result;
    }
    
    public String getLine(int lineNumber) {
    	String[] lines = sourceText.split("\n");
    	if (lineNumber < lines.length) {
    		return lines[lineNumber-1].replace("\r", "");
    	}
    	return "";
    }

}


