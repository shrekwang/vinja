package com.github.vinja.ui;

import java.awt.AWTException;
import java.awt.MenuItem;
import java.awt.PopupMenu;
import java.awt.SystemTray;
import java.awt.Toolkit;
import java.awt.TrayIcon;
import java.awt.event.ActionListener;

public class Main {

	public static void main(String[] args) {
		 
	     TrayIcon trayIcon = null;
	     if (SystemTray.isSupported()) {
	         // get the SystemTray instance
	         SystemTray tray = SystemTray.getSystemTray();

	         // load an image
	         String fileName = "/Users/wangsn/github/vinja-dev/vinja/java/resource/logo.png";
	         java.awt.Image image = Toolkit.getDefaultToolkit().getImage(fileName);

	         // create a action listener to listen for default action executed on the tray icon
	         java.awt.event.ActionListener listener = new ActionListener() {
	             public void actionPerformed(java.awt.event.ActionEvent e) {
	            	 System.out.println(e);
	             }
	         };
	         // create a popup menu
	         java.awt.PopupMenu popup = new PopupMenu();
	         // create menu item for the default action
	         MenuItem defaultItem = new MenuItem("exit");
	         defaultItem.addActionListener(listener);
	         popup.add(defaultItem);

	         trayIcon = new TrayIcon(image, "Tray Demo", popup);
	         trayIcon.setImageAutoSize(true);

	         trayIcon.addActionListener(listener);
	         try {
	             tray.add(trayIcon);
	         } catch (AWTException e) {
	             System.err.println(e);
	         }
	         
	         trayIcon.displayMessage("Title", "MESSAGE HERE", TrayIcon.MessageType.ERROR); //THIS IS THE LINE THAT SHOULD SHOW THE MESSAGE
	     } else {
	     }
	     
	     while (true) {
	    	 try {
				 Thread.sleep(100000);
	    	 } catch (Exception e) {
	    	 }
	     }
	 }

}
