package com.google.code.vimsztool.server;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.google.code.vimsztool.compiler.CompileResultInfo;
import com.google.code.vimsztool.compiler.CompilerContext;
import com.google.code.vimsztool.compiler.JDTCompiler;
import com.google.code.vimsztool.debug.BreakpointManager;
import com.google.code.vimsztool.debug.Debugger;
import com.google.code.vimsztool.omni.ClassMetaInfoManager;
import com.google.code.vimsztool.parser.JavaSourceSearcher;
import com.google.code.vimsztool.util.BufferStore;
import com.google.code.vimsztool.util.HotSwapUtil;
import com.google.code.vimsztool.util.IdGenerator;
import com.google.code.vimsztool.util.JdeLogger;
import com.google.code.vimsztool.util.VjdeUtil;

public class SzjdeCompilerCommand extends SzjdeCommand {
	private static JdeLogger log = JdeLogger.getLogger("SzjdeCompilerCommand");
	private String bufname;
	private String vimServerName;
	private ScheduledExecutorService exec = null;
	private StringWriter strWriter = new StringWriter();
	private PrintWriter out = new PrintWriter(strWriter);  
	private StringBuffer buffer = strWriter.getBuffer();
	private String uuid= null;
	
	public String execute() {
		
		
		StringBuffer resultBuffer = new StringBuffer();
		
		vimServerName = params.get(SzjdeConstants.PARAM_VIM_SERVER);
		bufname = params.get(SzjdeConstants.PARAM_BUF_NAME);
		
		String sourceFile = params.get(SzjdeConstants.PARAM_SOURCEFILE);
		if (sourceFile.equals("All")) {
			uuid=IdGenerator.getUniqueId();
			BufferStore.put(uuid, buffer);
			exec = Executors.newScheduledThreadPool(1);
	        exec.scheduleAtFixedRate(new BufferChecker(buffer,uuid), 1, 200, TimeUnit.MILLISECONDS);
	        new CompilerJob(resultBuffer,out).start();
		}  else {
	        new CompilerJob(resultBuffer,out).run();
		}
        return resultBuffer.toString();
	}
	
	private void finishCompiler(StringBuffer resultBuffer) {
		exec.shutdown();
		
		new BufferChecker(buffer,uuid).run();
		
		uuid=IdGenerator.getUniqueId();
		BufferStore.put(uuid, resultBuffer);
		
		String funcName = "HandleBuildResult";
		String[] args = {uuid};
		VjdeUtil.callVimFunc(vimServerName, funcName, args);
	}
	
	private void hotSwapClass(CompileResultInfo resultInfo) {
		
		for (Debugger debugger : Debugger.getInstances()) {
			BreakpointManager bpm = debugger.getBreakpointManager();
			List<String[]> outputs = resultInfo.getOutputInfo();
			for (String[] names : outputs) {
				String className = names[0];
				String outputPath = names[1];
				HotSwapUtil.replace(debugger, new File(outputPath), className);
				bpm.tryResetBreakpointRequest(className);
			}
		}
		
	}
	
	class CompilerJob extends Thread {
		
		private StringBuffer sb = null;
		private PrintWriter out = null;
		
		public CompilerJob(StringBuffer compilerResultBuffer, PrintWriter outputWriter) {
			this.sb = compilerResultBuffer;
			this.out = outputWriter;
		}
		
		public void run() {
			String classPathXml = params.get(SzjdeConstants.PARAM_CLASSPATHXML);
			String sourceFile = params.get(SzjdeConstants.PARAM_SOURCEFILE);
			CompilerContext cc=getCompilerContext(classPathXml);
			JDTCompiler compiler =new JDTCompiler(cc);
			String[] allSrcFiles = new String[] {};
			if (sourceFile.equals("All")) {
				allSrcFiles = cc.getAllSourceFiles();
			} else {
				ClassMetaInfoManager metaInfoManager = cc.getClassMetaInfoManager();
				String targetClassName=cc.buildClassName(sourceFile);
				Set<String> dependentClasses = metaInfoManager.getDependentClasses(targetClassName);
				List<String> srcFileList = new ArrayList<String>();
				for (String depClass : dependentClasses ) {
					String rtlPathName = depClass.replace(".", "/") + ".java";
					String sourcePath = cc.findSourceFileInSrcPath(rtlPathName);
					if (sourcePath != null ) {
						srcFileList.add(sourcePath);
					}
				}
				if (!srcFileList.contains(sourceFile)) {
					srcFileList.add(sourceFile);
				}
				allSrcFiles = srcFileList.toArray(new String[]{});
			}
			
			for (String srcFileName : allSrcFiles) {
				JavaSourceSearcher.clearSearcher(srcFileName);
			}
			
			CompileResultInfo resultInfo =compiler.generateClass(allSrcFiles,out);
			List<String> problemList = resultInfo.getProblemInfoList();
			for (String srcFile : allSrcFiles) {
				sb.append(srcFile).append("\n");
			}
			sb.append("$$$$$");
			
			if (!resultInfo.isError() && !sourceFile.equals("All")) {
				try {
					hotSwapClass(resultInfo);
				} catch (Throwable e) {
					log.info("hotswap class file failed.");
				}
			}
			
			for (String problem : problemList) {
				sb.append(problem);
			}
			out.println("(jde project build finished.)");
			
			if (sourceFile.equals("All")) {
				finishCompiler(sb);
			}
			
		}
	}
	
	class BufferChecker implements Runnable {
		private StringBuffer buffer;
		private String uuid;
		
		private BufferChecker(StringBuffer buffer,String uuid) {
			this.buffer = buffer;
			this.uuid = uuid;
		}
		public void run() {
			synchronized (buffer) {
				if ( ! (buffer.length() > 0)) return; 
				String[] args = new String[] { uuid,bufname };
				VjdeUtil.callVimFunc(vimServerName, "FetchResult", args);
				
			}
		}
	}
}
