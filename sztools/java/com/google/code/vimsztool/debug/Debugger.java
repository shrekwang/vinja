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
	
	public String launch(String mainClass,String classPathXml) {
		vm = ConnectorManager.launch(mainClass, classPathXml);
	    // try create class prepare request for later
	    //create breakpoint request
	    Process process = vm.process();
	    BreakpointManager bm = BreakpointManager.getInstance();
	    bm.tryCreatePrepareRequest();
	    
		eventHandler = new EventHandler(vm);
		eventHandler.start();
		
	    StreamRedirector outRedirector = new StreamRedirector(process.getInputStream(), getVimServerName());
	    StreamRedirector errRedirector = new StreamRedirector(process.getErrorStream(), getVimServerName());
	    outRedirector.start();
	    errRedirector.start();
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
		if (vm!=null )  return true;
		return false;
	}
	public void setVimServerName(String vimServerName) {
		this.vimServerName = vimServerName;
	}
	public String getVimServerName() {
		return vimServerName;
	}

}
