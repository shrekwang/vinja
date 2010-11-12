package com.google.code.vimsztool.omni;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;


public class PackageInfo {
	
	private static Map<String,Set<String>> cache = new HashMap<String,Set<String>>();
	private static Map<String, String> cachedPath = new HashMap<String,String>();
	
	
	
	public static List<String> findClass(String nameStart) {
		List<String> result = new ArrayList<String>();
		for (String pkgname : cache.keySet()) {
			Set<String> classNames = cache.get(pkgname);
			for (String className : classNames) {
				if (className.startsWith(nameStart)) {
					result.add(pkgname+"."+className);
				}
			}
		}
		return result;
	}
    
	
	public static List<String> findPackage(String className) {
		List<String> result = new ArrayList<String>();
		for (String pkgname : cache.keySet()) {
			if (cache.get(pkgname).contains(className)) {
				result.add(pkgname+"."+className);
			}
		}
		return result;
	}
	
	private static void addClassNameToCache(String className) {
		//don't cache class under sun or com.sun package
		if (className == null || className.startsWith("sun") 
				|| className.startsWith("com.sun")) return ;
		String[] tokens = className.split("\\.");
		if (tokens == null || tokens.length < 2 ) return;
		int count = 0;
		String key= tokens[0];
		while ( true ) {
			Set<String> names=cache.get(key);
			if (names == null) {
				names=new HashSet<String>();
				cache.put(key, names);
			}
			names.add(tokens[count+1]);
			count = count + 1;
			if (count >= tokens.length - 1 ) break;
			key = key+"."+tokens[count];
		}
	}
	
	@SuppressWarnings("unchecked")
	public static void cacheClassNameInDist(String outputDir) {
		Map<String,Set<String>> result = new HashMap<String,Set<String>>();
		File dir = new File(outputDir) ;
		Iterator it=FileUtils.iterateFiles(dir, new String[] {"class"}	,true);
		while (it.hasNext()) {
			File file = (File)it.next();
			String relativeName = file.getAbsolutePath().substring(outputDir.length()+1);
		    if (relativeName.indexOf("$") > -1) continue;
			String className = relativeName.replace('/', '.').replace('\\', '.').replace(".class", "");
		    addClassNameToCache(className);
		}
	}
	
	public static void cacheClassNameInJar(String path) {
		File file = new File(path);
		if (! file.exists()) return;
		String hadCached=cachedPath.get(path);
		if (hadCached !=null && hadCached.equals("true")) return;
		try {
			 JarFile jarFile = new JarFile(path);         
	         Enumeration<JarEntry> entries = jarFile.entries();
	         while(entries.hasMoreElements()) {
	             JarEntry entry = entries.nextElement();
	             String entryName = entry.getName();
	             if (entryName.indexOf("$") > -1) continue;
	             String className = entryName.replace('/', '.').replace('\\', '.').replace(".class", "");
	             addClassNameToCache(className);
	         }
		} catch (IOException e ) {
		}
		cachedPath.put(path, "true");
		return ;
	}
	
	public static void  cacheSystemRtJar() {
		String javaHome=System.getProperty("java.home");
		File rtjarFile=new File(FilenameUtils.concat(javaHome, "jre/lib/rt.jar"));
		if (! rtjarFile.exists())
			rtjarFile=new File(FilenameUtils.concat(javaHome, "lib/rt.jar"));
		if (! rtjarFile.exists()) return ;
		cacheClassNameInJar(rtjarFile.getPath());
	}
	 

	public static List<String> getClassesForPackage(String pkgname,ClassLoader classLoader) {
	    List<String> classNames=new ArrayList<String>();
	    File directory = null;
	    String fullPath;
	    String relPath = pkgname.replace('.', '/');
	    
	    if (cache.get(pkgname) !=null) {
	    	 Set<String> classNameSet=cache.get(pkgname);
            for (String name : classNameSet) {
            	classNames.add(name);
            }
            return classNames;
	    }

	    try {
		    Enumeration<URL> resources = classLoader.getResources((relPath));
		    if (resources == null) return null;
		    while (resources.hasMoreElements()) {
		    	URL resource = resources.nextElement();
			    fullPath = resource.getFile();
			    directory = new File(fullPath);
		
			    if (directory.exists()) {
			        File[] files = directory.listFiles();
			        for (int i = 0; i < files.length; i++) {
			        	String fileName=files[i].getName();
			        	if (files[i].isDirectory()) {
			        		classNames.add(fileName);
			        	} else if (fileName.endsWith(".class")) {
			                String className =  fileName.substring(0, fileName.length() - 6);
			                classNames.add(className);
			            }
			        }
			    }
			    else {
		            String jarPath = fullPath.replaceFirst("[.]jar[!].*", ".jar").replaceFirst("file:", "");
		            cacheClassNameInJar(jarPath);
		            Set<String> classNameSet=cache.get(pkgname);
		            for (String name : classNameSet) {
		            	classNames.add(name);
		            }
			    }
		    }
	    } catch (Exception e ) {
	    	
	    }
	    return classNames;
	}


}
