
package com.google.code.vimsztool.compiler;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.eclipse.jdt.internal.compiler.classfmt.ClassFileReader;
import org.eclipse.jdt.internal.compiler.env.ICompilationUnit;
import org.eclipse.jdt.internal.compiler.env.INameEnvironment;
import org.eclipse.jdt.internal.compiler.env.NameEnvironmentAnswer;

public class NameEnvironment implements INameEnvironment {

	
	private String sourceFile;
	private String targetClassName;
	private CompilerContext ctx;
	
	public NameEnvironment(String sourceFile, 
			String targetClassName,CompilerContext ctx) {
		this.sourceFile=sourceFile;
		this.targetClassName=targetClassName;
		this.ctx=ctx;
	}
	
    public NameEnvironmentAnswer findType(char[][] compoundTypeName) {
        String result = "";
        String sep = "";
        for (int i = 0; i < compoundTypeName.length; i++) {
            result += sep;
            result += new String(compoundTypeName[i]);
            sep = ".";
        }
        return findType(result);
    }

    public NameEnvironmentAnswer  findType(char[] typeName,  char[][] packageName) {
            String result = "";
            String sep = "";
            for (int i = 0; i < packageName.length; i++) {
                result += sep;
                result += new String(packageName[i]);
                sep = ".";
            }
            result += sep;
            result += new String(typeName);
            return findType(result);
    }
    
    private NameEnvironmentAnswer findType(String className) {

        InputStream is = null;
        try {
            if (className.equals(targetClassName)) {
                ICompilationUnit compilationUnit = 
                    new CompilationUnit(sourceFile, className,ctx.getEncoding());
                return 
                    new NameEnvironmentAnswer(compilationUnit, null);
            }
            String resourceName = 
                className.replace('.', '/') + ".class";
            ClassLoader classLoader=ctx.getClassLoader();
            is = classLoader.getResourceAsStream(resourceName);
            if (is != null) {
                byte[] classBytes;
                byte[] buf = new byte[8192];
                ByteArrayOutputStream baos = 
                    new ByteArrayOutputStream(buf.length);
                int count;
                while ((count = is.read(buf, 0, buf.length)) > 0) {
                    baos.write(buf, 0, count);
                }
                baos.flush();
                classBytes = baos.toByteArray();
                char[] fileName = className.toCharArray();
                ClassFileReader classFileReader = 
                    new ClassFileReader(classBytes, fileName, 
                                        true);
                return 
                    new NameEnvironmentAnswer(classFileReader, null);
            }
        } catch (IOException exc) {
        	exc.printStackTrace();
        } catch (org.eclipse.jdt.internal.compiler.classfmt.ClassFormatException exc) {
        	exc.printStackTrace();
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException exc) {
                    // Ignore
                }
            }
        }
        return null;
    }

    private boolean isPackage(String result) {
        if (result.equals(targetClassName)) {
            return false;
        }
        String resourceName = result.replace('.', '/') + ".class";
        try {
	        ClassLoader classLoader=ctx.getClassLoader();
	        InputStream is = classLoader.getResourceAsStream(resourceName);
	        return is == null;
        } catch (Exception e) {
        	e.printStackTrace();
        }
        return false;
    }

    public boolean isPackage(char[][] parentPackageName, 
                             char[] packageName) {
        String result = "";
        String sep = "";
        if (parentPackageName != null) {
            for (int i = 0; i < parentPackageName.length; i++) {
                result += sep;
                String str = new String(parentPackageName[i]);
                result += str;
                sep = ".";
            }
        }
        String str = new String(packageName);
        if (Character.isUpperCase(str.charAt(0))) {
            if (!isPackage(result)) {
                return false;
            }
        }
        result += sep;
        result += str;
        return isPackage(result);
    }

    public void cleanup() {
    }


}


