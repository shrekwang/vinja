package com.google.code.vimsztool.server;

import com.google.code.vimsztool.util.HotSwapUtil;

public class SzjdeHotswapCommand extends SzjdeCommand {
	
	public String execute() {
		HotSwapUtil hotSwapUtil = HotSwapUtil.getInstance();
		String hotSwapEnabled = params.get(SzjdeConstants.PARAM_HOTSWAP_ENABLED);
		if (hotSwapEnabled != null && hotSwapEnabled.equals("true")) {
			String port = params.get(SzjdeConstants.PARAM_HOTSWAP_PORT);
			hotSwapUtil.setEnabled(true);
			hotSwapUtil.setPort(port);
		} else {
			hotSwapUtil.setEnabled(false);
		}
		return "";
	}

}
