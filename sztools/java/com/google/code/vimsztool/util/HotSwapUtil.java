package com.google.code.vimsztool.util;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Logger;

import com.google.code.vimsztool.debug.Debugger;
import com.sun.jdi.ReferenceType;
import com.sun.jdi.VirtualMachine;

public class HotSwapUtil {
	private static Logger log = JdeLogger.getLogger("HotSwapServer");

	@SuppressWarnings("unchecked")
	public static void replace(Debugger debugger, File classFile, String className) {
		
		VirtualMachine vm = debugger.getVm();
		if (vm == null) return;
		if (!vm.canRedefineClasses()) return;
		
		byte[] classBytes = loadClassFile(classFile);
		if (classBytes == null ) return;
		
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

	private static byte[] loadClassFile(File classFile) {
		try {
			DataInputStream in = new DataInputStream(new FileInputStream(classFile));
			byte[] ret = new byte[(int) classFile.length()];
			in.readFully(ret);
			in.close();
			return ret;
		} catch (IOException e) {
			log.info(e.getMessage());
		}
		return null;
	}
	
}
