package com.google.code.vimsztool.debug;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import com.sun.jdi.AbsentInformationException;
import com.sun.jdi.Location;
import com.sun.jdi.ReferenceType;
import com.sun.jdi.VirtualMachine;
import com.sun.jdi.request.BreakpointRequest;
import com.sun.jdi.request.ClassPrepareRequest;
import com.sun.jdi.request.EventRequest;
import com.sun.jdi.request.EventRequestManager;

public class BreakpointManager {

	private List<Breakpoint> allBreakpoints = new ArrayList<Breakpoint>();
	private static BreakpointManager instance = new BreakpointManager();

	private BreakpointManager() {
	}

	public static BreakpointManager getInstance() {
		return instance;
	}

	public String addBreakpoint(String mainClass, int lineNum) {
		Breakpoint breakpoint = new Breakpoint(mainClass, lineNum);
		allBreakpoints.add(breakpoint);
		tryCreateBreakpointRequest(breakpoint);
		return "success";
	}

	public String removeBreakpoint(String mainClass, int lineNum) {

		Breakpoint breakpoint = null;
		for (Breakpoint bp : allBreakpoints) {
			if (bp.getMainClass().equals(mainClass) && bp.getLineNum() == lineNum) {
				breakpoint = bp;
				break;
			}
		}
		if (breakpoint != null) {
			tryRemoveBreakpointRequest(breakpoint);
			allBreakpoints.remove(breakpoint);
		}
		return "success";

	}

	public void tryCreatePrepareRequest() {
		
		Debugger debugger = Debugger.getInstance();
		VirtualMachine vm = debugger.getVm();
		if (vm==null) return ;
		EventRequestManager erm = vm.eventRequestManager();
		if (erm == null) return;
		
		HashSet<String> names = new HashSet<String>();
		
		for (Breakpoint bp : allBreakpoints) {
			if (names.contains(bp.getMainClass())) continue;
			names.add(bp.getMainClass());
			ClassPrepareRequest classPrepareRequest = erm.createClassPrepareRequest();
			classPrepareRequest.addClassFilter(bp.getMainClass()+"*");
			classPrepareRequest.addCountFilter(1);
			classPrepareRequest.setSuspendPolicy(EventRequest.SUSPEND_EVENT_THREAD);
			classPrepareRequest.enable();
		}

	}

	public void tryRemoveBreakpointRequest(Breakpoint breakpoint) {
		
		Debugger debugger = Debugger.getInstance();
		VirtualMachine vm = debugger.getVm();
		if (vm == null)
			return;
		List<BreakpointRequest> requests = breakpoint.getRequests();
		if (requests == null || requests.size() == 0)
			return;
		for (BreakpointRequest bp : requests) {
			try {
				vm.eventRequestManager().deleteEventRequest(bp);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

	}
	
	public void tryCreateBreakpointRequest() {
		for (Breakpoint bp : allBreakpoints) {
			tryCreateBreakpointRequest(bp);
		}
	}
	
	public void tryCreateBreakpointRequest(String className) {
		for (Breakpoint bp : allBreakpoints) {
			if (bp.getMainClass().equals(className)) {
				tryCreateBreakpointRequest(bp);
			}
		}
	}

	public void tryCreateBreakpointRequest(Breakpoint breakpoint) {

		String className = breakpoint.getMainClass();
		int lineNum = breakpoint.getLineNum();
		
		Debugger debugger = Debugger.getInstance();
		VirtualMachine vm = debugger.getVm();
		if (vm == null)
			return;
		List<ReferenceType> refTypes = vm.classesByName(className);
		if (refTypes == null)
			return;

		for (int i = 0; i < refTypes.size(); i++) {
			ReferenceType rt = refTypes.get(i);
			if (!rt.isPrepared()) {
				continue;
			}
			List<Location> lines;

			try {
				lines = rt.locationsOfLine(lineNum);
				if (lines.size() == 0) {
					continue;
				}
				Location loc = lines.get(0);
				BreakpointRequest request = vm.eventRequestManager().createBreakpointRequest(loc);
				request.setSuspendPolicy(EventRequest.SUSPEND_EVENT_THREAD);
				request.setEnabled(true);
				breakpoint.addRequest(request);
			} catch (AbsentInformationException e) {
				e.printStackTrace();
			}
		}

	}

}
