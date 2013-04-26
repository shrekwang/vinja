package com.google.code.vimsztool.server;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.UUID;

import org.apache.commons.io.FileUtils;

import com.google.code.vimsztool.compiler.CompileResultInfo;
import com.google.code.vimsztool.compiler.CompilerContext;
import com.google.code.vimsztool.compiler.CompilerContextManager;
import com.google.code.vimsztool.compiler.JDTCompiler;
import com.google.code.vimsztool.parser.JavaSourceSearcher;
import com.google.code.vimsztool.util.BufferStore;
import com.google.code.vimsztool.util.VjdeUtil;

public class SzjdeProjectClean extends SzjdeShextCommand {
	
	private StringBuffer sb= new StringBuffer();

	@Override
	public Thread createShextJob() {
		Thread job = new Thread() {
			public void run() {
				String classPathXml = params.get(SzjdeConstants.PARAM_CLASSPATHXML);
				CompilerContext cc=getCompilerContext(classPathXml);
				out.println("clean output dir...");
				out.println("");
				File outputDir = new File(cc.getOutputDir());
				for (File subFile : outputDir.listFiles()) {
					try {
						if (subFile.isFile()) {
							subFile.delete();
						} else {
							FileUtils.deleteDirectory(subFile);
						}
					} catch (Exception e) {
						out.println(e.getMessage());
					}
				}
				
				out.println("reload classpath...");
				out.println("");
				CompilerContextManager ccm = CompilerContextManager.getInstnace();
				ccm.reloadCompilerContext(classPathXml, false);
				
				//get reloaded compilerContext
				cc=getCompilerContext(classPathXml);
				out.println("compile java source...");
				out.println("");
				compileSource(cc);
				out.println("");
				
				out.println("copy resource file...");
				out.println("");
				copyResource(cc);
					
				out.println("refresh cache...");
				cc.cacheClassInfo();
			}
			
			private void copyResource(CompilerContext cc) {
				String[] allSrcFiles = cc.getAllResourceFiles();
				for (String srcPath : allSrcFiles) {
					String dstPath = cc.getResourceDistFile(srcPath);
					//srcPath not under any configured source path in .class xml
					if (dstPath == null) continue;
					File srcFile = new File(srcPath);
					File dstFile = new File(dstPath);
					try {
						copyFile(srcFile,dstFile);
					} catch (IOException e) {
						out.println(e.getMessage());
					}
				}
					
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
			
			private void compileSource(CompilerContext cc) {
				JDTCompiler compiler =new JDTCompiler(cc);
				String[] allSrcFiles = cc.getAllSourceFiles();
				
				for (String srcFileName : allSrcFiles) {
					JavaSourceSearcher.clearSearcher(srcFileName);
				}
				
				CompileResultInfo resultInfo =compiler.generateClass(allSrcFiles,out);
				List<String> problemList = resultInfo.getProblemInfoList();
				for (String srcFile : allSrcFiles) {
					sb.append(srcFile).append("\n");
				}
				sb.append("$$$$$");
				
				for (String problem : problemList) {
					sb.append(problem);
				}
				
			}
		};
		return job;
	}

	@Override
	public String getCmdName() {
		return "jde project clean";
	}

	public Thread callBackJob () {
		return new Thread() {
			public void run() {
				uuid=UUID.randomUUID().toString();
				BufferStore.put(uuid, sb);
				
				String funcName = "HandleBuildResult";
				String[] args = {uuid};
				VjdeUtil.callVimFunc(vimServerName, funcName, args);
			}
		};
	}
	

}