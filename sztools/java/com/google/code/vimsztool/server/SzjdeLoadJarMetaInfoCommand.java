package com.google.code.vimsztool.server;

import java.net.URL;
import java.util.List;

import com.google.code.vimsztool.compiler.CompilerContext;
import com.google.code.vimsztool.omni.ClassMetaInfoManager;

public class SzjdeLoadJarMetaInfoCommand extends SzjdeShextCommand {

	@Override
	public Thread createShextJob() {
		Thread job = new Thread() {
			public void run() {
				String classPathXml = params.get(SzjdeConstants.PARAM_CLASSPATHXML);
				CompilerContext ctx = getCompilerContext(classPathXml);
				List<URL> urls = ctx.getClassPathUrls();
				ClassMetaInfoManager cmm = ctx.getClassMetaInfoManager();
				
				for (URL url : urls) {
					String path = url.getPath();
					if (path.endsWith(".jar")) {
						cmm.loadMetaInfoInJar(path);
						out.println("loadding " +path + " ...");
					}
				}
				cmm.constructAllSubNames();
				out.println("all jar info loaded.");
			}
		};
		return job;
	}

	@Override
	public String getCmdName() {
		return "jde project loadjar";
	}

}