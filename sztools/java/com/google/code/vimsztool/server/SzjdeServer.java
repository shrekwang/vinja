package com.google.code.vimsztool.server;

import static com.google.code.vimsztool.server.SzjdeConstants.CMD_AUTOIMPORT;
import static com.google.code.vimsztool.server.SzjdeConstants.CMD_COMPILE;
import static com.google.code.vimsztool.server.SzjdeConstants.CMD_COMPLETE;
import static com.google.code.vimsztool.server.SzjdeConstants.CMD_COPY_RESOURCE;
import static com.google.code.vimsztool.server.SzjdeConstants.CMD_DEBUG;
import static com.google.code.vimsztool.server.SzjdeConstants.CMD_DUMP_CLASS;
import static com.google.code.vimsztool.server.SzjdeConstants.CMD_FETCH_RESULT;
import static com.google.code.vimsztool.server.SzjdeConstants.CMD_GET_CONSTRUCTDEFS;
import static com.google.code.vimsztool.server.SzjdeConstants.CMD_GET_DEFCLASS;
import static com.google.code.vimsztool.server.SzjdeConstants.CMD_GET_METHODDEFS;
import static com.google.code.vimsztool.server.SzjdeConstants.CMD_LOCATEDB;
import static com.google.code.vimsztool.server.SzjdeConstants.CMD_OVERIDE;
import static com.google.code.vimsztool.server.SzjdeConstants.CMD_PROJECT_CLEAN;
import static com.google.code.vimsztool.server.SzjdeConstants.CMD_RUN;
import static com.google.code.vimsztool.server.SzjdeConstants.CMD_RUN_SYS;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import com.google.code.vimsztool.debug.DebugCommand;
import com.google.code.vimsztool.util.JdeLogger;
import com.google.code.vimsztool.util.VjdeUtil;

public class SzjdeServer extends Thread {
	private static Logger log = JdeLogger.getLogger("SzjdeServer");
 
   private ServerSocket ss;
   private final static String END_TOKEN="==end==";
   
   public SzjdeServer(int port) throws IOException {
      ss = new ServerSocket(port);
   }

   public void run() {
      while (true) {
         BufferedReader is=null;
         PrintWriter pw =null;
         Socket socket = null;
         try {
             socket = ss.accept();
             pw=new PrintWriter(new OutputStreamWriter(socket.getOutputStream()));
	         is = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             List<String> inputs = new ArrayList<String>();
             while (true) {
	             String param = is.readLine();
	             if (param == null || param.equals(END_TOKEN)) break;
	             inputs.add(param);
             }
             String msg = handleInput(inputs);
    		 pw.write(msg);
             pw.flush();

         }catch (Throwable e) {
    		String errorMsg = VjdeUtil.getExceptionValue(e);
    		log.info(errorMsg);
        	if (pw != null ) {
        		pw.write(errorMsg);
	            pw.flush();
        	}
         } finally {
        	 pw.close();
        	 try { is.close(); } catch (Exception e) {}
             try { socket.close(); } catch (Exception e) {}
         }
      }
   }
   
   public String handleInput(List<String> inputs) {
	   
	   Map<String,String> params=new HashMap<String,String>();
	   String cmdStr = "";
	   for (String line : inputs) {
		   int eqIndex = line.indexOf("=");
		   if (eqIndex < -1 ) continue;
		   String name = line.substring(0,eqIndex).trim();
		   String value = "";
		   if (eqIndex < line.length() -1 ) {
	           value = line.substring(eqIndex + 1).trim();
		   }
		   if (name.equals("cmd")) {
			   cmdStr = value;
		   } else {
			   params.put(name, value);
		   }
	   }
	   SzjdeCommand szjdeCommand = null;
	   if (cmdStr.equals(CMD_COMPILE)) {
		   szjdeCommand = new SzjdeCompilerCommand();
	   } else if (cmdStr.equals(CMD_COMPLETE)) {
		   szjdeCommand = new SzjdeCompletionCommand();
	   } else if (cmdStr.equals(CMD_AUTOIMPORT)) {
		  szjdeCommand = new SzjdeAutoImportCommand(); 
	   } else if (cmdStr.equals(CMD_RUN)) {
		   szjdeCommand = new SzjdeClassRunnerCommand();
	   } else if (cmdStr.equals(CMD_DUMP_CLASS)) {
		   szjdeCommand = new SzjdeDumpClassInfoCommand();
	   } else if (cmdStr.equals(CMD_OVERIDE)) {
		   szjdeCommand = new SzjdeOverideMethodsCommand();
	   } else if (cmdStr.equals(CMD_COPY_RESOURCE)) {
		   szjdeCommand = new SzjdeCopyResourceCommand();
	   } else if (cmdStr.equals(CMD_RUN_SYS) || cmdStr.endsWith(CMD_FETCH_RESULT)) {
		   szjdeCommand = new SzjdeSystemCommand(cmdStr);
	   } else if (cmdStr.equals(CMD_LOCATEDB)) {
		   szjdeCommand = new SzjdeLocatedbCommand();
	   } else if (cmdStr.equals(CMD_PROJECT_CLEAN)) {
		   szjdeCommand = new SzjdeProjectClean();
	   } else if (cmdStr.equals(CMD_GET_DEFCLASS)) {
		   szjdeCommand = new SzjdeGetDefClassCommand();
	   } else if (cmdStr.equals(CMD_GET_METHODDEFS)) {
		   szjdeCommand = new SzjdeGetMethodDefs();
	   } else if (cmdStr.equals(CMD_GET_CONSTRUCTDEFS)) {
		   szjdeCommand = new SzjdeGetConstructorDefs();
	   } else if (cmdStr.equals(CMD_DEBUG)) {
		   szjdeCommand = new DebugCommand();
	   }
	   if (szjdeCommand == null) {
		   return ("can't find the command '"+cmdStr+"' definition.");
	   }
	   szjdeCommand.setParams(params);
	   String result=szjdeCommand.execute();
	   return result;
	   
   }
   
}

