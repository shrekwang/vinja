package com.google.code.vimsztool.locate;

import net.contentobjects.jnotify.JNotify;
import net.contentobjects.jnotify.JNotifyListener;

public class FileSystemWatcher implements JNotifyListener {

	public FileSystemWatcher() {
		
	}
		
	public void addWatch(String path) {
		
		int mask = JNotify.FILE_CREATED | JNotify.FILE_DELETED
				| JNotify.FILE_MODIFIED | JNotify.FILE_RENAMED;
		boolean watchSubtree = true;

		try {
			int watchID = JNotify.addWatch(path, mask, watchSubtree, this);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void fileRenamed(int wd, String rootPath, String oldName,
			String newName) {
		print("renamed " + rootPath + " : " + oldName + " -> " + newName);
	}

	public void fileModified(int wd, String rootPath, String name) {
		print("modified " + rootPath + " : " + name);
	}

	public void fileDeleted(int wd, String rootPath, String name) {
		print("deleted " + rootPath + " : " + name);
	}

	public void fileCreated(int wd, String rootPath, String name) {
		print("created " + rootPath + " : " + name);
	}

	void print(String msg) {
		System.err.println(msg);
	}

}
