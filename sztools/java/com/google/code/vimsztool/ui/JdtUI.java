package com.google.code.vimsztool.ui;

import java.io.File;
import java.io.InputStream;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Tray;
import org.eclipse.swt.widgets.TrayItem;

import com.google.code.vimsztool.server.SzjdeServer;
import com.google.code.vimsztool.util.Preference;
import com.google.code.vimsztool.util.UserLibConfig;

public class JdtUI {
	private Shell shell;
	private Tray systemTray;
	private Image img;
	private TrayItem systemTrayItem;
	private Display display;
	private Preference pref = Preference.getInstance();

	public static void main(String[] args) {

		int i = 0;
		String confPath = "";
		while (i < args.length && args[i].startsWith("-")) {
			String arg = args[i++];
			if (arg.equals("--conf") && ( i<args.length)) {
				confPath = args[i++];
			}
		}
		JdtUI ui = new JdtUI(confPath);
		ui.run();
	}
	public JdtUI(String confPath) {
		pref.init(confPath);
	}

	public void run() {
		display = new Display();
		shell = new Shell(display);
		initSysTray();
		initServer();
		initUserLibConfig();
		shell.setSize(700, 500);
		shell.setText("SzTool"); //$NON-NLS-1$

		while (!shell.isDisposed()) {
			if (!display.readAndDispatch())
				display.sleep();
		}
	}

	private void exit() {
		img.dispose();
		shell.dispose();
		System.exit(0);
	}

	private void initServer() {
		try {
			String port=pref.getValue(Preference.JDE_SERVER_PORT);
			int portNum = Integer.parseInt(port);
			new SzjdeServer(portNum).start();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void initUserLibConfig() {
		String conxml = pref.getValue(Preference.JDE_ECLIPSE_CONXML_PATH);
		File file = new File(conxml);
		if (file.exists() && file.isFile() && file.canRead()) {
			UserLibConfig.init(conxml);
		}
	}

	private void initSysTray() {
		systemTray = shell.getDisplay().getSystemTray();

		final Menu systemTrayMenu = new Menu(shell, SWT.POP_UP);
		MenuItem quitItem = new MenuItem(systemTrayMenu, SWT.NONE);
		quitItem.setText("quit");

		quitItem.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				exit();
			}
		});

		systemTrayItem = new TrayItem(systemTray, SWT.NONE);
		InputStream is = JdtUI.class.getClassLoader().getResourceAsStream(
				"resource/logo.png");
		img = new Image(Display.getDefault(), is);

		systemTrayItem.setImage(img);
		systemTrayItem.setToolTipText("SzTool"); //$NON-NLS-1$

		systemTrayItem.addListener(SWT.MenuDetect, new Listener() {
			public void handleEvent(Event event) {
				systemTrayMenu.setVisible(true);
			}
		});

	}

}
