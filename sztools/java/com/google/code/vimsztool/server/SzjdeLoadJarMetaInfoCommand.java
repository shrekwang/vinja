package com.google.code.vimsztool.server;

import java.net.URL;
import java.util.List;

import com.google.code.vimsztool.compiler.CompilerContext;
import com.google.code.vimsztool.omni.ClassMetaInfoManager;

public class SzjdeLoadJarMetaInfoCommand extends SzjdeCommand {

	public String execute() {
		String classPathXml = params.get(SzjdeConstants.PARAM_CLASSPATHXML);
		CompilerContext ctx = getCompilerContext(classPathXml);
		List<URL> urls = ctx.getClassPathUrls();
		ClassMetaInfoManager cmm = ctx.getClassMetaInfoManager();
		
		for (URL url : urls) {
			String path = url.getPath();
			if (path.endsWith(".jar")) {
				cmm.loadMetaInfoInJar(path);
			}
		}
		cmm.constructAllSubNames();
		return "success";
	}

}