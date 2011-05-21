package com.google.code.vimsztool.locate;

import java.util.ArrayList;
import java.util.List;

public class RecordBatchUpdater implements Runnable {

	private List<String[]> createdRecords = new ArrayList<String[]>();
	private List<String[]> deletedRecords = new ArrayList<String[]>();
	private SqliteManager sqliteManager;
	private static final String insertSql = "insert into fsdb_files (name,start_dir,rtl_path) values(?,?,?)";
    private static final String deleteSql = "delete from fsdb_files where start_dir = ? and rtl_path = ? ";
    
	public RecordBatchUpdater(SqliteManager sqliteManager) {	
		this.sqliteManager = sqliteManager;

	}
	
	public synchronized void addCreatedRecords(String[] values) {
		createdRecords.add(values);
	}
	public synchronized void addDeletedRecords(String[] values) {
		deletedRecords.add(values);
	}
	
	private synchronized void updateDb() {
		if (createdRecords.size() > 0) sqliteManager.batchUpdate(insertSql	, createdRecords);
		if (deletedRecords.size() > 0) sqliteManager.batchUpdate(deleteSql, deletedRecords);
		
		createdRecords.clear();
		deletedRecords.clear();
		
	}

	public  void run() {
		updateDb();
	}

}
