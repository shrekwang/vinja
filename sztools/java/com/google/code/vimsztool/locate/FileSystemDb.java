package com.google.code.vimsztool.locate;

import java.io.File;
import java.util.ArrayList;
import java.util.List;


public class FileSystemDb {
	
	SqliteManager sqliteManager ;

	public FileSystemDb(String dbPath) {
		File file = new File(dbPath);
		sqliteManager = new SqliteManager(dbPath);
		if (!file.exists()) {
			File parent=file.getParentFile();
			if (!parent.exists()) {
				parent.mkdirs();
			}
			try {
				file.createNewFile();
			} catch (Exception e) {
				
			}
			this.initTable();
		}
	}
  
    public void initTable() {
        String sql1 = " create table dirlist( integer primary key, alias varchar(60), path varchar(160) ) ";
        String sql2 = " create table locatedb( integer primary key ,  root varchar(200), "
               +" name varchar(80),   path varchar(160) )  ";
        sqliteManager.executeUpdate(sql1);
        sqliteManager.executeUpdate(sql2);
    }
    
    public void indexDir(String dir) {
    	RecordCollector recordCollector=new RecordCollector();
    	List<Record> records=recordCollector.collect("/project/vim-sztool", null);
    	List<String[]> values = new ArrayList<String[]>();
    	for (Record record : records) {
    		String[] valueArray = new String[3];
    		valueArray[0] = record.getName();
    		valueArray[1] = record.getStartDir();
    		valueArray[2] = record.getRelativePath();
    		values.add(valueArray);
    	}
    	String sql = "insert into locatedb (name,root,path) values(?,?,?)";
    	sqliteManager.batchUpdate(sql, values);
    	
    }
    
}
