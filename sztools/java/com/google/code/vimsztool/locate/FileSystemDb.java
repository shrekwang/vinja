package com.google.code.vimsztool.locate;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import net.contentobjects.jnotify.JNotify;
import net.contentobjects.jnotify.JNotifyListener;

import org.apache.commons.io.FilenameUtils;

import com.google.code.vimsztool.util.JdeLogger;
import com.google.code.vimsztool.util.Preference;
import com.google.code.vimsztool.util.VjdeUtil;


public class FileSystemDb  implements JNotifyListener {
	private static Logger log = JdeLogger.getLogger("FileSystemDb");
	private SqliteManager sqliteManager ;
	private Map<String,WatchedDirInfo> watchedDir= new HashMap<String,WatchedDirInfo>();
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
				String errorMsg = VjdeUtil.getExceptionValue(e);
	    		log.info(errorMsg);
			}
			this.initTable();
		}
	}
	
	public static FileSystemDb getInstance() {
		return instance;
	}
	
	public void initWatchOnIndexedDir() {
		List<WatchedDirInfo> dirs =getIndexedDirs();
		for (WatchedDirInfo dirInfo : dirs) {
			addWatch(dirInfo);
		}
	}
		
	public void removeWatch(String path) {
		WatchedDirInfo dirInfo = watchedDir.get(path);
		if (dirInfo == null) return ;
		try {
			JNotify.removeWatch(dirInfo.getWatchId());
		} catch (Exception e) {
			String errorMsg = VjdeUtil.getExceptionValue(e);
    		log.info(errorMsg);
		}
		watchedDir.remove(path);
	}
	public void addWatch(WatchedDirInfo dirInfo) {
		
		String startDir = dirInfo.getStartDir();
		if (watchedDir.get(startDir)!=null ) return;
		
		int mask = JNotify.FILE_CREATED | JNotify.FILE_DELETED | JNotify.FILE_RENAMED;
		boolean watchSubtree = true;
		try {
			int watchId = JNotify.addWatch(startDir, mask, watchSubtree, this);
			dirInfo.setWatchId(watchId);
			watchedDir.put(startDir, dirInfo);
		} catch (Exception e) {
			String errorMsg = VjdeUtil.getExceptionValue(e);
    		log.info(errorMsg);
		}
	}


	
	public List<WatchedDirInfo> getIndexedDirs() {
		
		String sql = " select start_dir,alias,excludes,depth from fsdb_dirs ";
    	List<String[]> values = sqliteManager.query(sql);
    	List<WatchedDirInfo> result = new ArrayList<WatchedDirInfo>();
    	for (String[] valueArray : values ) {
    		WatchedDirInfo dirInfo = new WatchedDirInfo();
    		dirInfo.setStartDir(valueArray[0]);
    		dirInfo.setAlias(valueArray[1]);
    		dirInfo.setExcludes(valueArray[2]);
    		dirInfo.setDepth(Integer.parseInt(valueArray[3]));
    		result.add(dirInfo);
    	}
    	return result;
	}
  
   
    
    public void refreshIndex(String alias) {
    	String sql = " select start_dir,excludes,depth from fsdb_dirs where alias='"+alias+"'";
    	List<String[]> values = sqliteManager.query(sql);
    	if (values.size()==0) return;
    	String[] valueArray = values.get(0);
    	String startDir = valueArray[0];
    	String excludes = valueArray[1];
    	String depth = valueArray[2];
    	this.removeIndexedDir(alias);
    	addIndexedDir(alias, startDir, excludes, Integer.parseInt(depth));
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
    	
    	if (excludes == null ) {
    		excludes = defaultExcludes;
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
    	
    	WatchedDirInfo info = new WatchedDirInfo();
    	info.setAlias(alias);
    	info.setStartDir(dir);
    	info.setExcludes(excludes);
    	info.setDepth(depth);
    	addWatch(info);
    	
    }
	
    public void fileCreated(int wd, String rootPath, String name) {
    	String absPath = FilenameUtils.concat(rootPath, name);
    	String[] indexedData = getIndexedData(absPath);
    	if  (indexedData == null ) return ;
    	String startDir = indexedData[0];
    	String rtlPath = indexedData[1];
    	name = FilenameUtils.getName(name);
    	WatchedDirInfo watchedDirInfo=watchedDir.get(startDir);
    	
    	if (! PatternUtil.isExclude(watchedDirInfo.getExcludes(), new File(absPath))) {
	    	String sql = "insert into fsdb_files (name,start_dir,rtl_path) values(?,?,?)";
	    	List<String[]> values = new ArrayList<String[]>();
	    	values.add(new String[] {name,startDir,rtlPath});
	    	sqliteManager.batchUpdate(sql, values);
    	}
    	
    }
    
    public void fileDeleted(int wd, String rootPath, String name) {
    	String absPath = FilenameUtils.concat(rootPath, name);
    	String[] indexedData = getIndexedData(absPath);
    	if  (indexedData == null ) return ;
    	String startDir = indexedData[0];
    	String rtlPath = indexedData[1];
    	
    	String sql = "delete from fsdb_files where start_dir = ? and rtl_path = ? ";
    	List<String[]> values = new ArrayList<String[]>();
    	values.add(new String[] {startDir,rtlPath});
    	sqliteManager.batchUpdate(sql, values);
    }
    
    public void fileRenamed(int wd, String rootPath, String oldName, String newName) {
    	fileDeleted(wd, rootPath, oldName);
    	fileCreated(wd, rootPath, newName);
    }
    
	public void fileModified(int wd, String rootPath, String name) { 
	}
	
    private String[] getIndexedData(String absPath) {
    	Set<String> dirs = watchedDir.keySet();
    	String startDir = null;
    	for (String dir : dirs) {
    		if (absPath.startsWith(dir)) {
    			startDir = dir;
    		}
    	}
    	if (startDir == null) return null;
    	
    	int sepIndex = startDir.length() ;
    	if (! startDir.endsWith(File.separator)) {
    		sepIndex = startDir.length() + 1;
    	}
    	String rtlPath =  absPath.substring(sepIndex);
    	return new String[] {startDir,rtlPath};
    }
    
}
