package com.google.code.vimsztool.server;

import static com.google.code.vimsztool.server.SzjdeConstants.PARAM_CLASSPATHXML;
import static com.google.code.vimsztool.server.SzjdeConstants.PARAM_DUMP_CLASS;

import com.google.code.vimsztool.compiler.CompilerContext;
import com.google.code.vimsztool.compiler.ReflectAbleClassLoader;
import com.google.code.vimsztool.omni.ClassInfo;

public class SzjdeDumpClassInfoCommand extends SzjdeCommand {

	@SuppressWarnings("unchecked")
	public String execute() {
		String classPathXml = params.get(PARAM_CLASSPATHXML);
		String dumpClass = params.get(PARAM_DUMP_CLASS);
		String[] classNameList = dumpClass.split(",");

		CompilerContext ctx = getCompilerContext(classPathXml);
		ReflectAbleClassLoader classLoader = ctx.getClassLoader();

		Class aClass = null;
		for (String className : classNameList) {
			try {
				aClass = classLoader.loadClass(className);
			} catch (ClassNotFoundException e) {
				try {
					aClass = classLoader.loadClass("java.lang." + className);
				} catch (ClassNotFoundException e2) { }
			}
			if (aClass==null) continue;
			ClassInfo classInfo = new ClassInfo();
			String dumpInfo = classInfo.dumpClassInfo(aClass);
			if (dumpInfo.length() > 0) return dumpInfo;
		}

		return "";
	}

}