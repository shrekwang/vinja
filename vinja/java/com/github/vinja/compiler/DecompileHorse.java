package com.github.vinja.compiler;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.commons.io.IOUtils;

import com.github.vinja.util.ConsoleDecompiler;
import com.github.vinja.util.Preference;
import com.github.vinja.util.VjdeUtil;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;

public class DecompileHorse {

    private static final String decompiledJarPath = "/Users/wangsn/.vinja/decompile"; 


    private ListeningExecutorService listeningExecutorService 
        = MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(10));

    private ConcurrentHashMap<String, ListenableFuture<String>> decompileCache 
        = new ConcurrentHashMap<String, ListenableFuture<String>>();

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
                    if (str.indexOf("=") > 0) {
                        String targetJarPath = str.substring(0, str.indexOf("="));
                        String decompiledJarPath = str.substring(str.indexOf("=")+1);
                        decompileCache.putIfAbsent(targetJarPath, Futures.immediateFuture(decompiledJarPath));
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void decompileJar(String targetJarPath, CompilerContext cc) {
        final ListenableFuture<String> decompileFuture = decompileCache.get(targetJarPath);
        if (decompileFuture == null ) {

            Callable<String> callable = new Callable<String>() {
                public String call() {
                	System.out.println("start decompile:" + targetJarPath);
                    String path=  ConsoleDecompiler.decompileJar(targetJarPath, decompiledJarPath);
					PrintWriter pw = null;
                    try {
						File decompileCacheFile = new File(vinjaDataHome, "decompile.cache");
						pw = new PrintWriter(new FileWriter(decompileCacheFile, true));
						pw.println(targetJarPath + "=" + path);
                    } catch (Exception e) {
                    	e.printStackTrace();
                    } finally {
                    	if (pw != null ) { pw.close(); }
                    }
                    return path;
                }
            };
            final ListenableFuture<String> future = listeningExecutorService.submit(callable);
            future.addListener(new Runnable() {
                public void run() {
                    try {
                        cc.appendLibSrcLocation(future.get());
                    } catch (Exception e) {
                    }
                }
            }, listeningExecutorService);

            decompileCache.put(targetJarPath, future);

        } else if (decompileFuture.isDone()) {
            try {
                String decompiledJarPath = decompileFuture.get();
                cc.appendLibSrcLocation(decompiledJarPath);
            } catch (Exception e) {
            }
        } else {
            decompileFuture.addListener(new Runnable() {
                public void run() {
                    try {
                        cc.appendLibSrcLocation(decompileFuture.get());
                    } catch (Exception e) {
                    }
                }
            }, listeningExecutorService);
        }
    }




}
