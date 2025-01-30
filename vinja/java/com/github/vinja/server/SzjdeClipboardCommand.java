package com.github.vinja.server;

import com.github.vinja.ui.JdtUI;

public class SzjdeClipboardCommand extends SzjdeCommand {

	public String execute() {
		String opname = params.get(SzjdeConstants.PARAM_OPNAME);
		if (opname.equals("get")) {
			return getClipboardContent();
		}
		String value = params.get("value");
		setClipboardContent(value);
		return "";
	}

	public void setClipboardContent(final String value) {
	}
	
	public String getClipboardContent() {
		return "";
	}

}