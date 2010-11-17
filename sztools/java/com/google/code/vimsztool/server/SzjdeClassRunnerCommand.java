package com.google.code.vimsztool.server;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.List;

import com.google.code.vimsztool.compiler.CompilerContext;
import com.google.code.vimsztool.util.VjdeUtil;

public class SzjdeClassRunnerCommand extends SzjdeCommand {

	public String execute() {
		String classPathXml = params.get(SzjdeConstants.PARAM_CLASSPATHXML);
		String sourceFile = params.get(SzjdeConstants.PARAM_SOURCEFILE);
		CompilerContext cc = getCompilerContext(classPathXml);
		List<URL> urls = cc.getClassPathUrls();
		StringBuilder cmd = new StringBuilder("java -cp ");
		for (URL url : urls) {
			cmd.append(url.getPath()).append(File.pathSeparator);
		}

		cmd.append(" ");
		cmd.append(cc.buildClassName(sourceFile));

		try {
			String line;
			File workingDir = new File(cc.getProjectRoot());
			Process p = Runtime.getRuntime().exec(cmd.toString(),new String[]{},workingDir);
			BufferedReader input = new BufferedReader(new 
					InputStreamReader(p.getInputStream()));
			StringBuilder runResult = new StringBuilder();
			while ((line = input.readLine()) != null) {
				runResult.append(line).append("\n");
			}
			input.close();
			return runResult.toString();
		} catch (Exception err) {
			return VjdeUtil.getExceptionValue(err);
		}
	}

}