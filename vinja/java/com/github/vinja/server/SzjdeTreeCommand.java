package com.github.vinja.server;

import com.github.vinja.ui.JdtUI;
import com.github.vinja.util.ShellUtil;

public class SzjdeTreeCommand extends SzjdeCommand {

	public String execute() {
		String treePath = params.get("treePath");
		String cmdName = params.get("cmdName");
		doTreeCmd(treePath,cmdName);
		return "";
	}
	
	public void doTreeCmd(final String treePath, final String treeCmd) {

	}

}