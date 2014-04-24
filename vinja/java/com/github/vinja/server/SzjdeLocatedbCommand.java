package com.github.vinja.server; 

import java.io.File;

import com.github.vinja.locate.FileSystemDb;


public class SzjdeLocatedbCommand extends SzjdeShextCommand {
	
	private static FileSystemDb fileSystemDb = FileSystemDb.getInstance();
	
	private int depth = 200 ;
	private String excludes;
	private String dir;
	private String action;
	private String name;
	
	private final String helpMsg = "locatedb Usage:\n\n"
	        + "\tlocatedb add [--depth n] [--excludes pattern] [--dir path] alias \n" 
	        + "\tif no [--dir path] specified, add current dir content to index db\n"
	        + "\n"
	        + "\tlocatedb {refresh|rm} alias  \n"
	        + "\trefresh or remove the index \n"
	        + "\n"
	        + "\tlocatedb {list}\n"
	        + "\tlist all indexed dir \n";

	private final String NOT_EXISTS = "dir or name not exists";
	public static final String SEP_ROW_TXT="=========================\n";
	
	
	@Override
    public Thread createShextJob() {
        Thread job = new Thread() {
            public void run() {
                runLocatedbCommand();
            }
        };
        return job;
    }

    @Override
    public String getCmdName() {

		String[] args = params.get(SzjdeConstants.PARAM_ARGS	).split(";");
		StringBuffer orignalCmd  = new StringBuffer();
		for (String arg : args) {
		    orignalCmd.append(arg).append(" ");
		}
		return "locatedb " + orignalCmd;
    }
	
	public void runLocatedbCommand() {
		String[] args = params.get(SzjdeConstants.PARAM_ARGS	).split(";");
		dir = params.get(SzjdeConstants.PARAM_PWD);
		action = args[0];
		if (args.length < 2 && ! ( action.equals("list") || action.equals("help")) ) {
			out.println(helpMsg);
			return;
		}
		
		if (args.length > 1) {
			name = args[args.length-1];
            parseArgument(args);
		}

		if (action.equals("add")) {
            boolean indexed = fileSystemDb.alreadyIndexed(name, dir);
			if (indexed) {
			    out.println("dir or name already exists.");
			    return;
			}
			fileSystemDb.addIndexedDir(name, dir, excludes, depth,out);
            return;
		} else if (action.equals("rm")) {
			String[] nameList = name.split(",");
			for (String sname : nameList ) {
                fileSystemDb.removeIndexedDir(sname,out);
                out.println(SEP_ROW_TXT);
                
			}
            String msg = fileSystemDb.listIndexedDir();
            out.println(msg);
            return ;

		} else if (action.equals("refresh")) {
			

			String[] nameList = name.split(",");
			for (String sname : nameList ) {
                boolean indexed = fileSystemDb.alreadyIndexed(sname, dir);
                if (!indexed) {
                    out.println(NOT_EXISTS);
                    continue ;
                }
                fileSystemDb.refreshIndex(sname,out);
                out.println(SEP_ROW_TXT);
			}
            return;

		} else if (action.equals("list")) {
			String msg = fileSystemDb.listIndexedDir();
			out.println(msg);
			return;
		} 
		out.println(helpMsg);
        return ;
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
	
	
}
