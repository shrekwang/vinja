package com.google.code.vimsztool.debug;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.Map;

import com.google.code.vimsztool.compiler.CompilerContext;
import com.google.code.vimsztool.compiler.CompilerContextManager;
import com.sun.jdi.Bootstrap;
import com.sun.jdi.VirtualMachine;
import com.sun.jdi.VirtualMachineManager;
import com.sun.jdi.connect.AttachingConnector;
import com.sun.jdi.connect.Connector;
import com.sun.jdi.connect.IllegalConnectorArgumentsException;
import com.sun.jdi.connect.LaunchingConnector;
import com.sun.jdi.connect.VMStartException;

public class ConnectorManager {

	private static String getClassPath(String classPathXml) {
		CompilerContextManager ccm = CompilerContextManager.getInstnace();
		CompilerContext ctx = ccm.getCompilerContext(classPathXml);

		List<URL> urls = ctx.getClassPathUrls();
		StringBuilder sb = new StringBuilder();
		for (URL url : urls) {
			sb.append(url.getPath()).append(File.pathSeparator);
		}
		return sb.toString();
	}

	public static VirtualMachine launch(String mainClass, String classPathXml,
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

	public static VirtualMachine attach(String port) {
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

}
