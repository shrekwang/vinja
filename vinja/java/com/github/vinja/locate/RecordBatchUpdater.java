package com.github.vinja.locate;

import com.github.vinja.util.JdeLogger;
import java.io.File;
import org.apache.commons.io.FilenameUtils;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class RecordBatchUpdater implements Runnable {

	private static JdeLogger log = JdeLogger.getLogger("RecordBatchUpdater");

	private List<String[]> createdRecords = new ArrayList<String[]>();
	private List<String[]> deletedRecords = new ArrayList<String[]>();
	private SqliteManager sqliteManager;
	private static final String insertSql = "replace into fsdb_files (name,start_dir,rtl_path) values(?,?,?)";
    private static final String deleteSql = "delete from fsdb_files where start_dir = ? and rtl_path = ? ";
    
	public RecordBatchUpdater(SqliteManager sqliteManager) {	
		this.sqliteManager = sqliteManager;

	}
	
	public synchronized void addCreatedRecord(Record record) {
		createdRecords.add( new String[] {
				record.getName(), record.getStartDir(), record.getRelativePath()});
	}
	
	public synchronized void addDeletedRecord(Record record) {
		deletedRecords.add(new String[] {
				record.getStartDir(), record.getRelativePath() 
		});
	}
	
	private synchronized void updateDb() {
		List<String[]> existedPath = new ArrayList<String[]>();
        for (String[] arr : deletedRecords ) {
            String abPath = FilenameUtils.concat(arr[0],arr[1]);
            File file = new File(abPath);
            if (file.exists()) {
                log.info("file " + abPath + " existed in file system,shouldn't remove from index");
                existedPath.add(arr);
            }
        }
        deletedRecords.removeAll(existedPath);

        List<String[]> notExistedPath = new ArrayList<String[]>();
        for (String[] arr : createdRecords ) {
            String abPath = FilenameUtils.concat(arr[0],arr[1]);
            File file = new File(abPath);
            if (!file.exists()) {
                log.info("file " + abPath + " not existed in file system,shouldn't insert into index");
                notExistedPath.add(arr);
            }
        }
        createdRecords.removeAll(notExistedPath);


		if (deletedRecords.size() > 0) sqliteManager.batchUpdate(deleteSql, deletedRecords);
		if (createdRecords.size() > 0) sqliteManager.batchUpdate(insertSql	, createdRecords);
		
		createdRecords.clear();
		deletedRecords.clear();
		
	}

	public  void run() {
		try {
			updateDb();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
