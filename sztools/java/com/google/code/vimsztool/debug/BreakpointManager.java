package com.google.code.vimsztool.debug;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

	private Debugger debugger;
	private List<Breakpoint> allBreakpoints = new ArrayList<Breakpoint>();

	public BreakpointManager(Debugger debugger) {
		this.debugger = debugger;
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
		if (metaInfo == null) return false;
		if (metaInfo.getLineNums().contains(lineNum)) return true;
		for (String innerClassName : metaInfo.getInnerClasses()) {
			ClassInfo innerClass = cmm.getMetaInfo(innerClassName);
			if (innerClass !=null && innerClass.getLineNums().contains(lineNum)) return true;
		}
		return false;
	}
	
	public String addTempBreakpoint(String mainClass, int lineNum,boolean resumeThread) {
		ThreadReference threadRef = null;
		if (resumeThread ) {
			debugger.checkVm();
			debugger.checkSuspendThread();
			SuspendThreadStack threadStack = debugger.getSuspendThreadStack();
			threadRef = threadStack.getCurThreadRef();
		}
		
		String result =this.addBreakpoint(mainClass, lineNum, null,true);
		if (result.indexOf(Debugger.CMD_SUCCESS) < 0 ) return result;
		
		if (resumeThread ) threadRef.resume();
		return Debugger.CMD_SUCCESS + ":add temp breakpoint at class \"" + mainClass + "\", line " + lineNum ;
	}
	
	public String addWatchpoint(String mainClass, String fieldName, int accessMode) {
		CompilerContext ctx = debugger.getCompilerContext();
		ClassMetaInfoManager cmm = ctx.getClassMetaInfoManager();
		
		ClassInfo metaInfo = cmm.getMetaInfo(mainClass);
		if (metaInfo == null) return "failure";
		for (Breakpoint bp : allBreakpoints) {
			if (bp.getKind() == Breakpoint.Kind.WATCH_POINT
					&& bp.getMainClass().equals(mainClass) 
					&& bp.getField().equals(fieldName)
					&& bp.getAccessMode() == accessMode) {
				
				return "watchpoint at class \"" + mainClass + "\", field " + fieldName+ "already exists";
			}
		}
		
		Breakpoint breakpoint = new Breakpoint(mainClass, fieldName);
		breakpoint.setAccessMode(accessMode);
		allBreakpoints.add(breakpoint);
		tryCreateRequest(breakpoint);
		return Debugger.CMD_SUCCESS + ": add watchpoint at class \"" + mainClass + "\", field " + fieldName;
		
	}
	
	public String addBreakpoint(String mainClass, int lineNum,String conExp) {
		return this.addBreakpoint(mainClass, lineNum, conExp,false);
	}
	
	public String addBreakpoint(String cmdLine) {
		Pattern pat = Pattern.compile("^(.*)\\s+(\\d+)\\s+(.*?)(\\s+if\\s+\\[.*?\\])?(\\s+do\\s+\\[.*?\\])?");
        Matcher matcher = pat.matcher(cmdLine);
		if (! matcher.matches()) return "parse do command error";
		
		int lineNum = Integer.parseInt(matcher.group(2));
		String mainClass = matcher.group(3);
		
		Breakpoint breakpoint = createBreakpoint(mainClass, lineNum);
		if (breakpoint == null) {
			return "no source line " + lineNum +" in class \"" + mainClass+"\"";
		}
		
		String ifClause = matcher.group(4);
		if (ifClause !=null) {
			ifClause = ifClause.trim();
			String condition = ifClause.substring(ifClause.indexOf("[")+1, ifClause.length()-1);
			breakpoint.setConExp(condition);
		}
		String doClause = matcher.group(5);
		if (doClause != null) {
			doClause =doClause.trim();
			String[] cmds = doClause.substring(doClause.indexOf("[")+1, doClause.length()-1).split(";");
			for (String cmd: cmds) {
				cmd = cmd.trim();
				if (cmd.equals("")) continue;
				breakpoint.addAutoCmd(cmd);
			}
		}
		
		tryCreateRequest(breakpoint);
		return Debugger.CMD_SUCCESS + ": add breakpoint at class \"" + mainClass + "\", line " + lineNum;
	}

	public String addBreakpoint(String mainClass, int lineNum,String conExp,boolean temp) {
		
		Breakpoint breakpoint = createBreakpoint(mainClass, lineNum);
		if (breakpoint == null) {
			return "no source line " + lineNum +" in class \"" + mainClass+"\"";
		}
		breakpoint.setConExp(conExp);
		breakpoint.setTemp(temp);
		
		tryCreateRequest(breakpoint);
		return Debugger.CMD_SUCCESS + ": add breakpoint at class \"" + mainClass + "\", line " + lineNum;
	}
	
	private Breakpoint createBreakpoint(String mainClass, int lineNum) {
		
		Breakpoint breakpoint = null;
		for (Breakpoint bp : allBreakpoints) {
			if (bp.getKind() == Breakpoint.Kind.WATCH_POINT) continue;
			if (bp.getMainClass().equals(mainClass) && bp.getLineNum() == lineNum) {
				breakpoint = bp;
				break;
			}
		}
		if (breakpoint != null) return breakpoint;
		
		CompilerContext ctx = debugger.getCompilerContext();
		ClassMetaInfoManager cmm = ctx.getClassMetaInfoManager();
		
		ClassInfo metaInfo = cmm.getMetaInfo(mainClass);
		if (metaInfo == null) return null;
		
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
		if (breakpoint != null) {
			allBreakpoints.add(breakpoint);
		}
		return breakpoint;
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
			return Debugger.CMD_SUCCESS + ": remove breakpoint at class \"" + mainClass + "\", line " + lineNum;
		}

		return Debugger.CMD_SUCCESS + ": breakpoint at class \"" + mainClass + "\", line " + lineNum + " not exists.";
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
			return Debugger.CMD_SUCCESS + ": remove watchpoint at class \"" + mainClass + "\", field " + field;
		}
		return Debugger.CMD_SUCCESS + ": watchpoint at class \"" + mainClass + "\", field " + field + " not exists.";
	}

	public void tryCreatePrepareRequest() {
		
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
			if ( (breakpoint.getAccessMode() & Breakpoint.ACCESS_READ) ==  Breakpoint.ACCESS_READ ) {
				WatchpointRequest request = vm.eventRequestManager().createAccessWatchpointRequest(field);
				request.setSuspendPolicy(EventRequest.SUSPEND_EVENT_THREAD);
				request.setEnabled(true);
				request.putProperty("breakpointObj", breakpoint);
				breakpoint.addRequest(request);
			}
			if ( (breakpoint.getAccessMode() & Breakpoint.ACCESS_WRITE) ==  Breakpoint.ACCESS_WRITE ) {
				WatchpointRequest request = vm.eventRequestManager().createModificationWatchpointRequest(field);
				request.setSuspendPolicy(EventRequest.SUSPEND_EVENT_THREAD);
				request.setEnabled(true);
				request.putProperty("breakpointObj", breakpoint);
				breakpoint.addRequest(request);
			}
		}

	}

}
