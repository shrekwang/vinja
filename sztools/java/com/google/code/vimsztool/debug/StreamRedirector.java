package com.google.code.vimsztool.debug;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class StreamRedirector extends Thread {

	private BufferedReader br;
	private String vimServerName ;

	public StreamRedirector(InputStream is,String vimServerName) {
		this.br = new BufferedReader(new InputStreamReader(is));
		this.vimServerName = vimServerName;
	}
	

	public void run() {
		String line;
		try {
			while ((line = br.readLine()) != null) {
				writeToVim(line);
			}
		} catch (Exception e) {
			throw new Error(e);
		}
	}

	private void writeToVim(String line) {
		try {
			String vimCmdCall="<esc><esc>:FetchDebugOutput "+line+"<cr>";
			String[] vimRemoteCmdArray = new String[] {"gvim","--servername",vimServerName,"--remote-send",	vimCmdCall};
			Runtime.getRuntime().exec(vimRemoteCmdArray);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
