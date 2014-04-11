package com.github.vinja.server; 

import java.io.File;

import com.github.vinja.locate.FileSystemDb;


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
		if (args.length < 2 && ! ( action.equals("list") || action.equals("help")) ) {
			return "not enough arguments.";
		}
		if (!isActionValid(action)) {
			return "the action "+action+"  is not valid.";
		}
		
		if (args.length > 1) {
			name = args[args.length-1];
            parseArgument(args);
		}
		boolean indexed = fileSystemDb.alreadyIndexed(name, dir);

		if (action.equals("add")) {
			if (indexed) return "dir or name already exists.";
			fileSystemDb.addIndexedDir(name, dir, excludes, depth);
            return "locatedb add succeeded";
		} else if (action.equals("rm")) {
			if (!indexed) return "dir or name not exists.";
			fileSystemDb.removeIndexedDir(name);
            return "locatedb rm succeeded";
		} else if (action.equals("update")) {
			if (!indexed) return "dir or name not exists.";
			fileSystemDb.refreshIndex(name);
            return "locatedb update succeeded";
		} else if (action.equals("list")) {
			return fileSystemDb.listIndexedDir();
		} else if (action.equals("help")) {
		    String help= "Usage:\n"
		                + "\tlocatedb add [--depth n] [--excludes pattern] [--dir path] \n" 
                        + "\tlocatedb {refresh|remove} name\n"
		                + "\tlocatedb list";
		    return help;
		}
        return "no such locatedb command";
		
	}
	
	
	private void parseArgument(String[] args) {
		//args[0] is locatedb action
		int i = 1;
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
				dir = args[i++].replace("/", File.separator);
			}
		}
	}
	
	
	private boolean isActionValid(String action) {
		String[] validActions = new String[] {"add","rm","update","list","help"};
		for (String name : validActions) {
			if (name.equals(action)) return true;
		}
		return  false;
	}
}
