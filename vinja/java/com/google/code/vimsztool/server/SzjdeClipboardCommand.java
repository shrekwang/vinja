package com.google.code.vimsztool.server;

import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.FileTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.widgets.Display;

import com.google.code.vimsztool.ui.JdtUI;

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
		Thread job = new Thread() {
			public void run() {
				final Display display = JdtUI.instance.getDisplay();
				display.asyncExec(new Runnable() {
					public void run() {
						String[] names = value.split(";");
						Clipboard clipboard = new Clipboard(display);
						FileTransfer transfer = FileTransfer.getInstance();
						clipboard.setContents(new Object[] { names }, new Transfer[] { transfer });
						clipboard.dispose();
					}
				});
			}
		};
		job.start();
	}
	
	public String getClipboardContent() {
		
		final StringBuilder sb = new StringBuilder();
		final Display display = Display.getDefault();
		
		Thread job = new Thread() {
			public void run() {
				display.syncExec(new Runnable() {
				public void run() {
					Clipboard clipboard = new Clipboard(display);
					FileTransfer transfer = FileTransfer.getInstance();
					String[] data = (String[]) clipboard.getContents(transfer);
					clipboard.dispose();
					for (String name : data) {
						sb.append(name).append(";");
					}
				}
			});
				
			}
		};
		job.start();
		try {
			job.join();
		} catch (InterruptedException e) {
			return "";
		}
			
		return sb.toString();
	}
}