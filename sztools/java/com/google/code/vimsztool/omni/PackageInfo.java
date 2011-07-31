package com.google.code.vimsztool.omni;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;

import com.google.code.vimsztool.compiler.CompilerContext;
import com.google.code.vimsztool.util.VjdeUtil;


public class PackageInfo {
	
	private Map<String,Set<String>> cache = new HashMap<String,Set<String>>();
	private Map<String, String> cachedPath = new HashMap<String,String>();
	
	private Set<String> dstClassNames = new HashSet<String>();
	
	
	public boolean isClassInDst(String className) {
		return dstClassNames.contains(className);
	}
	
	@SuppressWarnings("unchecked")
	public List<Class> findSubClass(CompilerContext ctx, String searchClassName) {
		ClassLoader classLoader = ctx.getClassLoader();
		List<Class> resultList = new ArrayList<Class>();
		for (String pkgname : cache.keySet()) {
			Set<String> classNames = cache.get(pkgname);
			if (pkgname.startsWith("java") || pkgname.startsWith("com.sun")) continue;
			for (String className : classNames) {
				String binClassName = pkgname+"."+className;
				Class aClass = null;
				try {
					aClass = classLoader.loadClass(binClassName);
				} catch (Throwable e) {
				}
				if (aClass == null)
					continue;
				LinkedList<Class> classList = ClassInfoUtil.getAllSuperClass(aClass);
				boolean isSuperior = false;
				for (Class tmpClass : classList) {
					if (tmpClass.getName().equals(className)) {
						isSuperior = true;
						break;
					}
				}
				if (isSuperior ) {
					resultList.add(aClass);
				}
			}
		}
		return resultList;
		
		
	}
	
	private Pattern getPattern(String name,boolean ignoreCase) {
        String patStr = name.replace("*",".*") + ".*";
        Pattern pattern = null;
        if (ignoreCase) {
	        pattern = Pattern.compile(patStr, Pattern.CASE_INSENSITIVE);
        } else {
	        pattern = Pattern.compile(patStr);
        }
        return pattern;
	}
	
	public List<String> findClass(String nameStart,boolean ignoreCase) {
		
		List<String> result = new ArrayList<String>();
        Pattern pattern = getPattern(nameStart,ignoreCase);
	        
		for (String pkgname : cache.keySet()) {
			Set<String> classNames = cache.get(pkgname);
			result.addAll(getMatchedClassName(classNames, nameStart, pattern, pkgname));
		}
		return result;
	}
	
	public List<String> findClassByQualifiedName(String name,boolean ignoreCase) {
		String[] splits = splitClassName(name);
		String pkgName = splits[0];
		String className = splits[1];
		Pattern pattern = getPattern(className, ignoreCase);
		Set<String> classNames = cache.get(pkgName);
		return getMatchedClassName(classNames, className, pattern, pkgName);
	}
	
	public List<String> getMatchedClassName(Set<String> set, String plainPat,
			Pattern pattern,String pkgName) {
		List<String> result = new ArrayList<String>();
		for (String className : set) {
			if ( pattern.matcher(className).matches()) {
				if (pkgName.equals("")) {
					result.add(className);
				} else {
					result.add(pkgName +"."+className);
				}
			}
		}
		return result;
	}
	
    
	private String[] splitClassName(String className) {
		int dotPos = className.lastIndexOf(".");
		if (dotPos < 0 ) return new String[] {"",className};
		return new String[] {className.substring(0,dotPos) , className.substring(dotPos+1)};
	}
	
	public List<String> findPackage(String className) {
		List<String> result = new ArrayList<String>();
		for (String pkgname : cache.keySet()) {
			if (cache.get(pkgname).contains(className)) {
				result.add(pkgname+"."+className);
			}
		}
		return result;
	}
	
	public void addClassNameToCache(String className) {
		//don't cache class under sun or com.sun package
		//if (className == null || className.startsWith("sun") || className.startsWith("com.sun")) return ;
		if (className == null) return;
		String[] tokens = className.split("\\.");
		if (tokens == null ) return;
		if (tokens.length < 2) {
			
			Set<String> names=cache.get("");
			if (names == null) {
				names=new HashSet<String>();
				cache.put("", names);
			}
			names.add(tokens[0]);
			return;
		}
		
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
	public void cacheClassNameInDist(String outputDir) {
		File dir = new File(outputDir) ;
		if (!dir.exists()) {
			boolean suc = dir.mkdirs();
			if (!suc) return;
		}
		if (!dir.isDirectory()) {
			return;
		}
		Iterator it=FileUtils.iterateFiles(dir, new String[] {"class"}	,true);
		while (it.hasNext()) {
			File file = (File)it.next();
			String relativeName = file.getAbsolutePath().substring(outputDir.length()+1);
			String className = relativeName.replace('/', '.').replace('\\', '.').replace(".class", "");
		    addClassNameToCache(className);
		    dstClassNames.add(className);
		}
	}
	
	public void cacheClassNameInJar(String path) {
		File file = new File(path);
		if (! file.exists()) return;
		String hadCached=cachedPath.get(path);
		if (hadCached !=null && hadCached.equals("true")) return;
		 JarFile jarFile = null;
		try {
			 jarFile = new JarFile(path);         
	         Enumeration<JarEntry> entries = jarFile.entries();
	         while(entries.hasMoreElements()) {
	             JarEntry entry = entries.nextElement();
	             String entryName = entry.getName();
	             if (entryName.indexOf("$") > -1) continue;
	             if (!entryName.endsWith(".class")) continue;
	             String className = entryName.replace('/', '.').replace('\\', '.').replace(".class", "");
	             addClassNameToCache(className);
	         }
	         jarFile.close();
		} catch (IOException e ) {
		} finally {
			if (jarFile !=null ) try {jarFile.close();} catch (Exception e) {};
		}
		cachedPath.put(path, "true");
		return ;
	}
	
	public void  cacheSystemRtJar() {
		String javaHome=System.getProperty("java.home");
		File rtjarFile=new File(FilenameUtils.concat(javaHome, "jre/lib/rt.jar"));
		if (! rtjarFile.exists())
			rtjarFile=new File(FilenameUtils.concat(javaHome, "lib/rt.jar"));
		if (! rtjarFile.exists()) return ;
		cacheClassNameInJar(rtjarFile.getPath());
	}
	 

	public List<String> getClassesForPackage(String pkgname,ClassLoader classLoader) {
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
