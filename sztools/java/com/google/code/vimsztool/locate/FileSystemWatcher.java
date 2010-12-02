package com.google.code.vimsztool.locate;

import java.util.HashMap;
import java.util.Map;

import net.contentobjects.jnotify.JNotify;
import net.contentobjects.jnotify.JNotifyListener;

public class FileSystemWatcher implements JNotifyListener {
	
	private Map<String,String> watchedDir= new HashMap<String,String>();
	private FileSystemDb fileSystemDb = FileSystemDb.getInstance();

		
	public void removeWatch(String path) {
		String IdStr = watchedDir.get(path);
		if (IdStr == null) return ;
		try {
			JNotify.removeWatch(Integer.parseInt(IdStr));
		} catch (Exception e) {
		}
		
	}
	public void addWatch(String path) {
		
		if (watchedDir.get(path)!=null ) return;
		
		int mask = JNotify.FILE_CREATED | JNotify.FILE_DELETED | JNotify.FILE_RENAMED;
		boolean watchSubtree = true;
		try {
			int watchID = JNotify.addWatch(path, mask, watchSubtree, this);
			watchedDir.put(path, String.valueOf(watchID)	);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void fileDeleted(int wd, String rootPath, String name) {
		fileSystemDb.removeFileRecord(rootPath, name);
	}

	public void fileCreated(int wd, String rootPath, String name) {
		fileSystemDb.addFileRecord(rootPath, name);
	}

	public void fileRenamed(int wd, String rootPath, String oldName, String newName) { }
	public void fileModified(int wd, String rootPath, String name) { }

}
