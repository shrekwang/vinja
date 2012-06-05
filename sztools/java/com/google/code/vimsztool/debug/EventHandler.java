package com.google.code.vimsztool.debug;

import com.google.code.vimsztool.compiler.CompilerContext;
import com.google.code.vimsztool.debug.eval.ExpEval;
import com.google.code.vimsztool.util.VjdeUtil;
import com.sun.jdi.Location;
import com.sun.jdi.ReferenceType;
import com.sun.jdi.ThreadReference;
import com.sun.jdi.VirtualMachine;
import com.sun.jdi.event.BreakpointEvent;
import com.sun.jdi.event.ClassPrepareEvent;
import com.sun.jdi.event.Event;
import com.sun.jdi.event.EventIterator;
import com.sun.jdi.event.EventQueue;
import com.sun.jdi.event.EventSet;
import com.sun.jdi.event.ExceptionEvent;
import com.sun.jdi.event.LocatableEvent;
import com.sun.jdi.event.StepEvent;
import com.sun.jdi.event.VMDeathEvent;
import com.sun.jdi.event.VMDisconnectEvent;
import com.sun.jdi.event.VMStartEvent;
import com.sun.jdi.event.WatchpointEvent;
import com.sun.jdi.request.BreakpointRequest;
import com.sun.jdi.request.EventRequestManager;

public class EventHandler extends Thread {

	private VirtualMachine vm;
	private EventQueue eventQueue;
	private EventSet eventSet;
	private EventRequestManager eventRequestManager;
	private boolean vmExit = false;

	public EventHandler(VirtualMachine vm) {
		this.vm = vm;
		this.eventRequestManager = vm.eventRequestManager();
	}

	public void run() {
		eventQueue = vm.eventQueue();
		while (true) {
			if (vmExit == true) {
				Debugger debugger = Debugger.getInstance();
				debugger.disconnectOrExit();
				break;
			}
			try {
				eventSet = eventQueue.remove();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			EventIterator eventIterator = eventSet.eventIterator();
			while (eventIterator.hasNext()) {
				Event event = (Event) eventIterator.next();
				if (event instanceof VMStartEvent) {
					handleVMStartEvent((VMStartEvent)event);
				} else if (event instanceof ClassPrepareEvent) {
					handleClassPrepareEvent((ClassPrepareEvent)event);
				} else if (event instanceof BreakpointEvent) {
					handleBreakpointEvent((BreakpointEvent)event);
				} else if (event instanceof VMDisconnectEvent) {
					handleVMDisconnectEvent((VMDisconnectEvent)event);
					vmExit = true;
				} else if (event instanceof VMDeathEvent) {
					handleVMDeathEvent((VMDeathEvent)event);
					vmExit = true;
				} else if (event instanceof ExceptionEvent) {
					handleExceptionEvent((ExceptionEvent)event);
				} else if (event instanceof StepEvent) {
					handleStepEvent((StepEvent)event);
				} else if (event instanceof WatchpointEvent) {
					handleWatchpointEvent((WatchpointEvent)event);
				} else {
					eventSet.resume();
				}
			}
		}
	}
	
	public void handleVMDeathEvent(VMDeathEvent event) {
		Debugger debugger = Debugger.getInstance();
		String funcName = "HandleJdiEvent";
		String[] args = {"msg", "process terminated."};
		VjdeUtil.callVimFunc(debugger.getVimServerName(), funcName, args);
	}
	
	public void handleVMDisconnectEvent(VMDisconnectEvent event) {
		Debugger debugger = Debugger.getInstance();
		String funcName = "HandleJdiEvent";
		String[] args = {"msg", "process disconnected."};
		VjdeUtil.callVimFunc(debugger.getVimServerName(), funcName, args);
	}
	
	private void handleVMStartEvent(VMStartEvent event) {
		
		ExceptionPointManager expm = ExceptionPointManager.getInstance();
		expm.tryCreateExceptionRequest();
		
		eventSet.resume();
	}
	
	private void handleClassPrepareEvent(ClassPrepareEvent event) {
		ClassPrepareEvent classPrepareEvent = (ClassPrepareEvent) event;
		String mainClassName = classPrepareEvent.referenceType().name();
		BreakpointManager bpm = BreakpointManager.getInstance();
		bpm.tryCreateBreakpointRequest(mainClassName);
		
		event.thread().resume();
	}
	
	private void handleExceptionEvent(ExceptionEvent event) {
		handleSuspendLocatableEvent(event);
	}
	
	private void handleBreakpointEvent(BreakpointEvent event) {
		ThreadReference threadRef = event.thread();
		ReferenceType refType = event.location().declaringType();
		SuspendThreadStack threadStack = SuspendThreadStack.getInstance();
		threadStack.setCurRefType(refType);
		threadStack.setCurThreadRef(threadRef);
		
		BreakpointManager bpm = BreakpointManager.getInstance();
		Breakpoint breakpoint = null;
		
		/*
		List<Breakpoint> allBreakpoints = bpm.getAllBreakpoints();
		Location loc = event.location();
		String className = loc.declaringType().name();
		int lineNum = loc.lineNumber();
		for (Breakpoint bp : allBreakpoints) {
			if (bp.getMainClass().equals(className) && bp.getLineNum() == lineNum) {
				breakpoint = bp;
				break;
			}
		}
		*/
		
		Object tmp = ((BreakpointRequest)event.request()).getProperty("breakpointObj");
		if (tmp != null) breakpoint = (Breakpoint)tmp;
		
		if (breakpoint !=null && breakpoint.getConExp() != null) {
			Object obj =ExpEval.eval(breakpoint.getConExp());
			if (obj ==null || !obj.equals("true")) {
				event.thread().resume();
				return; 
			}
			
		}
		handleSuspendLocatableEvent(event);
		
		//remove temporary breakpoint
		if (breakpoint !=null && breakpoint.isTemp() == true) {
			bpm.removeBreakpoint(breakpoint.getMainClass(), breakpoint.getLineNum());
		}
	}
 
	private void handleStepEvent(StepEvent event) {
		handleSuspendLocatableEvent(event);
	}
	
	private void handleWatchpointEvent(WatchpointEvent event) {
		handleSuspendLocatableEvent(event);
	}
	
	private void handleSuspendLocatableEvent(LocatableEvent event) {
		ThreadReference threadRef = event.thread();
		ReferenceType refType = event.location().declaringType();
		SuspendThreadStack threadStack = SuspendThreadStack.getInstance();
		threadStack.setCurRefType(refType);
		threadStack.setCurThreadRef(threadRef);
		
		Location loc = event.location();
		Debugger debugger = Debugger.getInstance();
		String className = loc.declaringType().name();
		int lineNum = loc.lineNumber();

		CompilerContext ctx = debugger.getCompilerContext();
		String abPath = "None";
		try {
			abPath = ctx.findSourceFile(loc.sourcePath());
		} catch (Throwable e) {
		}
		
		String funcName = "HandleJdiEvent";
		String[] args = {"suspend", abPath, String.valueOf(lineNum), className };
		VjdeUtil.callVimFunc(debugger.getVimServerName(), funcName, args);
	}

}
