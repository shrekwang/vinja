package com.google.code.vimsztool.ui;

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
import com.google.code.vimsztool.util.UserLibConfig;

public class JdtUI {
	private Shell shell;
	private Tray systemTray;
	private Image img;
	private TrayItem systemTrayItem;
	private Display display;
	private static final int DEFAULT_PORT = 9527;
	private String port;
	private String conxml="/home/wsn/.vim/sztools/share/conf/userlib.xml";

	public static void main(String[] args) {

		int i = 0;
		String port = null;
		String conxml = null;
		while (i < args.length && args[i].startsWith("-")) {
			String arg = args[i++];
			if (arg.equals("--port") && (i < args.length)) {
				port = args[i++];
			}
			if (arg.equals("--conxml") && (i < args.length)) {
				conxml = args[i++];
			}
		}
		JdtUI ui = new JdtUI();
		if (port !=null) ui.setPort(port);
		if (conxml !=null) ui.setConxml(conxml);
		ui.run();
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
			int portNum = (port == null) ? DEFAULT_PORT : Integer.parseInt(port);
			new SzjdeServer(portNum).start();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void initUserLibConfig() {
		UserLibConfig.init(conxml);
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

	public void setPort(String port) {
		this.port = port;
	}

	public void setConxml(String conxml) {
		this.conxml = conxml;
	}

}
