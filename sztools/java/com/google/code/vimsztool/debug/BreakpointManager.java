package com.google.code.vimsztool.debug;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import com.google.code.vimsztool.compiler.CompilerContext;
import com.google.code.vimsztool.omni.ClassInfo;
import com.google.code.vimsztool.omni.ClassMetaInfoManager;
import com.sun.jdi.AbsentInformationException;
import com.sun.jdi.Field;
import com.sun.jdi.Location;
import com.sun.jdi.ReferenceType;
import com.sun.jdi.ThreadReference;
import com.sun.jdi.VirtualMachine;
import com.sun.jdi.request.BreakpointRequest;
import com.sun.jdi.request.ClassPrepareRequest;
import com.sun.jdi.request.EventRequest;
import com.sun.jdi.request.EventRequestManager;
import com.sun.jdi.request.WatchpointRequest;

public class BreakpointManager {

	private List<Breakpoint> allBreakpoints = new ArrayList<Breakpoint>();
	private static BreakpointManager instance = new BreakpointManager();

	private BreakpointManager() {
	}

	public static BreakpointManager getInstance() {
		return instance;
	}
	
	public List<Breakpoint> getAllBreakpoints() {
		return allBreakpoints;
	}
	
	public String allBreakpointsInfo() {
		StringBuilder sb = new StringBuilder();
		for (Breakpoint bp : allBreakpoints) {
			sb.append(bp.getMainClass()).append(" ").append(bp.getLineNum());
			sb.append("\n");
		}
		return sb.toString();
	}
	
	public void verifyBreakpoint(String mainClass) {
		Debugger debugger = Debugger.getInstance();
		CompilerContext ctx = debugger.getCompilerContext();
		if (ctx == null) return ;
		
		ClassMetaInfoManager cmm =ctx.getClassMetaInfoManager();
		
		List<Breakpoint> invalidBreakPoints = new ArrayList<Breakpoint>();
		
		for (Breakpoint bp : allBreakpoints) {
			if (bp.getMainClass().equals(mainClass)) {
				if (!classContainsLineNum(cmm, mainClass, bp.getLineNum())) {
					invalidBreakPoints.add(bp);
				}
			}
		}
		allBreakpoints.removeAll(invalidBreakPoints);
		
	}
	
	public boolean classContainsLineNum(ClassMetaInfoManager cmm, String className, int lineNum) {
		ClassInfo metaInfo = cmm.getMetaInfo(className);
		if (metaInfo.getLineNums().contains(lineNum)) return true;
		for (String innerClassName : metaInfo.getInnerClasses()) {
			ClassInfo innerClass = cmm.getMetaInfo(innerClassName);
			if (innerClass !=null && innerClass.getLineNums().contains(lineNum)) return true;
		}
		return false;
	}
	
	public String addTempBreakpoint(String mainClass, int lineNum) {
		SuspendThreadStack threadStack = SuspendThreadStack.getInstance();
		ThreadReference threadRef = threadStack.getCurThreadRef();
		if (threadRef == null ) {
			return "no suspended thread";
		}
		String result =this.addBreakpoint(mainClass, lineNum, null,true);
		if (!result.equals("success")) return result;
		threadRef.resume();
		return "success";
	}
	
	public String addWatchpoint(String mainClass, String fieldName, int accessMode) {
		Debugger debugger = Debugger.getInstance();
		CompilerContext ctx = debugger.getCompilerContext();
		ClassMetaInfoManager cmm = ctx.getClassMetaInfoManager();
		
		ClassInfo metaInfo = cmm.getMetaInfo(mainClass);
		if (metaInfo == null) return "failure";
		Breakpoint breakpoint = null;
		breakpoint = new Breakpoint(mainClass, fieldName);
		allBreakpoints.add(breakpoint);
		tryCreateRequest(breakpoint);
		return "success";
	}
	
	public String addBreakpoint(String mainClass, int lineNum,String conExp) {
		return this.addBreakpoint(mainClass, lineNum, conExp,false);
	}

	public String addBreakpoint(String mainClass, int lineNum,String conExp,boolean temp) {
		Debugger debugger = Debugger.getInstance();
		CompilerContext ctx = debugger.getCompilerContext();
		ClassMetaInfoManager cmm = ctx.getClassMetaInfoManager();
		
		ClassInfo metaInfo = cmm.getMetaInfo(mainClass);
		if (metaInfo == null) return "failure";
		Breakpoint breakpoint = null;
		if (!metaInfo.getLineNums().contains(lineNum)) {
			for (String innerclassName : metaInfo.getInnerClasses()	) {
				ClassInfo innerClassInfo = cmm.getMetaInfo(innerclassName);
				if (innerClassInfo !=null && innerClassInfo.getLineNums().contains(lineNum)) {
					breakpoint = new Breakpoint(mainClass, innerclassName, lineNum);
					break;
				}
			}
		} else {
			breakpoint = new Breakpoint(mainClass, lineNum);
		}
		
		if (breakpoint == null) return "failure";
		breakpoint.setConExp(conExp);
		breakpoint.setTemp(temp);
		
		
		allBreakpoints.add(breakpoint);
		tryCreateRequest(breakpoint);
		return "success";
	}

	public String removeBreakpoint(String mainClass, int lineNum) {

		Breakpoint breakpoint = null;
		for (Breakpoint bp : allBreakpoints) {
			if (bp.getKind() == Breakpoint.Kind.WATCH_POINT) continue;
			if (bp.getMainClass().equals(mainClass) && bp.getLineNum() == lineNum) {
				breakpoint = bp;
				break;
			}
		}
		if (breakpoint != null) {
			tryRemoveRequest(breakpoint);
			allBreakpoints.remove(breakpoint);
		}
		return "success";

	}
	
	public String removeWatchpoint(String mainClass, String field) {
		Breakpoint breakpoint = null;
		for (Breakpoint bp : allBreakpoints) {
			if (bp.getKind() == Breakpoint.Kind.BREAK_POINT) continue;
			if (bp.getMainClass().equals(mainClass) && bp.getField().equals(field)) {
				breakpoint = bp;
				break;
			}
		}
		if (breakpoint != null) {
			tryRemoveRequest(breakpoint);
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
			String className = bp.getMainClass();
			if (bp.getInnerClass() !=null) {
				className = bp.getInnerClass();
			}
			if (names.contains(className)) continue;
			names.add(className);
			ClassPrepareRequest classPrepareRequest = erm.createClassPrepareRequest();
			classPrepareRequest.addClassFilter(className);
			classPrepareRequest.addCountFilter(1);
			classPrepareRequest.setSuspendPolicy(EventRequest.SUSPEND_EVENT_THREAD);
			classPrepareRequest.enable();
		}

	}

	public void tryRemoveRequest(Breakpoint breakpoint) {
		
		Debugger debugger = Debugger.getInstance();
		VirtualMachine vm = debugger.getVm();
		if (vm == null)
			return;
		List<EventRequest> requests = breakpoint.getRequests();
		if (requests == null || requests.size() == 0)
			return;
		for (EventRequest bp : requests) {
			try {
				vm.eventRequestManager().deleteEventRequest(bp);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

	}
	
	public void tryCreateBreakpointRequest() {
		for (Breakpoint bp : allBreakpoints) {
			tryCreateRequest(bp);
		}
	}
	
	public void tryResetBreakpointRequest(String className) {
		for (Breakpoint bp : allBreakpoints) {
			if (bp.getMainClass().equals(className)) {
				tryRemoveRequest(bp);
				tryCreateRequest(bp);
			}
		}
		
	}
	
	public void tryCreateBreakpointRequest(String className) {
	
		for (Breakpoint bp : allBreakpoints) {
			if (bp.getMainClass().equals(className)
					|| (bp.getInnerClass()!=null && bp.getInnerClass().equals(className))) {
				tryCreateRequest(bp);
			}
			
		}
	}
	
	public void tryCreateRequest(Breakpoint breakpoint) {
		if (breakpoint.getKind() == Breakpoint.Kind.WATCH_POINT) {
			_tryCreateWatchpointRequest(breakpoint);
		} else {
			_tryCreateBreakpointRequest(breakpoint);
		}
		
	}

	private void _tryCreateBreakpointRequest(Breakpoint breakpoint) {

		String className = breakpoint.getMainClass();
		if (breakpoint.getInnerClass() !=null) {
			className = breakpoint.getInnerClass();
		}
		
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
				request.putProperty("breakpointObj", breakpoint);
				breakpoint.addRequest(request);
			} catch (AbsentInformationException e) {
				e.printStackTrace();
			}
		}

	}
	
	private void _tryCreateWatchpointRequest(Breakpoint breakpoint) {

		String className = breakpoint.getMainClass();
		if (breakpoint.getInnerClass() !=null) {
			className = breakpoint.getInnerClass();
		}
		
		String fieldName = breakpoint.getField();
		
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

			Field field =rt.fieldByName(fieldName);
			if (field == null ) {
				continue;
			}
			//WatchpointRequest request = vm.eventRequestManager().createAccessWatchpointRequest(field);
			WatchpointRequest request = vm.eventRequestManager().createModificationWatchpointRequest(field);
			request.setSuspendPolicy(EventRequest.SUSPEND_EVENT_THREAD);
			request.setEnabled(true);
			request.putProperty("breakpointObj", breakpoint);
			breakpoint.addRequest(request);
		}

	}

}
