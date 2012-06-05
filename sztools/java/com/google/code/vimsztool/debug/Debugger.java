package com.google.code.vimsztool.debug;

import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.FilenameUtils;

import com.google.code.vimsztool.compiler.CompilerContext;
import com.google.code.vimsztool.compiler.CompilerContextManager;
import com.google.code.vimsztool.util.Preference;
import com.google.code.vimsztool.util.StreamGobbler;
import com.google.code.vimsztool.util.VjdeUtil;
import com.sun.jdi.Bootstrap;
import com.sun.jdi.IncompatibleThreadStateException;
import com.sun.jdi.Location;
import com.sun.jdi.ReferenceType;
import com.sun.jdi.StackFrame;
import com.sun.jdi.ThreadReference;
import com.sun.jdi.VirtualMachine;
import com.sun.jdi.VirtualMachineManager;
import com.sun.jdi.connect.AttachingConnector;
import com.sun.jdi.connect.Connector;
import com.sun.jdi.connect.IllegalConnectorArgumentsException;
import com.sun.jdi.connect.LaunchingConnector;
import com.sun.jdi.connect.ListeningConnector;
import com.sun.jdi.connect.VMStartException;

public class Debugger {

	private static Debugger instance = new Debugger();

	private VirtualMachine vm = null;
	private Process process = null;
	private EventHandler eventHandler = null;
	private String vimServerName = "";
	private CompilerContext compilerContext =null;
	private Preference pref = Preference.getInstance();
	private StringBuffer buffer = new StringBuffer();
	private ScheduledExecutorService exec = null;

	private Debugger() {
	}

	public static Debugger getInstance() {
		return instance;
	}
	
	public synchronized String fetchResult() {
		String result =this.buffer.toString();
		this.buffer.delete(0, buffer.length());
		return result;
	}


	private void startProcess() {
		if (process == null) {
			process = vm.process();
		}
		BreakpointManager bm = BreakpointManager.getInstance();
		bm.tryCreatePrepareRequest();

		eventHandler = new EventHandler(vm);
		eventHandler.start();

		if (process !=null) {
			//StreamRedirector outRedirector = new StreamRedirector(process .getInputStream(), getVimServerName());
			//StreamRedirector errRedirector = new StreamRedirector(process .getErrorStream(), getVimServerName());
			//outRedirector.start();
			//errRedirector.start();
			
			StreamGobbler stdOut=new StreamGobbler(buffer, process.getInputStream());
			StreamGobbler stdErr=new StreamGobbler(buffer, process.getErrorStream());
			stdOut.start();
			stdErr.start();
			exec = Executors.newScheduledThreadPool(1);
	        exec.scheduleAtFixedRate(new BufferChecker(), 1, 70, TimeUnit.MILLISECONDS);
		}
	}
	
	public String launch(String classPathXml, String cmdLine) {
        String[] bb = cmdLine.split("\\s+");
        List<String> opts = new ArrayList<String>();
        List<String> args = new ArrayList<String>();
        boolean beforeMain = true;
        String className = null;
        for (int i=0; i<bb.length; i++) {
            if (!bb[i].startsWith("-")) {
                beforeMain = false;
            }
            if (beforeMain) {
                opts.add(bb[i]);
            } else {
                if (className ==null ) {
                    className = bb[i];
                } else {
                    args.add(bb[i]);
                }
            }
        }
		if (vm != null) 
			return "VirtualMachine is not null";
		vm = launch(className,classPathXml,opts,args);
		startProcess();
		return "";
		
	}

	public String attach(String port) {
		if (vm != null) 
			return "VirtualMachine is not null";
		vm = attachToVm(port);
		if (vm == null)
			return "attach to port fails.";
		startProcess();
		BreakpointManager bpm = BreakpointManager.getInstance();
		bpm.tryCreateBreakpointRequest();
		
		ExceptionPointManager expm = ExceptionPointManager.getInstance();
		expm.tryCreateExceptionRequest();
		
		return "attach to remote vm successd.";
	}
	
	public String listBreakpoints() {
		BreakpointManager bpm = BreakpointManager.getInstance();
		List<Breakpoint> bps = bpm.getAllBreakpoints();
		StringBuilder sb = new StringBuilder();
		for (Breakpoint bp : bps ) {
			String className = bp.getMainClass();
			if (className.indexOf(".") > -1 ) {
				className = className.substring(className.lastIndexOf(".")+1);
			}
			if (bp.getKind() == Breakpoint.Kind.BREAK_POINT) {
				sb.append(className).append(" [line: ");
				sb.append(bp.getLineNum()).append("] - ");
			} else {
				sb.append(className).append(" [field: ");
				sb.append(bp.getField()).append("] - ");
			}
			sb.append("\n");
		}
		
		return sb.toString();
	}
	
	private List<String> getStackFrameInfos(ThreadReference threadRef) {
		List<String> result = new ArrayList<String>();
		SuspendThreadStack threadStack = SuspendThreadStack.getInstance();
		try {
			List<StackFrame> frames = threadRef.frames();
			for (int i=0; i<frames.size(); i++) {
				StackFrame frame = frames.get(i);
				Location loc = frame.location();
				String name = loc.declaringType().name() + "."
						+ loc.method().name();
				String frameInfo = name + " line: " + loc.lineNumber();
				if (i == threadStack.getCurFrame()) {
					frameInfo = frameInfo + "  (current frame) ";
				}
				result.add(frameInfo);
			}
		} catch (Throwable e) {
		}
		return result;
	}
	
	private String getIndentStr(int level) {
		StringBuilder sb = new StringBuilder();
		for (int i=0; i<level; i++) {
			sb.append("   ");
		}
		return sb.toString();
	}
	
	private void appendStackInfo(StringBuilder sb, ThreadReference ref, int startLevel) {
		sb.append(getIndentStr(startLevel));
		sb.append("Thread [" + ref.name() + "] ( ");
		String status = getThreadStatusName(ref.status());
		try {
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
					sb.append(getIndentStr(startLevel+1));
					sb.append(info).append("\n");
				}
			}
		} catch (Throwable e) {
		}
	}
	
	public String listFrames() {
		SuspendThreadStack threadStack = SuspendThreadStack.getInstance();
		ThreadReference threadRef = threadStack.getCurThreadRef();
		StringBuilder sb = new StringBuilder();
		appendStackInfo(sb,threadRef,0);
		return sb.toString();
	}
	
	public String listThreads() {
		if (vm == null)
			return "no virtual machine connected.";
		List<ThreadReference> threads = vm.allThreads();
		StringBuilder sb = new StringBuilder(vm.name());
		sb.append("\n");
		for (ThreadReference ref : threads) {
			appendStackInfo(sb,ref,1);
		}
		return sb.toString();
	}

	public String resume() {
		SuspendThreadStack threadStack = SuspendThreadStack.getInstance();
		ThreadReference threadRef = threadStack.getCurThreadRef();
		threadRef.resume();
		threadStack.clean();
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
			changeVimEditSourceLocaton(loc);
			return "success";
		} catch (IncompatibleThreadStateException e) {
			return "error:" + e.getMessage();
		}
	}
	
	public String changeCurrentFrame(int frameNum) {
		SuspendThreadStack threadStack = SuspendThreadStack.getInstance();
		ThreadReference threadRef = threadStack.getCurThreadRef();
		if (threadRef == null ) {
			return "no suspended thread";
		}
		try {
			Location loc = threadRef.frame(frameNum).location();
			ReferenceType refType= loc.declaringType();
			threadStack.setCurRefType(refType);
			threadStack.setCurFrame(frameNum);
			changeVimEditSourceLocaton(loc);
			return "success";
		} catch (IncompatibleThreadStateException e) {
			return "error" + e.getMessage();
		}
	}
	
	private void changeVimEditSourceLocaton(Location loc) {
		ReferenceType refType = loc.declaringType();
		String className = refType.name();
		int lineNum = loc.lineNumber();
		String abPath = "None";
		try {
			abPath = compilerContext.findSourceFile(loc.sourcePath());
		} catch (Throwable e) {
		}
		String funcName = "HandleJdiEvent";
		String[] args = {"suspend", abPath, String.valueOf(lineNum), className };
		VjdeUtil.callVimFunc(getVimServerName(), funcName, args);
	}
	


	public void disconnectOrExit() {
		if (vm == null ) return;
		try {
			vm.dispose();
		} catch (Throwable e) {
		} 
		vm = null;
		clean();
	}
	
	public void shutdown() {
		if (vm == null ) return;
		try {
			vm.exit(-1);
		} catch (Throwable e) {
		} 
		vm = null;
		clean();
	}
	
	private void clean() {
		SuspendThreadStack suspendThreadStack = SuspendThreadStack.getInstance();
		suspendThreadStack.clean();
		if (this.exec != null) {
			exec.shutdown();
		}
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

	public void setCompilerContext(CompilerContext compilerContext) {
		this.compilerContext = compilerContext;
	}

	public CompilerContext getCompilerContext() {
		return compilerContext;
	}
	
	private String getClassPath(String classPathXml) {
		CompilerContextManager ccm = CompilerContextManager.getInstnace();
		CompilerContext ctx = ccm.getCompilerContext(classPathXml);

		List<URL> urls = ctx.getClassPathUrls();
		StringBuilder sb = new StringBuilder();
		for (URL url : urls) {
			sb.append(url.getPath()).append(File.pathSeparator);
		}
		return sb.toString();
	}
	
	public int getAvailPort() {
		int port = 0;
		try {
			ServerSocket socket = new ServerSocket(0);
			port = socket.getLocalPort();
			socket.close();
		} catch (Exception e) {
		}
		return port;
	}
	
	public String launchTomcat() {
		
		String port = String.valueOf(getAvailPort());
		
		String tomcatHome=pref.getValue(Preference.TOMCAT_HOME);
		StringBuilder cmd = new StringBuilder("java");
		cmd.append(" -agentlib:jdwp=transport=dt_socket,address=localhost:"+port+",suspend=y ");
		cmd.append(" -Dcatalina.base="+tomcatHome);
		cmd.append(" -Dcatalina.home="+tomcatHome);
		cmd.append(" -Djava.io.tmpdir="+FilenameUtils.concat(tomcatHome, "temp"));
		String customOpts = pref.getValue(Preference.TOMCAT_JVMOPTS);
		if (customOpts != null ) {
			cmd.append(" " + customOpts);
		}
		cmd.append(" -cp " + FilenameUtils.concat(tomcatHome, "bin/bootstrap.jar"));
		cmd.append(" org.apache.catalina.startup.Bootstrap  start");
		
		try {
			File workingDir = new File(tomcatHome);
			process = Runtime.getRuntime().exec(cmd.toString(),null,workingDir);
		} catch (Exception err) {
			err.printStackTrace();
		}
		
		ListeningConnector connector = getListeningConnector();
		Map<String, Connector.Argument> connectArgs = connector.defaultArguments();
		Connector.Argument portArg = connectArgs.get("port");
		Connector.Argument hostArg = connectArgs.get("localAddress");
		portArg.setValue(port);
		hostArg.setValue("localhost"); 
		try {
			vm = connector.accept(connectArgs);
			startProcess();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return "";
	}
	
	public VirtualMachine launch(String mainClass, String classPathXml,
			List<String> opts,List<String> args) {
		
		
		String port = String.valueOf(getAvailPort());
		StringBuilder cmd = new StringBuilder("java -agentlib:jdwp=transport=dt_socket,address=localhost:"
			+port+",suspend=y");
		cmd.append(" -cp " + getClassPath(classPathXml));
		cmd.append(" ");
		if (opts !=null && opts.size() > 0) {
			for (String opt : opts ) {
				cmd.append(opt).append(" ");
			}
		}
		cmd.append(mainClass).append(" ");
		
		if (args!=null && args.size() > 0) {
			for (String arg : args ) {
				cmd.append(arg).append(" ");
			}
		}
		
		String projectRoot = new File(classPathXml).getParent();

		try {
			File workingDir = new File(projectRoot);
			process = Runtime.getRuntime().exec(cmd.toString(),null,workingDir);
		} catch (Exception err) {
			err.printStackTrace();
		}
		
		ListeningConnector connector = getListeningConnector();
		Map<String, Connector.Argument> connectArgs = connector.defaultArguments();
		Connector.Argument portArg = connectArgs.get("port");
		Connector.Argument hostArg = connectArgs.get("localAddress");
		portArg.setValue(port);
		hostArg.setValue("localhost"); 
		try {
			return connector.accept(connectArgs);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	public VirtualMachine launch2(String mainClass, String classPathXml,
			List<String> opts,List<String> args) {
		LaunchingConnector launchingConnector = Bootstrap
				.virtualMachineManager().defaultConnector();

		Map<String, Connector.Argument> defaultArguments = launchingConnector
				.defaultArguments();

		Connector.Argument mainArg = defaultArguments.get("main");
		StringBuilder mainSb = new StringBuilder(mainClass);
		
		if (args != null & args.size() > 0 ) {
			mainSb.append(" ");
			for (String arg : args) {
				mainSb.append(arg).append(" ");
			}
		}
		mainArg.setValue(mainSb.toString());

		Connector.Argument suspendArg = defaultArguments.get("suspend");
		suspendArg.setValue("true");

		Connector.Argument optionArg = (Connector.Argument) defaultArguments
				.get("options");
		
		StringBuilder optionSb = new StringBuilder();
		String projectRoot = new File(classPathXml).getParent();
		String user_dir = "-Duser.dir=" + projectRoot ;
		
		String cp = "-Djava.class.path=" + getClassPath(classPathXml);
		optionSb.append(user_dir).append(" ");
		optionSb.append(cp).append(" ");
		if (opts != null) {
			for (String opt : opts) {
				optionSb.append(opt).append(" ");
			}
		}
		optionArg.setValue(optionSb.toString());
		

		try {
			VirtualMachine vm = launchingConnector.launch(defaultArguments);
			return vm;
		} catch (IOException e) {
			e.printStackTrace();
		} catch (IllegalConnectorArgumentsException e) {
			e.printStackTrace();
		} catch (VMStartException e) {
			e.printStackTrace();
		}
		return null;
	}

	public VirtualMachine attachToVm(String port) {
		AttachingConnector connector = getAttachingConnector();
		if (connector == null) return null;
		Map<String, Connector.Argument> args = connector.defaultArguments();
		Connector.Argument portArg = args.get("port");
		Connector.Argument hostArg = args.get("hostname");
		try {
			portArg.setValue(port);
			hostArg.setValue("127.0.0.1"); 
			VirtualMachine vm = connector.attach(args);
			return vm;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	private static AttachingConnector getAttachingConnector() {
		VirtualMachineManager vmm = Bootstrap.virtualMachineManager();
		List<AttachingConnector> connectors = vmm.attachingConnectors();
		AttachingConnector connector = null;
		for (AttachingConnector conn : connectors) {
			if (conn.name().equals("com.sun.jdi.SocketAttach"))
				connector = conn;
		}
		return connector;
	}
	
	private static ListeningConnector getListeningConnector() {
		VirtualMachineManager vmm = Bootstrap.virtualMachineManager();
		List<ListeningConnector> connectors = vmm.listeningConnectors();
		ListeningConnector connector = null;
		for (ListeningConnector conn : connectors) {
			if (conn.name().equals("com.sun.jdi.SocketListen"))
				connector = conn;
		}
		return connector;
	}
	
	class BufferChecker implements Runnable {
		public void run() {
			synchronized (buffer) {
				if ( ! (buffer.length() > 0)) return; 
				String[] args = new String[]{} ;
				VjdeUtil.callVimFunc(vimServerName, "FetchJdbResult", args);
			}
		}
	}
}
	 
