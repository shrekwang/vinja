
package com.github.vinja.compiler;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;

import org.eclipse.jdt.internal.compiler.classfmt.ClassFileReader;
import org.eclipse.jdt.internal.compiler.env.ICompilationUnit;
import org.eclipse.jdt.internal.compiler.env.INameEnvironment;
import org.eclipse.jdt.internal.compiler.env.NameEnvironmentAnswer;

public class NameEnvironment implements INameEnvironment {

	private HashMap<String, String> targetClassNames = new HashMap<String, String>();
	private CompilerContext ctx;

	public NameEnvironment(String sourceFiles[], CompilerContext ctx) {
		this.ctx = ctx;
		for (int i = 0; i < sourceFiles.length; i++) {
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

	public NameEnvironmentAnswer findType(char[] typeName, char[][] packageName) {
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
		
		String sourceFile = targetClassNames.get(className);
		if (sourceFile != null) {
			ICompilationUnit compilationUnit = new CompilationUnit(sourceFile, className, ctx.getEncoding());
			return new NameEnvironmentAnswer(compilationUnit, null);
		}

		String resourceName = className.replace('.', '/') + ".class";
		char[] fileName = className.toCharArray();
		byte[] classBytes = ctx.getClassLoader().getResourceByte(ctx.getProjectRoot(),resourceName);
		if (classBytes == null) return  null;

		try {
			ClassFileReader classFileReader = new ClassFileReader(classBytes, fileName, true);
			return new NameEnvironmentAnswer(classFileReader, null);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	private boolean isPackage(String result) {
		if (targetClassNames.get(result) != null) {
			return false;
		}

		ReflectAbleClassLoader classLoader = ctx.getClassLoader();
		String resourceName = result.replace('.', '/') + ".class";
		return classLoader.getResourceByte(ctx.getProjectRoot(),resourceName) == null;

	}

	public boolean isPackage(char[][] parentPackageName, char[] packageName) {
		StringBuilder result = new StringBuilder();
		int i = 0;
		if (parentPackageName != null) {
			for (; i < parentPackageName.length; i++) {
				if (i > 0)
					result.append('.');
				result.append(parentPackageName[i]);
			}
		}

		if (Character.isUpperCase(packageName[0])) {
			if (!isPackage(result.toString())) {
				return false;
			}
		}
		if (i > 0)
			result.append('.');
		result.append(packageName);

		return isPackage(result.toString());

	}

	public void cleanup() {
	}

}
