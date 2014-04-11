package com.google.code.vimsztool.server;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import com.google.code.vimsztool.compiler.CompilerContext;
import com.google.code.vimsztool.util.VjdeUtil;

public class SzjdeCopyResourceCommand  extends SzjdeCommand {
	
	public String execute() {
		String classPathXml = params.get(SzjdeConstants.PARAM_CLASSPATHXML);
		String srcPath = params.get(SzjdeConstants.PARAM_SOURCEFILE);
		
		File file = new File(classPathXml);
		if (file.isDirectory()) return "";
		
		CompilerContext cc=getCompilerContext(classPathXml);
		String dstPath = cc.getResourceDistFile(srcPath);
		//srcPath not under any configured source path in .class xml
		if (dstPath == null) return "";
		File srcFile = new File(srcPath);
		File dstFile = new File(dstPath);
		try {
			copyFile(srcFile,dstFile);
		} catch (IOException e) {
			return VjdeUtil.getExceptionValue(e);
		}
		return "";
	}
	
	private void copyFile(File srcFile, File dstFile) throws IOException {
		if (Thread.currentThread().isInterrupted()) return;
		
		File dstParent = dstFile.getParentFile();
		if (!dstParent.exists()) dstParent.mkdirs();
		FileInputStream input = new FileInputStream(srcFile);
		FileOutputStream output = new FileOutputStream(dstFile);
		try {
			byte[] buffer = new byte[1024 * 16];
			int n = 0;
			while (-1 != (n = input.read(buffer)) ) {
				output.write(buffer, 0, n);
			}
			dstFile.setLastModified(srcFile.lastModified());
		} finally {
			try { if (input != null) input.close(); } catch (Exception e) { }
			try { if (output != null) output.close(); } catch (Exception e) { }
		}
	}
}
