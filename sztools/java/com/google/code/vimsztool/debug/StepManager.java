package com.google.code.vimsztool.debug;

import java.util.List;

import com.sun.jdi.ThreadReference;
import com.sun.jdi.VirtualMachine;
import com.sun.jdi.request.EventRequest;
import com.sun.jdi.request.EventRequestManager;
import com.sun.jdi.request.StepRequest;

public class StepManager {

	public static String step(int stepDepth, int count) {
		Debugger debugger = Debugger.getInstance();
		VirtualMachine vm = debugger.getVm();
		SuspendThreadStack threadStack = SuspendThreadStack.getInstance();
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

		StepRequest request = mgr.createStepRequest(threadRef, StepRequest.STEP_LINE, stepDepth);
		List<String> excludeFilters = StepFilterConfiger.getDefaultFilter(); 
		for (String filter : excludeFilters) {
			request.addClassExclusionFilter(filter);
		}
		request.addCountFilter(count);
		request.setSuspendPolicy(EventRequest.SUSPEND_EVENT_THREAD);
		request.enable();
		
		threadRef.resume();
		threadStack.clean();
		return "";
	}

}
