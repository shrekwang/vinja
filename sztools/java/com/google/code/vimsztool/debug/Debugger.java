package com.google.code.vimsztool.debug;

import java.util.ArrayList;
import java.util.List;

import com.google.code.vimsztool.util.VjdeUtil;
import com.sun.jdi.IncompatibleThreadStateException;
import com.sun.jdi.Location;
import com.sun.jdi.ReferenceType;
import com.sun.jdi.StackFrame;
import com.sun.jdi.ThreadReference;
import com.sun.jdi.VirtualMachine;
import com.sun.jdi.request.BreakpointRequest;

public class Debugger {

	private static Debugger instance = new Debugger();

	private VirtualMachine vm = null;
	private EventHandler eventHandler = null;
	private String vimServerName = "";

	private Debugger() {
	}

	public static Debugger getInstance() {
		return instance;
	}

	private void startProcess() {
		Process process = vm.process();
		BreakpointManager bm = BreakpointManager.getInstance();
		bm.tryCreatePrepareRequest();

		eventHandler = new EventHandler(vm);
		eventHandler.start();

		if (process !=null) {
			StreamRedirector outRedirector = new StreamRedirector(process .getInputStream(), getVimServerName());
			StreamRedirector errRedirector = new StreamRedirector(process .getErrorStream(), getVimServerName());
			outRedirector.start();
			errRedirector.start();
		}
	}

	public String launch(String mainClass, String classPathXml) {
		vm = ConnectorManager.launch(mainClass, classPathXml);
		startProcess();
		return "";
	}

	public String attach(String port) {
		vm = ConnectorManager.attach(port);
		if (vm == null)
			return "attach to port fails.";
		startProcess();
		BreakpointManager bpm = BreakpointManager.getInstance();
		bpm.tryCreateBreakpointRequest();
		
		ExceptionPointManager expm = ExceptionPointManager.getInstance();
		expm.tryCreateExceptionRequest();
		
		return "";
	}
	
	public String listBreakpoints() {
		List<BreakpointRequest> bps = vm.eventRequestManager().breakpointRequests();
		StringBuilder sb = new StringBuilder();
		for (BreakpointRequest bp : bps ) {
			Location loc = bp.location();
			String className = loc.declaringType().name();
			if (className.indexOf(".") > -1 ) {
				className = className.substring(className.lastIndexOf(".")+1);
			}
			sb.append(className).append(" [line: ");
			sb.append(loc.lineNumber()).append("] - ");
			sb.append(loc.method()).append("()");
			sb.append("\n");
		}
		
		return sb.toString();
	}
	
	private List<String> getStackFrameInfos(ThreadReference threadRef) {
		List<String> result = new ArrayList<String>();
		try {
			for (StackFrame frame : threadRef.frames()) {
				Location loc = frame.location();
				String name = loc.declaringType().name() + "."
						+ loc.method().name();
				result.add(name + " line: " + loc.lineNumber());
			}
		} catch (Throwable e) {
		}
		return result;
	}
	
	public String listThreads() {
		if (vm == null)
			return "";
		List<ThreadReference> threads = vm.allThreads();
		StringBuilder sb = new StringBuilder(vm.name());
		sb.append("\n");
		for (ThreadReference ref : threads) {
			try {
				sb.append("\tThread [" + ref.name() + "] ( ");
				String status = getThreadStatusName(ref.status());
				if (ref.isSuspended()
						&& ref.status() == ThreadReference.THREAD_STATUS_RUNNING) {
					status = "SUSPENDED";
				}
				sb.append(status);
				
				if (ref.isAtBreakpoint()) {
					Location loc = ref.frame(0).location();
					sb.append("(breakpoint at line ");
					sb.append(loc.lineNumber());
					sb.append(" in ").append(loc.declaringType().name());
					sb.append(") ");
				}
				sb.append(")").append(" uniqueId : ").append(ref.uniqueID());
				sb.append("\n");
				
				if (ref.isSuspended()) {
					List<String> stackInfos = getStackFrameInfos(ref);
					for (String info : stackInfos) {
						sb.append("\t\t").append(info).append("\n");
					}
				}
			} catch (Throwable e) {
			}
		}
		return sb.toString();
	}

	public String resume() {
		SuspendThreadStack threadStack = SuspendThreadStack.getInstance();
		ThreadReference threadRef = threadStack.getCurThreadRef();
		threadRef.resume();
		return "";
	}
	
	public String changeCurrentThread(String uniqueId) {
		List<ThreadReference> threads = vm.allThreads();
		ThreadReference correctRef = null;
		for (ThreadReference ref : threads) {
			if (String.valueOf(ref.uniqueID()).equals(uniqueId)) {
				correctRef = ref;
				break;
			}
		}
		if (correctRef == null) return "no suspend thread";
		SuspendThreadStack threadStack = SuspendThreadStack.getInstance();
		ReferenceType refType;
		try {
			Location loc = correctRef.frame(0).location();
			refType = loc.declaringType();
			threadStack.setCurRefType(refType);
			threadStack.setCurThreadRef(correctRef);
			String className = refType.name();
			int lineNum = loc.lineNumber();
			String[] cmdLine = {"HandleJdiEvent" ,"suspend" , className, String.valueOf(lineNum)};
			VjdeUtil.runVimCmd(getVimServerName(), cmdLine);
			return "success";
		} catch (IncompatibleThreadStateException e) {
			return "error:" + e.getMessage();
		}
	}
	


	public void exit() {
		if (vm == null ) return;
		try {
			vm.dispose();
		} catch (Throwable e) {
		} 
		vm = null;
	}
	public void shutdown() {
		if (vm == null ) return;
		try {
			vm.exit(-1);
		} catch (Throwable e) {
		} 
		vm = null;
	}
	
	public VirtualMachine getVm() {
		return vm;
	}

	public void setVimServerName(String vimServerName) {
		this.vimServerName = vimServerName;
	}

	public String getVimServerName() {
		return vimServerName;
	}

	
	private String getThreadStatusName(int status) {
		String name = "(unknown)";
		switch (status) {
		case ThreadReference.THREAD_STATUS_MONITOR:
			name = "MONITOR";
			break;
		case ThreadReference.THREAD_STATUS_NOT_STARTED:
			name = "NOT STARTED";
			break;
		case ThreadReference.THREAD_STATUS_RUNNING:
			name = "RUNNING";
			break;
		case ThreadReference.THREAD_STATUS_SLEEPING:
			name = "SLEEPING";
			break;
		case ThreadReference.THREAD_STATUS_UNKNOWN:
			name = "UNKNOWN";
			break;
		case ThreadReference.THREAD_STATUS_WAIT:
			name = "WAIT";
			break;
		case ThreadReference.THREAD_STATUS_ZOMBIE:
			name = "ZOMBIE";
			break;
		}
		return name;
	}
}
	 
