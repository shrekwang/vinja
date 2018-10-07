package com.github.vinja.debug;

import java.util.List;

import com.github.vinja.compiler.CompilerContext;
import com.github.vinja.parser.IJavaSourceSearcher;
import com.github.vinja.parser.VinjaJavaSourceSearcher;
import com.github.vinja.util.ClassNameUtil;
import com.sun.jdi.Location;
import com.sun.jdi.StackFrame;
import com.sun.jdi.ThreadReference;
import com.sun.jdi.VirtualMachine;
import com.sun.jdi.request.EventRequest;
import com.sun.jdi.request.EventRequestManager;
import com.sun.jdi.request.StepRequest;

public class StepManager {
	
	private Debugger debugger;
	
	public StepManager(Debugger debugger) {
		this.debugger = debugger;
	}
	

	public String step(int stepDepth, int count) {
		VirtualMachine vm = debugger.getVm();
		SuspendThreadStack threadStack = debugger.getSuspendThreadStack();
		ThreadReference threadRef = threadStack.getCurThreadRef();

		EventRequestManager mgr = vm.eventRequestManager();

		List<StepRequest> steps = mgr.stepRequests();
		for (int i = 0; i < steps.size(); i++) {
			StepRequest step = steps.get(i);
			if (step.thread().equals(threadRef)) {
				mgr.deleteEventRequest(step);
				break;
			}
		}

		if (threadRef == null) {
			return "";
		}
		
		
		StepRequest request = mgr.createStepRequest(threadRef, StepRequest.STEP_LINE, stepDepth);
		List<String> excludeFilters = StepFilterConfiger.getDefaultFilter(); 
		for (String filter : excludeFilters) {
			request.addClassExclusionFilter(filter);
		}
		request.addCountFilter(count);
		request.setSuspendPolicy(EventRequest.SUSPEND_EVENT_THREAD);
		request.enable();
		
		threadStack.clean();
		threadRef.resume();
		return "";
	}
	
	public String stepOut() {
		try {
			CompilerContext ctx = debugger.getCompilerContext();
			SuspendThreadStack threadStack = debugger.getSuspendThreadStack();
			ThreadReference threadRef = threadStack.getCurThreadRef();
			StackFrame stackFrame = threadRef.frame(threadStack.getCurFrame());
			Location loc = stackFrame.location();
			
			String locClassName = ClassNameUtil.sourceToClassName(loc.sourcePath());
		    String abPath = ctx.findSourceOrBinPath(locClassName);

		    IJavaSourceSearcher searcher = VinjaJavaSourceSearcher.createSearcher(abPath,ctx);
		    int currentLine = loc.lineNumber();
		    int outLine = searcher.searchLoopOutLine(currentLine);
		    if (outLine == -1) {
		    	return step(StepRequest.STEP_OUT, 1);
		    }
			BreakpointManager bpMgr = debugger.getBreakpointManager();
			String className = loc.declaringType().name();
		    bpMgr.addTempBreakpoint(className, outLine,false);
		    
			threadStack.clean();
		    threadRef.resume();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return "";
	}

}
