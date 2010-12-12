package com.google.code.vimsztool.util;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.sun.jdi.Bootstrap;
import com.sun.jdi.ReferenceType;
import com.sun.jdi.VirtualMachine;
import com.sun.jdi.VirtualMachineManager;
import com.sun.jdi.connect.AttachingConnector;
import com.sun.jdi.connect.Connector;
import com.sun.jdi.connect.IllegalConnectorArgumentsException;

public class HotSwapUtil {

	private boolean enabled;
	private VirtualMachine vm;
	private String transportName = "dt_socket";
	private String port;
	private String host;
	private static HotSwapUtil instance = new HotSwapUtil();
	
	private HotSwapUtil() {}
	public static HotSwapUtil getInstance() {
		return instance;
	}
	
	private void connect() throws IOException, IllegalConnectorArgumentsException {
		AttachingConnector connector = findAttachingConnector();
		Map<String, Connector.Argument> args = connector.defaultArguments();
		Connector.Argument arg;
		arg = args.get("port");
		arg.setValue(getPort());
	    vm = connector.attach(args);

	}

	@SuppressWarnings("unchecked")
	public void replace(File classFile, String className) throws Exception {
		
		if (vm == null) {
			connect();
		}
		if (!vm.canRedefineClasses()) {
			return;
		}
		
		byte[] classBytes = loadClassFile(classFile);
		List classes = vm.classesByName(className);

		if (classes == null || classes.size() == 0)
			return;

		for (int i = 0; i < classes.size(); i++) {
			ReferenceType refType = (ReferenceType) classes.get(i);
			HashMap map = new HashMap();
			map.put(refType, classBytes);
			vm.redefineClasses(map);
		}
	}

	@SuppressWarnings("unchecked")
	private AttachingConnector findAttachingConnector() {
		VirtualMachineManager manager = Bootstrap.virtualMachineManager();
		Iterator iter = manager.attachingConnectors().iterator();
		while (iter.hasNext()) {
			AttachingConnector ac = (AttachingConnector) iter.next();
			if (ac.transport().name().equals(this.transportName))
				return ac;
		}
		return null;
	}

	private byte[] loadClassFile(File classFile) throws IOException {
		DataInputStream in = new DataInputStream(new FileInputStream(classFile));

		byte[] ret = new byte[(int) classFile.length()];
		in.readFully(ret);
		in.close();

		return ret;
	}
	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}
	public boolean isEnabled() {
		return enabled;
	}
	public void setPort(String port) {
		this.port = port;
	}
	public String getPort() {
		return port;
	}
	public void setHost(String host) {
		this.host = host;
	}
	public String getHost() {
		return host;
	}
}
