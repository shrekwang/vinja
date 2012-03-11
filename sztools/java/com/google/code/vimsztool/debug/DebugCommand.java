package com.google.code.vimsztool.debug;

import com.google.code.vimsztool.compiler.CompilerContext;
import com.google.code.vimsztool.compiler.CompilerContextManager;
import com.google.code.vimsztool.server.SzjdeCommand;
import com.google.code.vimsztool.server.SzjdeConstants;
import com.sun.jdi.request.StepRequest;

public class DebugCommand  extends SzjdeCommand {
	
	private Debugger debugger = Debugger.getInstance();
	private BreakpointManager bpMgr = BreakpointManager.getInstance();
	private WatchVariableManager wvMgr = WatchVariableManager.getInstance();
	private ExceptionPointManager ecpm  = ExceptionPointManager.getInstance();
	
	private static String[] availCmds = { "run", "exit", "print", "eval","inspect",
		"breakpoints","locals","fields","frames", "attach","breakpoint_add", "breakpoint_remove",
		"step_into","step_over","step_return", "resume", "shutdown" ,"catch", "watch","show_watch",
		"unwatch","ignore","clear", "threads","thread", "syncbps","disconnect","reftype","frame" , "jval"
		};
	
	public String execute() {
		String classPathXml = params.get(SzjdeConstants.PARAM_CLASSPATHXML);
		String debugCmdArgs = params.get("debugCmdArgs");
		
		String serverName = params.get("serverName");
		debugger.setVimServerName(serverName);
		
		CompilerContextManager ccm = CompilerContextManager.getInstnace();
		CompilerContext ctx = ccm.getCompilerContext(classPathXml);
		debugger.setCompilerContext(ctx);
		
		if (debugCmdArgs == null || debugCmdArgs.trim().equals("")) {
			return "";
		}
		String[] args = debugCmdArgs.split(" ");
		String debugCmd = args[0];
		
		String actionResult = "";
		
		if (!isAvailCmd(debugCmd)) {
			return "not a valid command";
		}
		
		if (debugCmd.equals("run")) {
			String cmdLine = debugCmdArgs.substring(4).trim();
			actionResult = debugger.launch(classPathXml, cmdLine);
		} else if (debugCmd.equals("attach")) {
			String port = args[1];
			actionResult = debugger.attach(port);
		} else if (debugCmd.equals("breakpoint_add")) {
			String mainClass = args[1];
			int lineNum = Integer.parseInt(args[2]);
			//String mainClass = ctx.buildClassName(fileName);	
			actionResult = bpMgr.addBreakpoint(mainClass, lineNum);
		} else if (debugCmd.equals("breakpoint_remove")) {
			String mainClass = args[1];
			//String mainClass = ctx.buildClassName(fileName);
			int lineNum = Integer.parseInt(args[2]);
			actionResult = bpMgr.removeBreakpoint(mainClass, lineNum);
		} else if (debugCmd.equals("clear")) {
			String mainClass = args[1];
			int lineNum = Integer.parseInt(args[2]);
			actionResult = bpMgr.removeBreakpoint(mainClass, lineNum);
		} else if (debugCmd.equals("step_into")) {
			actionResult = StepManager.step(StepRequest.STEP_INTO);
		} else if (debugCmd.equals("step_over")) {
			actionResult = StepManager.step(StepRequest.STEP_OVER);
		} else if (debugCmd.equals("step_return")) {
			actionResult = StepManager.step(StepRequest.STEP_OUT);
		} else if (debugCmd.equals("eval") || debugCmd.equals("print")
				|| debugCmd.equals("inspect") || debugCmd.equals("locals")
				|| debugCmd.equals("fields") || debugCmd.equals("reftype")) {
			actionResult =  ExpressionEval.executeEvalCmd(debugCmd, debugCmdArgs);
		} else if (debugCmd.equals("watch")) {
			String exp = debugCmdArgs.substring(debugCmd.length()+1);
			actionResult =  wvMgr.addWatchVariables(exp);
		} else if (debugCmd.equals("unwatch")) {
			String exp = debugCmdArgs.substring(debugCmd.length()+1);
			actionResult =  wvMgr.removeWatchVariables(exp);
		} else if (debugCmd.equals("show_watch")) {
			actionResult =  wvMgr.evalWatchVariables();
		} else if (debugCmd.equals("breakpoints")) {
			actionResult = debugger.listBreakpoints();
		} else if (debugCmd.equals("frames")) {
			actionResult = debugger.listFrames();
		} else if (debugCmd.equals("threads")) {
			actionResult = debugger.listThreads();
		} else if (debugCmd.equals("thread")) {
			String uniqueId = args[1];
			actionResult = debugger.changeCurrentThread(uniqueId);
		} else if (debugCmd.equals("frame") ) {
			int frameNum = Integer.parseInt(args[1]);
			actionResult = debugger.changeCurrentFrame(frameNum);
		} else if (debugCmd.equals("catch")) {
			String className = args[1];
			actionResult = ecpm.addExceptionPoint(className);
		} else if (debugCmd.equals("ignore")) {
			String className = args[1];
			actionResult = ecpm.removeExceptionRequest(className);
		} else if (debugCmd.equals("resume")) {
			debugger.resume();
		} else if (debugCmd.equals("shutdown")) {
			debugger.shutdown();
		} else if (debugCmd.equals("exit")) {
			debugger.disconnectOrExit();
		} else if (debugCmd.equals("syncbps")) {
			actionResult = bpMgr.allBreakpointsInfo();
		} else if (debugCmd.equals("disconnect")) {
			debugger.disconnectOrExit();
		} else if (debugCmd.equals("jval")) {
			ExpEval jval = new ExpEval();
			String exp = debugCmdArgs.substring(4).trim();
			actionResult = jval.eval(exp);
		}
		return actionResult;
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
