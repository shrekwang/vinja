package com.google.code.vimsztool.debug;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.sun.jdi.ReferenceType;
import com.sun.jdi.VirtualMachine;
import com.sun.jdi.request.EventRequestManager;
import com.sun.jdi.request.ExceptionRequest;

public class ExceptionPointManager {
	
	private List<String> exceptions = new ArrayList<String>();
	private Map<String, ExceptionRequest> requests = new HashMap<String,ExceptionRequest>();
	
	private static ExceptionPointManager instance = new ExceptionPointManager();

	private ExceptionPointManager() {
	}

	public static ExceptionPointManager getInstance() {
		return instance;
	}
	
	public String addExceptionPoint(String className) {
		exceptions.add(className);
		tryCreateExceptionRequest(className);
		return "success";
	}
	
	public void tryCreateExceptionRequest(String className) {
		Debugger debugger = Debugger.getInstance();
		VirtualMachine vm = debugger.getVm();
		if (vm==null) return ;
		EventRequestManager erm = vm.eventRequestManager();
		if (erm == null) return;
		
		List<ReferenceType> refTypes = vm.classesByName(className);
		ExceptionRequest exReq = vm.eventRequestManager().createExceptionRequest(refTypes.get(0), true, true);
		requests.put(className, exReq);
		
	}
	

	public String removeExceptionRequest(String className) {
		tryRemoveExceptionRequest(className);
		exceptions.remove(className);
		return "success";
	}
	
	public void tryRemoveExceptionRequest(String className) {
		Debugger debugger = Debugger.getInstance();
		VirtualMachine vm = debugger.getVm();
		if (vm==null) return ;
		EventRequestManager erm = vm.eventRequestManager();
		if (erm == null) return;
		
		ExceptionRequest exReq = requests.get(className);
		vm.eventRequestManager().deleteEventRequest(exReq);
	}
	
	public void tryCreateExceptionRequest() {
		for (String className : exceptions) {
			tryCreateExceptionRequest(className);
		}
	}
	
}
