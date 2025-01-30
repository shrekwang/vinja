package com.github.vinja.ui;

import com.github.vinja.nio.VinjaServer;
import java.io.File;
import java.io.InputStream;

import com.github.vinja.locate.FileSystemDb;
import com.github.vinja.server.SzjdeServer;
import com.github.vinja.util.JdeLogger;
import com.github.vinja.util.Preference;
import com.github.vinja.util.UserLibConfig;
import com.github.vinja.util.VjdeUtil;

public class JdtUI {
	
	private static JdeLogger log = JdeLogger.getLogger("JdtUI");
	public static JdtUI instance = null;
	private Preference pref = Preference.getInstance();

	public static void main(String[] args) {

		int i = 0;
		String vinjaHome = "";
		while (i < args.length && args[i].startsWith("-")) {
			String arg = args[i++];
			if (arg.equals("--vinja-home") && ( i<args.length)) {
				vinjaHome = args[i++];
			}
		}
		instance = new JdtUI(vinjaHome);
		instance.run();
	}
	public JdtUI(String vinjaHome) {
		pref.init(vinjaHome);
	}
	
	public void run() {
		initServer();
		initUserLibConfig();
		initFsWatcher();
		System.out.println("vinja server started");

		while (true) {
			try {
				Thread.sleep(3000);
			} catch (Exception e) {
			}
		}
	}

	public void exit() {
		System.exit(0);
	}
	
	private void initFsWatcher() {
		FileSystemDb.getInstance().initWatchOnIndexedDir();
	}

	private void initServer() {
		try {
			String port=pref.getValue(Preference.JDE_SERVER_PORT);
			int portNum = Integer.parseInt(port);
			new SzjdeServer(portNum).start();

          
            VinjaServer server = new VinjaServer();
            server.start();

		} catch (Exception e) {
			String errorMsg = VjdeUtil.getExceptionValue(e);
    		log.info(errorMsg);
		}
	}

	private void initUserLibConfig() {
		String conxml = pref.getValue(Preference.JDE_ECLIPSE_CONXML_PATH);
		File file = new File(conxml);
		if (file.exists() && file.isFile() && file.canRead()) {
			UserLibConfig.init(conxml);
		}
	}


}
