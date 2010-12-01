package com.google.code.vimsztool.locate;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.DirectoryWalker;

public class RecordCollector extends DirectoryWalker<Record> {
	
	private String startPath;
	private List<Pattern> excludePatterns = new ArrayList<Pattern>();
	
	public static void main(String[] args) {
		/*
		RecordCollector app = new RecordCollector();
		List<Record> records=app.collect("/project/vim-sztool","sztools/java/*,*.py,*.jar");
		for (Record record : records) {
			System.out.println(record.getName()+"," +record.getStartDir()+","+record.getRelativePath());
		}
		*/
		FileSystemDb app = new FileSystemDb("/project/vim-sztool/test.db");
		app.indexDir("/project/vim-sztool");
	}
	
	private Pattern translate(String pat) {
		if (pat == null || pat.trim().equals("")) return null;
		StringBuilder sb= new StringBuilder(".*/");
		for (int i=0; i<pat.length(); i++) {
			char c = pat.charAt(i);
			switch (c) {
			case '*':
				sb.append(".*");
				break;
			case '?':
				sb.append(".");
				break;
			default:
				sb.append(Pattern.quote(String.valueOf(c)));
			}
		}
		return Pattern.compile(sb.toString());
	}

    public List<Record> collect(String startPath,String excludes) {
      List<Record> results = new ArrayList<Record>();
      if (excludes!=null) {
	      String[] excludeStrs = excludes.split(",");
	      for (String str : excludeStrs) {
	    	  this.excludePatterns.add(translate(str));
	      }
      }
      
      this.startPath = startPath;
      try {
    	  File startDirectory = new File(startPath);
	      walk(startDirectory, results);
      } catch (Exception e) {
    	  e.printStackTrace();
      }
      return results;
    }

    @Override
	protected boolean handleDirectory(File directory, int depth,
			Collection<Record> results) throws IOException {
    	if (isVcsMetaDir(directory)) return false;
    	if (! directory.getPath().equals(startPath)
    			&& ! isExclude(directory)) {
	    	results.add(buildRecord(directory));
    	}
    	return true;
	}

	@Override
	protected void handleFile(File file, int depth, Collection<Record> results)
			throws IOException {
		if (! isExclude(file)) {
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
    
    private boolean isExclude(File file) {
    	if (this.excludePatterns.size() == 0 ) return false;
    	String name = file.getAbsolutePath();
    	for (Pattern pat : this.excludePatterns) {
    		Matcher matcher = pat.matcher(name);
    		if (matcher.matches()) return true;
    	}
    	return false;
    }
    
    private boolean isVcsMetaDir(File dir) {
    	String dirName= dir.getName();
    	if (dirName.equalsIgnoreCase(".cvs") 
    			 || dirName.equalsIgnoreCase(".hg") 
    			 || dirName.equalsIgnoreCase(".svn")
    			 || dirName.equalsIgnoreCase(".git")) {
    		return true;
    	} 
    	return false;
    }
    
  }