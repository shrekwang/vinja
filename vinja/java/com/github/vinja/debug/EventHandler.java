package com.github.vinja.debug;

import com.github.vinja.compiler.CompilerContext;
import com.github.vinja.util.VjdeUtil;
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

public class EventHandler extends Thread {

	private Debugger debugger;
	private VirtualMachine vm;
	private EventQueue eventQueue;
	private EventSet eventSet;
	private boolean vmExit = false;

	public EventHandler(Debugger  debugger) {
		this.debugger = debugger;
		this.vm =debugger.getVm();
	}

	public void run() {
		eventQueue = vm.eventQueue();
		while (true) {
			if (vmExit == true) {
				debugger.disconnectOrExit();
				break;
			}
			try {
				eventSet = eventQueue.remove();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			EventIterator eventIterator = eventSet.eventIterator();
			boolean suspendEventHandled = false;
			while (eventIterator.hasNext()) {
				Event event = (Event) eventIterator.next();
				if (event instanceof VMStartEvent) {
					handleVMStartEvent((VMStartEvent)event);
				} 
				else if (event instanceof ClassPrepareEvent) {
					handleClassPrepareEvent((ClassPrepareEvent)event);
				} 
				else if (event instanceof StepEvent) {
					handleStepEvent((StepEvent)event);
					suspendEventHandled = true;
				} 
				else if (event instanceof BreakpointEvent) {
					if (!suspendEventHandled) handleBreakpointEvent((BreakpointEvent)event);
				} 
				else if (event instanceof VMDisconnectEvent) {
					handleVMDisconnectEvent((VMDisconnectEvent)event);
					vmExit = true;
				} 
				else if (event instanceof VMDeathEvent) {
					handleVMDeathEvent((VMDeathEvent)event);
					vmExit = true;
				} 
				else if (event instanceof ExceptionEvent) {
					handleExceptionEvent((ExceptionEvent)event);
				} 
				else if (event instanceof WatchpointEvent) {
					if (!suspendEventHandled) handleWatchpointEvent((WatchpointEvent)event);
				} 
				else {
					eventSet.resume();
				}
			}
		}
	}
	
	public void handleVMDeathEvent(VMDeathEvent event) {
		debugger.clean();
		String funcName = "HandleJdiEvent";
		String[] args = {"msg", "process terminated."};
		VjdeUtil.callVimFunc(debugger.getVimServerName(), funcName, args);
	}
	
	public void handleVMDisconnectEvent(VMDisconnectEvent event) {
		debugger.clean();
		String funcName = "HandleJdiEvent";
		String[] args = {"msg", "process disconnected."};
		VjdeUtil.callVimFunc(debugger.getVimServerName(), funcName, args);
	}
	
	private void handleVMStartEvent(VMStartEvent event) {
		
		ExceptionPointManager expm = debugger.getExceptionPointManager();
		expm.tryCreateExceptionRequest();
		eventSet.resume();
	}
	
	private void handleClassPrepareEvent(ClassPrepareEvent event) {
		ClassPrepareEvent classPrepareEvent = (ClassPrepareEvent) event;
		String mainClassName = classPrepareEvent.referenceType().name();
		BreakpointManager bpm = debugger.getBreakpointManager();
		bpm.tryCreateBreakpointRequest(mainClassName);
		
		event.thread().resume();
	}
	
	private void handleExceptionEvent(ExceptionEvent event) {
		handleSuspendLocatableEvent(event);
	}
	
	private void handleBreakpointEvent(BreakpointEvent event) {
		
		
		BreakpointManager bpm = debugger.getBreakpointManager();
		Breakpoint breakpoint = null;
		
		Object tmp = ((BreakpointRequest)event.request()).getProperty("breakpointObj");
		if (tmp != null) breakpoint = (Breakpoint)tmp;
		
		if (breakpoint !=null && breakpoint.getConExp() != null) {
			
			SuspendThreadStack threadStack = debugger.getSuspendThreadStack();
			ReferenceType oldRefType = threadStack.getCurRefType();
			ThreadReference oldThreadRef = threadStack.getCurThreadRef();
			
			threadStack.setCurRefType(event.location().declaringType());
			threadStack.setCurThreadRef(event.thread());
			
			Object obj = debugger.getExpEval().evalSimpleValue(breakpoint.getConExp());
			
			//restore threadStack for handleSuspendLocatableEvent() call
			threadStack.setCurRefType(oldRefType);
			threadStack.setCurThreadRef(oldThreadRef);
			
			if (obj ==null || !obj.equals("true")) {
				event.thread().resume();
				return; 
			}
			
			
		}
		handleSuspendLocatableEvent(event);
		if (breakpoint != null && breakpoint.getAutoCmds().size()>0 ) {
			StringBuilder resultSb = new StringBuilder("\nBreakpoint Autocmds:\n");
			DebugCommand debugCommand = new DebugCommand();
			for (String cmd : breakpoint.getAutoCmds()) {
				try {
					String line = debugCommand.execute(debugger, debugger.getClassPathXml(), cmd);
					if (!line.endsWith("\n")) line = line + "\n";
					resultSb.append("   ").append(line);
				} catch (Throwable e) {
					resultSb.append("execute " + cmd + " error:" + e.getMessage()+" \n");
				}
			}
			debugger.setAutoCmdResult(resultSb.toString());
			String funcName = "FetchAutocmdResult";
			VjdeUtil.callVimFunc(debugger.getVimServerName(), funcName, null);
		}
		
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
		SuspendThreadStack threadStack = debugger.getSuspendThreadStack();
		
		//already suspend at some point
		if (threadStack.hasSuspendedThread()) return;
		
		threadStack.setCurRefType(refType);
		threadStack.setCurThreadRef(threadRef);
		
		Location loc = event.location();
		String className = loc.declaringType().name();
		int lineNum = loc.lineNumber();

		CompilerContext ctx = debugger.getCompilerContext();
		String abPath = "None";
		try {
			String locClassName = loc.sourcePath().replace("/",".").replace(".java","");
			abPath = ctx.findSourceOrBinPath(locClassName);
		} catch (Throwable e) {
		}
		
		String appendOperate  = "null";
		if (event.request() instanceof BreakpointRequest) {
			Object tmp = ((BreakpointRequest)event.request()).getProperty("breakpointObj");
			if (tmp != null && ((Breakpoint)tmp).isTemp() )  {
				appendOperate = "remove_breakpoint";
			}
		}
		String funcName = "HandleJdiEvent";
		String[] args = {"suspend", abPath, String.valueOf(lineNum), className,appendOperate };
		VjdeUtil.callVimFunc(debugger.getVimServerName(), funcName, args);
	}

}
