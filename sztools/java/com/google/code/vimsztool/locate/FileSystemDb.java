package com.google.code.vimsztool.locate;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.contentobjects.jnotify.JNotify;
import net.contentobjects.jnotify.JNotifyListener;

import org.apache.commons.io.FilenameUtils;

import com.google.code.vimsztool.util.Preference;
import com.google.code.vimsztool.util.VjdeUtil;


public class FileSystemDb  implements JNotifyListener {
	
	private SqliteManager sqliteManager ;
	private Map<String,String> watchedDir= new HashMap<String,String>();
	private static final FileSystemDb instance = new FileSystemDb();
  	private Preference pref =  Preference.getInstance();
	
  	private void initTable() {
         String sql1 = " create table fsdb_dirs  ( integer primary key, alias varchar(60), "
 	        +"start_dir varchar(160) , excludes varchar(500) , depth integer ) ";
         String sql2 = " create table fsdb_files ( integer primary key ,  start_dir varchar(200), "
             +" name varchar(80),   rtl_path varchar(160) )  ";
         sqliteManager.executeUpdate(sql1);
         sqliteManager.executeUpdate(sql2);
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
	
	public static FileSystemDb getInstance() {
		return instance;
	}
	
	public void initWatchOnIndexedDir() {
		List<String> dirs =getIndexedDirs();
		for (String dir : dirs) {
			addWatch(dir);
		}
	}
		
	public void removeWatch(String path) {
		String IdStr = watchedDir.get(path);
		if (IdStr == null) return ;
		try {
			JNotify.removeWatch(Integer.parseInt(IdStr));
		} catch (Exception e) {
		}
		watchedDir.remove(path);
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


	
	public List<String> getIndexedDirs() {
		String sql = " select start_dir from fsdb_dirs ";
    	List<String[]> values = sqliteManager.query(sql);
    	List<String> result = new ArrayList<String>();
    	for (String[] valueArray : values ) {
    		result.add(valueArray[0]);
    	}
    	return result;
	}
  
   
    
    public void refreshIndex(String alias) {
    	//todo
    }
    
    public String listIndexedDir() {
    	String sql = " select alias,start_dir from fsdb_dirs ";
    	List<String[]> values = sqliteManager.query(sql);
    	if ( values.size() == 0 ) {
    		return "there's no indexed dir yet.";
    	}
    	StringBuilder sb = new StringBuilder();
    	for (String[] valueArray : values ) {
    		sb.append(valueArray[0]).append("\t");
    		sb.append(valueArray[1]).append("\n");
    	}
    	return sb.toString();
    }
    
    public boolean alreadyIndexed(String alias, String path) {
		String sql = " select count(*) from fsdb_dirs where alias='"
			+alias+"' or start_dir ='"+path+"'";
    	List<String[]> values = sqliteManager.query(sql);
    	String countStr = values.get(0)[0];
    	if (!countStr.equals("0")) return true;
    	return false;
    }
    
    public void removeIndexedDir(String alias) {
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
    	
    	removeWatch(path);
    }
    
    public void addIndexedDir(String alias, String dir,String excludes, int depth) {
    	RecordCollector recordCollector=new RecordCollector();
    	String defaultExcludes = pref.getValue(Preference.DEFAULT_EXCLUDE_PATTERN);
    	
    	if (defaultExcludes != null) {
    		if (excludes !=null) {
    			excludes =defaultExcludes+","+excludes;
    		} else {
    			excludes = defaultExcludes;
    		}
    	}
    	
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
    	
    	addWatch(dir);
    	
    }
	
    public void fileCreated(int wd, String rootPath, String name) {
    	String[] indexedData = getIndexedData(rootPath, name);
    	if  (indexedData == null ) return ;
    	String startDir = indexedData[0];
    	String rtlPath = indexedData[1];
    	
    	String sql = "insert into fsdb_files (name,start_dir,rtl_path) values(?,?,?)";
    	List<String[]> values = new ArrayList<String[]>();
    	values.add(new String[] {name,startDir,rtlPath});
    	sqliteManager.batchUpdate(sql, values);
    	
    }
    
    public void fileDeleted(int wd, String rootPath, String name) {
    	String[] indexedData = getIndexedData(rootPath, name);
    	if  (indexedData == null ) return ;
    	String startDir = indexedData[0];
    	String rtlPath = indexedData[1];
    	
    	String sql = "delete from fsdb_files where start_dir = ? and rtl_path = ? ";
    	List<String[]> values = new ArrayList<String[]>();
    	values.add(new String[] {startDir,rtlPath});
    	sqliteManager.batchUpdate(sql, values);
    }
    
    public void fileRenamed(int wd, String rootPath, String oldName, String newName) { }
	public void fileModified(int wd, String rootPath, String name) { }
	
    private String[] getIndexedData(String root,String name) {
    	List<String> dirs = getIndexedDirs();
    	String startDir = null;
    	for (String dir : dirs) {
    		if (root.startsWith(dir)) {
    			startDir = dir;
    		}
    	}
    	if (startDir == null) return null;
    	String absPath = FilenameUtils.concat(root, name);
    	
    	int sepIndex = startDir.length() ;
    	if (! startDir.endsWith(File.separator)) {
    		sepIndex = startDir.length() + 1;
    	}
    	String rtlPath =  absPath.substring(sepIndex);
    	return new String[] {startDir,rtlPath};
    }
    
}
