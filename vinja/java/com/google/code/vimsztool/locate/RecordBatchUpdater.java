package com.google.code.vimsztool.locate;

import java.util.ArrayList;
import java.util.Iterator;
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
	
	public synchronized void addCreatedRecord(Record record) {
		createdRecords.add( new String[] {
				record.getName(), record.getStartDir(), record.getRelativePath()});
	}
	
	public synchronized void addDeletedRecord(Record record) {
		deletedRecords.add(new String[] {
				record.getStartDir(), record.getRelativePath() 
		});
		
		for (Iterator<String[]> it=createdRecords.iterator(); it.hasNext(); ) {
			String[] values = it.next();
			if (record.getStartDir().equals(values[1]) 
					&& record.getRelativePath().equals(values[2])) {
				it.remove();
			}
			
		}
	}
	
	private synchronized void updateDb() {
		if (deletedRecords.size() > 0) sqliteManager.batchUpdate(deleteSql, deletedRecords);
		if (createdRecords.size() > 0) sqliteManager.batchUpdate(insertSql	, createdRecords);
		
		createdRecords.clear();
		deletedRecords.clear();
		
	}

	public  void run() {
		updateDb();
	}

}
