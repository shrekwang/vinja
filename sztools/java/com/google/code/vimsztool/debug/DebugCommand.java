package com.google.code.vimsztool.debug;

import com.google.code.vimsztool.compiler.CompilerContext;
import com.google.code.vimsztool.compiler.CompilerContextManager;
import com.google.code.vimsztool.server.SzjdeCommand;
import com.google.code.vimsztool.server.SzjdeConstants;
import com.google.code.vimsztool.debug.eval.ExpEval;
import com.google.code.vimsztool.exception.NoConnectedVmException;
import com.google.code.vimsztool.exception.NoSuspendThreadException;
import com.sun.jdi.request.StepRequest;

public class DebugCommand  extends SzjdeCommand {
	
	
	private static String[] availCmds = { "run", "runtest","exit", "print", "eval","inspect",
		"breakpoints","locals","fields","frames", "attach","breakpoint_add", "breakpoint_remove",
		"step_into","step_over","step_return","step_out", "resume", "shutdown" ,"catch", 
		"unwatch","ignore","clear", "threads","thread", "syncbps","disconnect","reftype","frame" , 
		"setvalue","runtomcat","fetchJdbResult","until","display","displayi","undisplay",
		"show_display","eval_display","tbreak", "watch","rwatch","awatch","up","down",
		"sfields","sinspect","qeval","geval","ginspect", "fetchAutocmdResult","sizeof",
		"resume_all","enable","disable"
		};
	
	public String execute() {
		try {
			String classPathXml = params.get(SzjdeConstants.PARAM_CLASSPATHXML);
			String debugCmdLine = params.get("debugCmdArgs");
			String serverName = params.get("serverName");
			Debugger debugger = Debugger.getInstance(serverName);
			debugger.setVimServerName(serverName);
			String actionResult = execute(debugger, classPathXml, debugCmdLine);
			
			return actionResult;
		} catch (NoConnectedVmException e) {
			return "no virtual machine connected.";
		} catch (NoSuspendThreadException e) {
			return "no suspend thread.";
		}
	}
	
	public String execute(Debugger debugger, String classPathXml, String debugCmdLine) {
		
		
		BreakpointManager bpMgr = debugger.getBreakpointManager();
		DisplayVariableManager dvMgr = debugger.getDisplayVariableManager();
		ExceptionPointManager ecpMgr = debugger.getExceptionPointManager();
		StepManager stepMgr = debugger.getStepMgr();
		
		CompilerContextManager ccm = CompilerContextManager.getInstnace();
		CompilerContext ctx = ccm.getCompilerContext(classPathXml);
		debugger.setCompilerContext(ctx);
		debugger.setClassPathXml(classPathXml);
		
		if (debugCmdLine == null || debugCmdLine.trim().equals("")) {
			return "";
		}
		String[] args = debugCmdLine.split("\\s+");
		String debugCmd = args[0];
		
		String actionResult = "";
		
		if (!isAvailCmd(debugCmd)) {
			return debugCmd + " is not a valid command, try \"help\".";
		}
		
		if (debugCmd.equals("run")) {
			String cmdLine = debugCmdLine.substring(4).trim();
			actionResult = debugger.launch(classPathXml, cmdLine,false);
		} else if (debugCmd.equals("runtest")) {
			String cmdLine = debugCmdLine.substring(8).trim();
			actionResult = debugger.launch(classPathXml, cmdLine,true);
		} else if (debugCmd.equals("attach")) {
			String host = null;
			String port = null;
			if (args.length == 2 ) {
				port = args[1];
			} else {
				host = args[1];
				port = args[2];
			}
			actionResult = debugger.attach(host,port);
		} else if (debugCmd.equals("breakpoint_add")) {
			actionResult = bpMgr.addBreakpoint(debugCmdLine);
		} else if (debugCmd.equals("until")) {
			int lineNum = Integer.parseInt(args[1]);
			String mainClass = args[2];
			actionResult = bpMgr.addTempBreakpoint(mainClass, lineNum,true);
		} else if (debugCmd.equals("tbreak")) {
			int lineNum = Integer.parseInt(args[1]);
			String mainClass = args[2];
			actionResult = bpMgr.addTempBreakpoint(mainClass, lineNum,false);
		} else if (debugCmd.equals("watch")) {
			String fieldName = args[1];
			String mainClass = args[2];
			actionResult = bpMgr.addWatchpoint(mainClass, fieldName, Breakpoint.ACCESS_WRITE);
		} else if (debugCmd.equals("rwatch")) {
			String fieldName = args[1];
			String mainClass = args[2];
			actionResult = bpMgr.addWatchpoint(mainClass, fieldName, Breakpoint.ACCESS_READ);
		} else if (debugCmd.equals("awatch")) {
			String fieldName = args[1];
			String mainClass = args[2];
			actionResult = bpMgr.addWatchpoint(mainClass, fieldName, Breakpoint.ACCESS_READ | Breakpoint.ACCESS_WRITE);
		} else if (debugCmd.equals("unwatch")) {
			String fieldName = args[1];
			String mainClass = args[2];
			actionResult = bpMgr.removeWatchpoint(mainClass, fieldName);
		} else if (debugCmd.equals("breakpoint_remove")) {
			String mainClass = args[2];
			int lineNum = Integer.parseInt(args[1]);
			actionResult = bpMgr.removeBreakpoint(mainClass, lineNum);
		} else if (debugCmd.equals("enable")) {
			String mainClass = args[2];
			String loc = args[1];
			actionResult = bpMgr.setBreakpointEnable(mainClass, loc, true);
		} else if (debugCmd.equals("disable")) {
			String mainClass = args[2];
			String loc = args[1];
			actionResult = bpMgr.setBreakpointEnable(mainClass, loc, false);
		} else if (debugCmd.equals("step_into")) {
			int count = this.getStepCount(args);
			actionResult = stepMgr.step(StepRequest.STEP_INTO, count);
		} else if (debugCmd.equals("step_over")) {
			int count = this.getStepCount(args);
			actionResult = stepMgr.step(StepRequest.STEP_OVER, count);
		} else if (debugCmd.equals("step_return")) {
			int count = this.getStepCount(args);
			actionResult = stepMgr.step(StepRequest.STEP_OUT, count);
		} else if (debugCmd.equals("step_out")) {
			actionResult = stepMgr.stepOut();
		} else if (debugCmd.equals("eval") || debugCmd.equals("print")
				|| debugCmd.equals("inspect") || debugCmd.equals("locals")
				|| debugCmd.equals("fields") || debugCmd.equals("reftype")
				|| debugCmd.equals("sfields") || debugCmd.equals("sinspect")
				|| debugCmd.equals("geval") || debugCmd.equals("qeval") 
				|| debugCmd.equals("ginspect") || debugCmd.equals("sizeof")) {
			ExpEval expEval = debugger.getExpEval();
			actionResult =  expEval.executeEvalCmd(debugCmd, debugCmdLine);
		} else if (debugCmd.equals("display")) {
			String exp = debugCmdLine.substring(debugCmd.length()+1);
			actionResult =  dvMgr.addWatchExpression(exp);
		} else if (debugCmd.equals("displayi")) {
			String exp = debugCmdLine.substring(debugCmd.length()+1);
			actionResult =  dvMgr.addInspectExpression(exp);
		} else if (debugCmd.equals("undisplay")) {
			String exp = debugCmdLine.substring(debugCmd.length()+1);
			actionResult =  dvMgr.removeWatchVariables(exp);
		} else if (debugCmd.equals("show_display")) {
			actionResult =  dvMgr.showWatchVariables();
		} else if (debugCmd.equals("eval_display")) {
			actionResult =  dvMgr.evalWatchVariables();
		} else if (debugCmd.equals("breakpoints")) {
			actionResult = debugger.listBreakpoints();
		} else if (debugCmd.equals("frames")) {
			actionResult = debugger.listFrames();
		} else if (debugCmd.equals("threads")) {
			actionResult = debugger.listThreads();
		} else if (debugCmd.equals("thread")) {
			if (args.length == 1 ) {
				debugger.changeToNextSuspnedThread();
				return "success";
			} else {
				String uniqueId = args[1];
				actionResult = debugger.changeCurrentThread(uniqueId);
			}
		} else if (debugCmd.equals("frame") ) {
			int frameNum = Integer.parseInt(args[1]);
			actionResult = debugger.changeCurrentFrame(frameNum);
			if (actionResult!=null && actionResult.equals("success")){
				actionResult = debugger.listFrames();
			}
		} else if (debugCmd.equals("up") ) {
			actionResult = debugger.currentFrameUp();
			if (actionResult!=null && actionResult.equals("success")){
				actionResult = debugger.listFrames();
			}
		} else if (debugCmd.equals("down") ) {
			actionResult = debugger.currentFrameDown();
			if (actionResult!=null && actionResult.equals("success")){
				actionResult = debugger.listFrames();
			}
		} else if (debugCmd.equals("catch")) {
			String className = args[1];
			actionResult = ecpMgr.addExceptionPoint(className);
		} else if (debugCmd.equals("ignore")) {
			String className = args[1];
			actionResult = ecpMgr.removeExceptionRequest(className);
		} else if (debugCmd.equals("resume")) {
			debugger.resume();
		} else if (debugCmd.equals("resume_all")) {
			debugger.resumeAll();
		} else if (debugCmd.equals("shutdown")) {
			debugger.shutdown();
		} else if (debugCmd.equals("exit")) {
			debugger.disconnectOrExit();
		} else if (debugCmd.equals("syncbps")) {
			actionResult = bpMgr.allBreakpointsInfo();
		} else if (debugCmd.equals("disconnect")) {
			debugger.disconnectOrExit();
		} else if (debugCmd.equals("setvalue")) {
			String name = args[1];
			String valueExp = debugCmdLine.substring(debugCmdLine.indexOf(name,"setvalue".length()) + name.length());
			ExpEval expEval = debugger.getExpEval();
			actionResult =  expEval.setFieldValue(name,valueExp);
		} else if (debugCmd.equals("runtomcat")) {
			actionResult = debugger.launchTomcat();
		} else if (debugCmd.equals("fetchJdbResult")) {
			actionResult = debugger.fetchResult();
		} else if (debugCmd.equals("fetchAutocmdResult")) {
			actionResult = debugger.fetchAutocmdResult();
		} 
		return actionResult;
	}
	
	private int getStepCount(String[] args) {
		if (args == null || args.length < 2 ) return 1;
		try {
			int count = Integer.parseInt(args[1]);
			return count;
		} catch (Exception e) {
			return 1;
		}
	}
	
	public boolean isAvailCmd(String cmd) {
		for (String availCmd : availCmds) {
			if (availCmd.equals(cmd)) {
				return true;
			}
		}
		return false;
	}

}
