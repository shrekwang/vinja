package com.google.code.vimsztool.server; 

import com.google.code.vimsztool.locate.FileSystemDb;


public class SzjdeLocateCommand extends SzjdeCommand {
	
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
		name = args[1];
		if (args.length < 2 ) {
			return "not enough arguments.";
		}
		if (!isActionValid(action)) {
			return "the action "+action+"  is not valid.";
		}
		parseArgument(args);
		boolean indexed = fileSystemDb.alreadyIndexed(name, dir);
		if (action.equals("add")) {
			if (indexed) return "dir or name already exists.";
			fileSystemDb.indexDir(name, dir, excludes, depth);
		} else if (action.equals("remove")) {
			if (!indexed) return "dir or name not exists.";
			fileSystemDb.removeIndex(name);
		} else if (action.equals("refresh")) {
			if (!indexed) return "dir or name not exists.";
			fileSystemDb.refreshIndex(name);
		}
		
		return "sp command succeeded";
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
		String[] validActions = new String[] {"add","remove","refresh"};
		for (String name : validActions) {
			if (name.equals(action)) return true;
		}
		return  false;
	}
}
