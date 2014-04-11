package com.github.vinja.server;

import static com.github.vinja.server.SzjdeConstants.PARAM_CLASSPATHXML;
import static com.github.vinja.server.SzjdeConstants.PARAM_DUMP_CLASS;

import com.github.vinja.compiler.CompilerContext;
import com.github.vinja.compiler.ReflectAbleClassLoader;
import com.github.vinja.omni.ClassInfoUtil;

public class SzjdeDumpClassInfoCommand extends SzjdeCommand {

	@SuppressWarnings("all")
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
			String dumpInfo = ClassInfoUtil.dumpClassInfo(aClass);
			if (dumpInfo.length() > 0) return dumpInfo;
		}

		return "";
	}

}