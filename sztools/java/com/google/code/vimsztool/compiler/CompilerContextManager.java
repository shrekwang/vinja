package com.google.code.vimsztool.compiler;

import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;

public class CompilerContextManager {
	
	private static ConcurrentHashMap<String, Future<CompilerContext>> ctxCache = new ConcurrentHashMap<String, Future<CompilerContext>>();
	private static CompilerContextManager instance = new CompilerContextManager();
	private ExecutorService es = Executors.newSingleThreadExecutor();

	private CompilerContextManager () { }
	
	public static CompilerContextManager  getInstnace() {
		return instance;
	}
	
	public Future<CompilerContext> loadCompilerContext(final String classPathXml) {
		Future<CompilerContext> f = ctxCache.get(classPathXml);
		
        if (f == null) {
            Callable<CompilerContext> loader = new Callable<CompilerContext>() {
                public CompilerContext call() throws InterruptedException {
                    CompilerContext ctx = new CompilerContext(classPathXml);
                    return ctx;
                }
            };
            FutureTask<CompilerContext> ft = new FutureTask<CompilerContext>(loader);
            f = ctxCache.putIfAbsent(classPathXml, ft);
            if (f == null) { 
            	f = ft; 
            	es.submit(ft);
            }
        }
        return f;
	}
	
	public CompilerContext getCompilerContext(String classPathXml) {
		Future<CompilerContext> future=ctxCache.get(classPathXml);
		if (future ==null) {
			future=loadCompilerContext(classPathXml);
		}
		try {
			CompilerContext ctx = future.get();
			return ctx;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
	
	public void reloadCompilerContext(String classPathXml) {
		ctxCache.remove(classPathXml);
		loadCompilerContext(classPathXml);
	}
	

}
