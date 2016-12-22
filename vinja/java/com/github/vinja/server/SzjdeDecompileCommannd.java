package com.github.vinja.server;

import com.github.vinja.util.DecompileUtil;

public class SzjdeDecompileCommannd extends SzjdeCommand {

	@SuppressWarnings("all")
	public String execute() {
		String jarPath = params.get("jarPath");
		String innerClass = params.get("innerPath");

		try {
			String t = DecompileUtil.decompile(jarPath, innerClass);
			return t;
		} catch (Throwable e) {
			e.printStackTrace();
		}
		return "";
	}

}
