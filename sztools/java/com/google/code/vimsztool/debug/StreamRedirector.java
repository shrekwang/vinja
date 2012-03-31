package com.google.code.vimsztool.debug;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import com.google.code.vimsztool.util.VjdeUtil;

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
				VjdeUtil.callVimFunc(vimServerName, "FetchDebugOutput",new String[] { line} );
			}
		} catch (Exception e) {
			throw new Error(e);
		}
	}

}
