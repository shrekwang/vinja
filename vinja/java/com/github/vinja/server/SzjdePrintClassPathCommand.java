package com.github.vinja.server;

import java.util.List;

import com.github.vinja.compiler.CompilerContext;

public class SzjdePrintClassPathCommand extends SzjdeShextCommand {

	@Override
	public Thread createShextJob() {
		Thread job = new Thread() {
			public void run() {
				String classPathXml = params.get(SzjdeConstants.PARAM_CLASSPATHXML);
				CompilerContext ctx = getCompilerContext(classPathXml);
				List<String> urls = ctx.getFsClassPathUrls();
				
				for (String path : urls) {
                    out.println(path);
				}
			}
		};
		return job;
	}

	@Override
	public String getCmdName() {
		return "jde project classpath";
	}

}