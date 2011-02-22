package com.google.code.vimsztool.debug;

import com.google.code.vimsztool.compiler.CompilerContext;
import com.google.code.vimsztool.compiler.CompilerContextManager;
import com.google.code.vimsztool.server.SzjdeCommand;
import com.google.code.vimsztool.server.SzjdeConstants;
import com.sun.jdi.request.StepRequest;

public class DebugCommand  extends SzjdeCommand {
	
	private Debugger debugger = Debugger.getInstance();
	private BreakpointManager bpMgr = BreakpointManager.getInstance();
	
	private static String[] availCmds = { "launch", "exit", "print", 
		"breakpoints", "stack", "attach","breakpoint_add", "breakpoint_remove",
		"step_into","step_over","step_return", "resume"};
	
	public String execute() {
		String classPathXml = params.get(SzjdeConstants.PARAM_CLASSPATHXML);
		String debugCmdArgs = params.get("debugCmdArgs");
		String serverName = params.get("serverName");
		
		CompilerContextManager ccm = CompilerContextManager.getInstnace();
		CompilerContext ctx = ccm.getCompilerContext(classPathXml);
		if (debugCmdArgs == null || debugCmdArgs.trim().equals("")) {
			return "";
		}
		String[] args = debugCmdArgs.split(" ");
		String debugCmd = args[0];
		
		String actionResult = "";
		
		if (!isAvailCmd(debugCmd)) {
			return "not a valid command";
		}
		
		if (debugCmd.equals("launch")) {
			String mainClass = args[1];
			debugger.setVimServerName(serverName);
			actionResult = debugger.launch(mainClass, classPathXml);
		} else if (debugCmd.equals("attach")) {
			String port = args[1];
			actionResult = debugger.attach(port);
		} else if (debugCmd.equals("breakpoint_add")) {
			String fileName = args[1];
			int lineNum = Integer.parseInt(args[2]);
			String mainClass = ctx.buildClassName(fileName);
			actionResult = bpMgr.addBreakpoint(mainClass, lineNum);
		} else if (debugCmd.equals("breakpoint_remove")) {
			String fileName = args[1];
			String mainClass = ctx.buildClassName(fileName);
			int lineNum = Integer.parseInt(args[2]);
			actionResult = bpMgr.removeBreakpoint(mainClass, lineNum);
		} else if (debugCmd.equals("step_into")) {
			actionResult = StepManager.step(StepRequest.STEP_INTO);
		} else if (debugCmd.equals("step_over")) {
			actionResult = StepManager.step(StepRequest.STEP_OVER);
		} else if (debugCmd.equals("step_return")) {
			actionResult = StepManager.step(StepRequest.STEP_OUT);
		} else if (debugCmd.equals("print")) {
			String exp = args[1];
			actionResult =  ExpressionEval.eval(exp);
		} else if (debugCmd.equals("resume")) {
			debugger.resume();
		} else if (debugCmd.equals("exit")) {
			debugger.exit();
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
