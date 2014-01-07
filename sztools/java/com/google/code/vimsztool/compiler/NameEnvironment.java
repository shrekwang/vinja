
package com.google.code.vimsztool.compiler;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;

import org.apache.commons.io.FilenameUtils;
import org.eclipse.jdt.internal.compiler.classfmt.ClassFileReader;
import org.eclipse.jdt.internal.compiler.env.ICompilationUnit;
import org.eclipse.jdt.internal.compiler.env.INameEnvironment;
import org.eclipse.jdt.internal.compiler.env.NameEnvironmentAnswer;

public class NameEnvironment implements INameEnvironment {

	
	private HashMap<String,String> targetClassNames= new HashMap<String,String>();
	private CompilerContext ctx;
	
	public NameEnvironment(String sourceFiles[], CompilerContext ctx) {
		this.ctx=ctx;
		for (int i=0; i<sourceFiles.length; i++) {
			targetClassNames.put(ctx.buildClassName(sourceFiles[i]), sourceFiles[i]);
		}
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
    
    private boolean isValidResource(URL url) throws IOException {
    	if (url == null) return true;
    	if (!url.getProtocol().equals("file")) return true;
    	File file = new File(url.getFile());
    	if (!file.exists()) return false;
    	String name1 = FilenameUtils.getBaseName(file.getAbsolutePath());
    	String name2 = FilenameUtils.getBaseName(file.getCanonicalPath());
    	return name1.equals(name2);
    }
    
    
    private NameEnvironmentAnswer findType(String className) {

        InputStream is = null;
        try {
        	String sourceFile = targetClassNames.get(className);
        	if (sourceFile != null) {
                ICompilationUnit compilationUnit = 
                	new CompilationUnit(sourceFile, className,ctx.getEncoding());
                return 
                    new NameEnvironmentAnswer(compilationUnit, null);
            }
            String resourceName = 
                className.replace('.', '/') + ".class";
            ClassLoader classLoader=ctx.getClassLoader();
            is = classLoader.getResourceAsStream(resourceName);
            if (is != null && isValidResource(classLoader.getResource(resourceName))) {
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
		if (targetClassNames.get(result) != null) {
			return false;
		}
		String resourceName = result.replace('.', '/') + ".class";
		try {
			ClassLoader classLoader = ctx.getClassLoader();
			// InputStream is = classLoader.getResourceAsStream(resourceName);
			URL url = classLoader.getResource(resourceName);
			if (url != null) {
				URLConnection uc = url.openConnection();
				uc.setUseCaches(false);
				InputStream is = uc.getInputStream();
				if (is != null && isValidResource(classLoader.getResource(resourceName))) {
					return false;
				}
			}
			return true;
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


