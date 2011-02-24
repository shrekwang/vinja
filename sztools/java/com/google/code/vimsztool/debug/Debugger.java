package com.google.code.vimsztool.debug;

import java.util.List;

import com.sun.jdi.Location;
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
		return "";
	}
	
	public String listBreakpoints() {
		List<BreakpointRequest> bps = vm.eventRequestManager().breakpointRequests();
		StringBuilder sb = new StringBuilder();
		for (BreakpointRequest bp : bps ) {
			Location loc = bp.location();
			String className = loc.declaringType().name();
			sb.append(className).append(" [line: ");
			sb.append(loc.lineNumber()).append("] - ");
			sb.append(loc.method()).append("()");
			sb.append("\n");
		}
		
		return sb.toString();
	}

	public String resume() {
		SuspendThreadStack threadStack = SuspendThreadStack.getInstance();
		ThreadReference threadRef = threadStack.getCurThreadRef();
		threadRef.resume();
		return "";
	}

	public String eval(String exp) {
		return "";
	}

	public void exit() {
		destoryVm();

	}
	public void shutdown() {
		destoryVm();
	}
	
	private void destoryVm() {
		if (vm == null ) return;
		try {
			vm.dispose();
		} catch (Throwable e) {
		} 
		vm = null;
	}

	public VirtualMachine getVm() {
		return vm;
	}

	public boolean isRunning() {
		if (vm != null)
			return true;
		return false;
	}

	public void setVimServerName(String vimServerName) {
		this.vimServerName = vimServerName;
	}

	public String getVimServerName() {
		return vimServerName;
	}

}
