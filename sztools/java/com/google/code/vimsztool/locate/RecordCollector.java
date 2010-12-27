package com.google.code.vimsztool.locate;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Logger;

import org.apache.commons.io.DirectoryWalker;

import com.google.code.vimsztool.util.JdeLogger;
import com.google.code.vimsztool.util.VjdeUtil;

public class RecordCollector extends DirectoryWalker<Record> {
	private static Logger log = JdeLogger.getLogger("RecordCollector");
	private String startPath;
	private String excludes;
	

    public List<Record> collect(String startPath,String excludes) {
      List<Record> results = new ArrayList<Record>();
      this.startPath = startPath;
      this.excludes = excludes;
      try {
    	  File startDirectory = new File(startPath);
	      walk(startDirectory, results);
      } catch (Exception e) {
    	  String errorMsg = VjdeUtil.getExceptionValue(e);
  		  log.info(errorMsg);
      }
      return results;
    }

    @Override
	protected boolean handleDirectory(File directory, int depth,
			Collection<Record> results) throws IOException {
    	if (! directory.getPath().equals(startPath)
    			&& ! PatternUtil.isExclude(this.excludes, directory)) {
	    	results.add(buildRecord(directory));
    	}
    	return true;
	}

	@Override
	protected void handleFile(File file, int depth, Collection<Record> results)
			throws IOException {
		if (! PatternUtil.isExclude(this.excludes, file)) {
			results.add(buildRecord(file));
		}
	}

    
    private Record buildRecord(File file) {
    	Record record= new Record();
    	record.setStartDir(startPath);
    	record.setName(file.getName());
    	String absPath = file.getAbsolutePath();
    	int sepIndex = startPath.length() ;
    	if (! startPath.endsWith(File.separator)) {
    		sepIndex = startPath.length() + 1;
    	}
    	record.setRelativePath(absPath.substring(sepIndex));
    	return record;
    }
    
  
    
  
    
  }