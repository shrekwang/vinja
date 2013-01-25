package com.google.code.vimsztool.debug;

import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.FilenameUtils;

import com.google.code.vimsztool.compiler.CompilerContext;
import com.google.code.vimsztool.compiler.CompilerContextManager;
import com.google.code.vimsztool.compiler.TomcatJvmoptConf;
import com.google.code.vimsztool.debug.eval.ExpEval;
import com.google.code.vimsztool.exception.NoConnectedVmException;
import com.google.code.vimsztool.exception.NoSuspendThreadException;
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


	private VirtualMachine vm = null;
	private Process process = null;
	private EventHandler eventHandler = null;
	private String vimServerName = "";
	private CompilerContext compilerContext =null;
	private Preference pref = Preference.getInstance();
	private StringBuffer buffer = new StringBuffer();
	private ScheduledExecutorService exec = null;
	
	private BreakpointManager bpMgr = null;
	private DisplayVariableManager dvMgr = null;
	private ExceptionPointManager ecpMgr  = null;
	private StepManager stepMgr = null;
	private ExpEval expEval = null;
	private SuspendThreadStack suspendThreadStack;
	
	public static final String CMD_SUCCESS = "success";
	public static final String CMD_FAIL = "fail";
	
	private static ConcurrentHashMap<String, Debugger> instances = new ConcurrentHashMap<String, Debugger>();

	private Debugger(String vimServerName) {
		this.vimServerName = vimServerName;
		bpMgr = new BreakpointManager(this);
		dvMgr = new DisplayVariableManager(this);
		ecpMgr  = new ExceptionPointManager(this);
		stepMgr = new StepManager(this);
		expEval = new ExpEval(this);
		suspendThreadStack = new SuspendThreadStack();
	}

	public static Debugger getInstance(String vimServerName) {
		Debugger instance = instances.get(vimServerName);
		if (instance == null) {
			instance = new Debugger(vimServerName);
			instances.put(vimServerName, instance);
		}
		return instance;
	}
	
	public static List<Debugger> getInstances() {
		List<Debugger> result = new ArrayList<Debugger>();
		result.addAll(instances.values());
		return result;
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
		
		bpMgr.tryCreatePrepareRequest();
		eventHandler = new EventHandler(this);
		eventHandler.start();

		if (process !=null) {
			StreamGobbler stdOut=new StreamGobbler(buffer, process.getInputStream());
			StreamGobbler stdErr=new StreamGobbler(buffer, process.getErrorStream());
			stdOut.start();
			stdErr.start();
			exec = Executors.newScheduledThreadPool(1);
	        exec.scheduleAtFixedRate(new BufferChecker(), 1, 70, TimeUnit.MILLISECONDS);
		}
	}
	
	public String launch(String classPathXml, String cmdLine,boolean runAsTest) {
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
		
		vm = launch(className,classPathXml,opts,args,runAsTest);
	
		startProcess();
		return "run class: " + className;
		
	}

	public String attach(String host,String port) {
		if (vm != null) 
			return "VirtualMachine is not null";
		vm = attachToVm(host, port);
		if (vm == null)
			return "attach to port fails.";
		startProcess();
		bpMgr.tryCreateBreakpointRequest();
		
		ecpMgr.tryCreateExceptionRequest();
		
		return "attach to remote vm succeeded.";
	}
	
	public String listBreakpoints() {
		List<Breakpoint> bps = bpMgr.getAllBreakpoints();
		if (bps.size() == 0) {
			return "no breakpoints or watchpoints.";
		}
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
				
				if (bp.getAccessMode() == Breakpoint.ACCESS_READ) {
					sb.append("Read");
				} else if (bp.getAccessMode() == Breakpoint.ACCESS_WRITE) {
					sb.append("Write");
				} else {
					sb.append("Read|Write");
				}
			}
			sb.append("\n");
		}
		
		return sb.toString();
	}
	
	private List<String> getStackFrameInfos(ThreadReference threadRef) {
		List<String> result = new ArrayList<String>();
		long currentSuspendId = -1;
		if (suspendThreadStack.getCurThreadRef() != null) {
			currentSuspendId = suspendThreadStack.getCurThreadRef().uniqueID();
		}
		
		try {
			List<StackFrame> frames = threadRef.frames();
			for (int i=0; i<frames.size(); i++) {
				StackFrame frame = frames.get(i);
				Location loc = frame.location();
				String name = loc.declaringType().name() + "."
						+ loc.method().name();
				String frameInfo = name + " line: " + loc.lineNumber();
				if (i == suspendThreadStack.getCurFrame()
						&& threadRef.uniqueID() == currentSuspendId ) {
					frameInfo = frameInfo + "  (current frame) ";
				}
				String num = padStr(3,"#"+i);
				result.add( num +":  " + frameInfo );
			}
		} catch (Throwable e) {
		}
		return result;
	}
	
	private static String padStr(int maxLen, String origStr) {
		if (origStr  == null) return "";
		int len = origStr.length();
		if (len >= maxLen ) return origStr;
		for (int i=0; i< (maxLen-len); i++) {
			origStr = origStr + " ";
		}
		return origStr;
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
		long currentSuspendId = -1;
		if (suspendThreadStack.getCurThreadRef() != null) {
			currentSuspendId = suspendThreadStack.getCurThreadRef().uniqueID();
		}
		
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
			if (ref.uniqueID() == currentSuspendId) {
				sb.append(" (current thread)");
			}
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
	
	public void checkVm() {
		if (getVm() == null ) {
			throw new NoConnectedVmException();
		}
		
		
	}
	public void checkSuspendThread() {
		ThreadReference threadRef = suspendThreadStack.getCurThreadRef();
		if (threadRef == null ) {
			throw new NoSuspendThreadException();
		}
	}
	
	
	public String listFrames() {
		checkVm();
		checkSuspendThread();
		ThreadReference threadRef = suspendThreadStack.getCurThreadRef();
		StringBuilder sb = new StringBuilder();
		appendStackInfo(sb,threadRef,0);
		return sb.toString();
	}
	
	public String listThreads() {
		checkVm();
		List<ThreadReference> threads = vm.allThreads();
		StringBuilder sb = new StringBuilder(vm.name());
		sb.append("\n");
		for (ThreadReference ref : threads) {
			appendStackInfo(sb,ref,1);
		}
		return sb.toString();
	}

	public String resume() {
		checkVm();
		checkSuspendThread();
		ThreadReference threadRef = suspendThreadStack.getCurThreadRef();
		suspendThreadStack.clean();
		threadRef.resume();
		return "";
	}
	
	public String changeCurrentThread(String uniqueId) {
		checkVm();
		
		List<ThreadReference> threads = vm.allThreads();
		ThreadReference correctRef = null;
		for (ThreadReference ref : threads) {
			if (String.valueOf(ref.uniqueID()).equals(uniqueId)) {
				if (ref.isSuspended()) {
					correctRef = ref;
					break;
				}
			}
		}
		if (correctRef == null) return "no matched suspend thread";
		ReferenceType refType;
		try {
			Location loc = correctRef.frame(0).location();
			refType = loc.declaringType();
			suspendThreadStack.setCurRefType(refType);
			suspendThreadStack.setCurThreadRef(correctRef);
			changeVimEditSourceLocaton(loc);
			return "success";
		} catch (IncompatibleThreadStateException e) {
			return "error:" + e.getMessage();
		}
	}
	
	public String currentFrameUp() {
		checkVm();
		checkSuspendThread();
		int curFrame = suspendThreadStack.getCurFrame();
		return changeCurrentFrame(curFrame-1);
	}
	
	public String currentFrameDown() {
		checkVm();
		checkSuspendThread();
		int curFrame = suspendThreadStack.getCurFrame();
		return changeCurrentFrame(curFrame+1);
	}
	
	
	public String changeCurrentFrame(int frameNum) {
		checkVm();
		checkSuspendThread();
		
		ThreadReference threadRef = suspendThreadStack.getCurThreadRef();
		try {
			Location loc = threadRef.frame(frameNum).location();
			ReferenceType refType= loc.declaringType();
			suspendThreadStack.setCurRefType(refType);
			suspendThreadStack.setCurFrame(frameNum);
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
		String[] args = {"suspend", abPath, String.valueOf(lineNum), className,"null"};
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
		suspendThreadStack.clean();
		if (this.exec != null) {
			exec.shutdown();
		}
		//do last buffer check
		new BufferChecker().run();
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
		cmd.append(" " + TomcatJvmoptConf.getJvmOptions());
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
			List<String> opts,List<String> args,boolean runAsTest) {
		
		
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
		if (runAsTest) {
			cmd.append("org.junit.runner.JUnitCore").append(" ");
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

	public VirtualMachine attachToVm(String host, String port) {
		AttachingConnector connector = getAttachingConnector();
		if (connector == null) return null;
		Map<String, Connector.Argument> args = connector.defaultArguments();
		Connector.Argument portArg = args.get("port");
		Connector.Argument hostArg = args.get("hostname");
		try {
			portArg.setValue(port);
			if (host == null || host.trim().equals("")) {
				hostArg.setValue("127.0.0.1"); 
			} else {
				hostArg.setValue(host);
			}
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

	public BreakpointManager getBreakpointManager() {
		return bpMgr;
	}

	public DisplayVariableManager getDisplayVariableManager() {
		return dvMgr;
	}

	public ExceptionPointManager getExceptionPointManager() {
		return ecpMgr;
	}

	public StepManager getStepMgr() {
		return stepMgr;
	}

	public ExpEval getExpEval() {
		return expEval;
	}

	public SuspendThreadStack getSuspendThreadStack() {
		return suspendThreadStack;
	}

	public void setSuspendThreadStack(SuspendThreadStack suspendThreadStack) {
		this.suspendThreadStack = suspendThreadStack;
	}

}
	 
