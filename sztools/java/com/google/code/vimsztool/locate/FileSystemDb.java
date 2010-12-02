package com.google.code.vimsztool.locate;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FilenameUtils;

import com.google.code.vimsztool.util.VjdeUtil;


public class FileSystemDb {
	
	private SqliteManager sqliteManager ;
	private static final FileSystemDb instance = new FileSystemDb();
	private FileSystemWatcher fileSystemWatcher=new FileSystemWatcher();
	
	public static FileSystemDb getInstance() {
		return instance;
	}

	private FileSystemDb() {
		String dbPath = FilenameUtils.concat(VjdeUtil.getToolDataHome(),"locate.db");
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
        String sql1 = " create table fsdb_dirs  ( integer primary key, alias varchar(60), "
	        +"start_dir varchar(160) , excludes varchar(500) , depth integer ) ";
        String sql2 = " create table fsdb_files ( integer primary key ,  start_dir varchar(200), "
            +" name varchar(80),   rtl_path varchar(160) )  ";
        sqliteManager.executeUpdate(sql1);
        sqliteManager.executeUpdate(sql2);
    }
    
    public void refreshIndex(String alias) {
    	//todo
    }
    
    public boolean alreadyIndexed(String alias, String path) {
		String sql = " select count(*) from fsdb_dirs where alias='"
			+alias+"' or start_dir ='"+path+"'";
    	List<String[]> values = sqliteManager.query(sql);
    	String countStr = values.get(0)[0];
    	if (!countStr.equals("0")) return true;
    	return false;
    }
    
    public void removeIndex(String alias) {
    	String sql = " select start_dir from fsdb_dirs where alias='"+alias+"'";
    	List<String[]> values = sqliteManager.query(sql);
    	if (values.size()==0) return;
    	
    	String path = values.get(0)[0];
    	List<String[]> params = new ArrayList<String[]>();
    	sql = "delete from fsdb_dirs where alias = ? ";
    	params.add(new String[] {alias});
    	sqliteManager.batchUpdate(sql, params);
    	
    	params.clear();
    	params.add(new String[] {path});
    	sql = "delete from fsdb_files where start_dir = ? ";
    	sqliteManager.batchUpdate(sql, params);
    	
    	fileSystemWatcher.removeWatch(path);
    }
    
    public void indexDir(String alias, String dir,String excludes, int depth) {
    	RecordCollector recordCollector=new RecordCollector();
    	List<Record> records=recordCollector.collect(dir, excludes);
    	List<String[]> values = new ArrayList<String[]>();
    	for (Record record : records) {
    		String[] valueArray = new String[3];
    		valueArray[0] = record.getName();
    		valueArray[1] = record.getStartDir();
    		valueArray[2] = record.getRelativePath();
    		values.add(valueArray);
    	}
    	String sql = "insert into fsdb_files (name,start_dir,rtl_path) values(?,?,?)";
    	sqliteManager.batchUpdate(sql, values);
    	
    	sql = "insert into fsdb_dirs(alias,start_dir,excludes, depth) values(?,?,?,?)";
    	values = new ArrayList<String[]>();
    	values.add(new String[]{alias,dir,excludes,String.valueOf(depth)});
    	sqliteManager.batchUpdate(sql, values);
    	
    	fileSystemWatcher.addWatch(dir);
    	
    }
    
    public void addFileRecord(String root,String name) {
    	
    }
    
    public void removeFileRecord(String root,String name) {
    }
    
}
