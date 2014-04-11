package com.google.code.vimsztool.server; 

import com.google.code.vimsztool.locate.FileSystemDb;


public class SzjdeLocatedbCommand extends SzjdeCommand {
	
	private static FileSystemDb fileSystemDb = FileSystemDb.getInstance();
	
	private int depth = 200 ;
	private String excludes;
	private String dir;
	private String action;
	private String name;
	
	
	public String execute() {
		String[] args = params.get(SzjdeConstants.PARAM_ARGS	).split(";");
		dir = params.get(SzjdeConstants.PARAM_PWD);
		action = args[0];
		if (args.length < 2 && ! action.equals("list")) {
			return "not enough arguments.";
		}
		if (!isActionValid(action)) {
			return "the action "+action+"  is not valid.";
		}
		
		if (args.length > 1) {
			name = args[1];
		}
		parseArgument(args);
		boolean indexed = fileSystemDb.alreadyIndexed(name, dir);
		if (action.equals("add")) {
			if (indexed) return "dir or name already exists.";
			fileSystemDb.addIndexedDir(name, dir, excludes, depth);
		} else if (action.equals("remove")) {
			if (!indexed) return "dir or name not exists.";
			fileSystemDb.removeIndexedDir(name);
		} else if (action.equals("refresh")) {
			if (!indexed) return "dir or name not exists.";
			fileSystemDb.refreshIndex(name);
		} else if (action.equals("list")) {
			return fileSystemDb.listIndexedDir();
		}
		
		return "locatedb command succeeded";
	}
	
	
	private void parseArgument(String[] args) {
		int i = 0;
		while (i < args.length && args[i].startsWith("-")) {
			String arg = args[i++];
			if (arg.equals("--depth") && ( i<args.length)	) {
				String depthStr = args[i++];
				try {
					depth = Integer.parseInt(depthStr);
				} catch (Exception e) {
				}
			}
			if (arg.equals("--excludes") && ( i<args.length) ) {
				excludes = args[i++];
			}
			if (arg.equals("--dir")) {
				dir = args[i++];
			}
		}
	}
	
	
	private boolean isActionValid(String action) {
		String[] validActions = new String[] {"add","remove","refresh","list"};
		for (String name : validActions) {
			if (name.equals(action)) return true;
		}
		return  false;
	}
}
