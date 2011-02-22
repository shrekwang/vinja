package com.google.code.vimsztool.debug;

import com.sun.jdi.ThreadReference;
import com.sun.jdi.VirtualMachine;

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
		try {
			vm.dispose();
		} catch (Throwable e) {
		} finally {
			vm = null;
		}
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
