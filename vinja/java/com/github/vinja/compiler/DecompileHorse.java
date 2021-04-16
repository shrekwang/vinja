package com.github.vinja.compiler;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.commons.io.IOUtils;

import com.alibaba.fastjson.JSONObject;
import com.github.vinja.util.VjdeUtil;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;

public class DecompileHorse {

    private static final String decompiledJarPath = "/Users/wangsn/.vinja/decompile"; 


    private ListeningExecutorService listeningExecutorService 
        = MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(10));

    private ConcurrentHashMap<String, ListenableFuture<DecompileCacheInfo>> decompileCache = new ConcurrentHashMap<>();

    private static DecompileHorse instance = null;

    private static Lock instanceCreateLock = new ReentrantLock();

	private static String vinjaDataHome = VjdeUtil.getToolDataHome();

    public static DecompileHorse getInstance() {
        synchronized (instanceCreateLock) {
            if (instance == null ) {
                instance = new DecompileHorse();
            }
            return instance;
        }
    }


    private DecompileHorse() {
        File decompileCacheFile = new File(vinjaDataHome, "decompile.cache");
        if (decompileCacheFile.exists()) {
            try {
                List<String> decompileList = IOUtils.readLines(new FileReader(decompileCacheFile));
                for (String str  :  decompileList ) {
                    if (str.indexOf("decompiledJarPath") > 0) {
                    	DecompileCacheInfo cacheInfo = JSONObject.parseObject(str, DecompileCacheInfo.class);
                        decompileCache.put(cacheInfo.getOriginalJarPath(), Futures.immediateFuture(cacheInfo));
                    }
                }
                
                //clean duplicate cache info by rewrite the whole cache
            	PrintWriter pw = null;
				try {
					pw = new PrintWriter(new FileWriter(decompileCacheFile));
					for (ListenableFuture<DecompileCacheInfo> future : decompileCache.values()) {
						DecompileCacheInfo cacheInfo = future.get();
						pw.println(JSONObject.toJSONString(cacheInfo));
					}
				} catch (Exception e) {
					e.printStackTrace();
				} finally {
					if (pw != null ) { pw.close(); }
				}

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void decompileJar(String targetJarPath, CompilerContext cc) {
        final ListenableFuture<DecompileCacheInfo> decompileFuture = decompileCache.get(targetJarPath);
        
        File targetJarFile = new File(targetJarPath);
        if (!targetJarFile.exists()) return;
        
        final long gmtModified = targetJarFile.lastModified();
        
        boolean notModifiedJar = true;
        try {
        	DecompileCacheInfo cacheInfo = decompileFuture.get();
        	notModifiedJar = cacheInfo.getOriginalJarGmtModified().longValue() == gmtModified;
        } catch (Exception e) {
        }
        
        	
        if (decompileFuture == null || ! notModifiedJar ) {

            Callable<DecompileCacheInfo> callable = new Callable<DecompileCacheInfo>() {
                public DecompileCacheInfo call() {
                    return null;
                }
            };
            final ListenableFuture<DecompileCacheInfo> future = listeningExecutorService.submit(callable);
            future.addListener(new Runnable() {
                public void run() {
                    try {
                    	DecompileCacheInfo cacheInfo = future.get();
                        cc.addDecompiledLibSrcLocation(targetJarPath, cacheInfo.getDecompiledJarPath());
                    } catch (Exception e) {
                    }
                }
            }, listeningExecutorService);

            decompileCache.put(targetJarPath, future);

        }
	}

}

class DecompileCacheInfo {

	private String decompiledJarPath ;
	private Long originalJarGmtModified;
	private String originalJarPath;

	public String getOriginalJarPath() {
		return originalJarPath;
	}
	public void setOriginalJarPath(String originalJarPath) {
		this.originalJarPath = originalJarPath;
	}
	public String getDecompiledJarPath() {
		return decompiledJarPath;
	}
	public void setDecompiledJarPath(String decompiledJarPath) {
		this.decompiledJarPath = decompiledJarPath;
	}
	public Long getOriginalJarGmtModified() {
		return originalJarGmtModified;
	}
	public void setOriginalJarGmtModified(Long originalJarGmtModified) {
		this.originalJarGmtModified = originalJarGmtModified;
	}

}
