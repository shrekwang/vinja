package com.google.code.vimsztool.server;

import org.eclipse.swt.widgets.Display;

import com.google.code.vimsztool.ui.JdtUI;
import com.google.code.vimsztool.util.ShellUtil;

public class SzjdeTreeCommand extends SzjdeCommand {

	public String execute() {
		String treePath = params.get("treePath");
		String cmdName = params.get("cmdName");
		doTreeCmd(treePath,cmdName);
		return "";
	}
	
	public void doTreeCmd(final String treePath, final String treeCmd) {
		Thread job = new Thread() {
			public void run() {
				final Display display = JdtUI.instance.getDisplay();
				display.asyncExec(new Runnable() {
					public void run() {
						if (treeCmd.equals("openWithDefault")) {
							ShellUtil.openFileWithDefaultApp(treePath);
						} else if (treeCmd.equals("openInTerminal")) {
							ShellUtil.openTerminal(treePath);
						}
					}
				});
			}
		};
		job.start();
	}

}