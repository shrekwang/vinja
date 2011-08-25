package com.google.code.vimsztool.omni;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.objectweb.asm.ClassReader;

import com.google.code.vimsztool.compiler.CompilerContext;

public class ClassMetaInfoManager  {
	
	private Map<String,ClassInfo> metaInfos = new ConcurrentHashMap<String,ClassInfo>();
	
	private CompilerContext ctx;
	
	
	public void cacheAllInfo(String dir,CompilerContext ctx) {
		this.ctx = ctx;
		loadAllMetaInfo(dir);
		constructAllSubNames();
	}
	
	public void loadAllMetaInfo(String dir) {
		File file = new File(dir);
		File[] classes = file.listFiles(); 
		for (File classFile : classes ) {
			if (classFile.isDirectory()) {
				loadAllMetaInfo(classFile.getAbsolutePath());
			} else {
				loadSingleMetaInfo(classFile);
			}
		}
	}
	
	public void loadMetaInfoInJar(String jarPath) {
		File file = new File(jarPath);
		if (! file.exists()) return;
		 JarFile jarFile = null;
		try {
			 jarFile = new JarFile(jarPath);         
	         Enumeration<JarEntry> entries = jarFile.entries();
	         while(entries.hasMoreElements()) {
	             JarEntry entry = entries.nextElement();
	             if (!entry.getName().endsWith(".class")) continue;
	             InputStream is = jarFile.getInputStream(entry);
	             loadSingleMetaInfo(is);
	         }
		} catch (IOException e ) {
		} finally {
			if (jarFile !=null ) try {jarFile.close();} catch (Exception e) {};
		}
		return ;
	}
	
	public void loadSingleMetaInfo(InputStream stream) {
		try {
			ClassReader cr = new ClassReader(stream);
			loadSingleMetaInfo(cr);
		} catch (Throwable e) {
			e.printStackTrace();
		} finally {
			if (stream !=null ) try {stream.close();} catch (Exception e) {}
		}
	}
	
	public void loadSingleMetaInfo(File classFile) {
		if  (classFile.getName().endsWith(".class") ) {
			FileInputStream fs =null;
			try {
				fs = new FileInputStream(classFile);
				ClassReader cr = new ClassReader(fs);
				loadSingleMetaInfo(cr);
			} catch (IOException e) {
			} finally {
				if (fs!=null) { 
					try {fs.close();} catch (Exception e) {} 
				}
			}
		}
	}
	
	public void loadSingleMetaInfo(String className) {
		try {
			String classAsPath = className.replace('.', '/') + ".class";
			InputStream stream = ctx.getClassLoader().getResourceAsStream(classAsPath);
			ClassReader cr = new ClassReader(stream);
			stream.close();
			loadSingleMetaInfo(cr);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private void loadSingleMetaInfo(ClassReader cr) {
		ClassInfo classInfo = new ClassInfo();
		ClassMetaInfoReader classInfoReader = new ClassMetaInfoReader(
				classInfo, ctx);
		cr.accept(classInfoReader, 0);
		if (metaInfos.get(classInfo.getName()) != null) {
			ClassInfo oldInfo = metaInfos.get(classInfo.getName());
			ClassInfo superInfo = metaInfos.get(oldInfo.getSuperName());
			if (superInfo != null) {
				superInfo.getSubNames().remove(classInfo.getName());
			}
		}
		metaInfos.put(classInfo.getName(), classInfo);
	}
	
	public ClassInfo getMetaInfo(String className) {
		ClassInfo metaInfo = metaInfos.get(className);
		if (metaInfo ==null) {
			loadSingleMetaInfo(className);
			metaInfo = metaInfos.get(className);
		}
		return metaInfo;
	}
	
	public Set<String> getDependentClasses(String className) {
		Set<String> names = new HashSet<String>();
		for (ClassInfo metaInfo : metaInfos.values() ) {
			Set<String> dependentClasses = metaInfo.getDependents();
			if (dependentClasses.contains(className)) {
				names.add(metaInfo.getName());
				names.addAll(getAllSubName(metaInfo.getName()));
			}
		}
		return names;
	}
	
	public Set<String> getAllSubName(String className) {
		Set<String> result = new HashSet<String>();
		ClassInfo metaInfo = metaInfos.get(className);
		for (String subName : metaInfo.getSubNames()) {
			result.add(subName);
			result.addAll(getAllSubName(subName));
		}
		return result;
	}
	
	
	public List<String> getTypeHierarchy(String className) {
		List<String> result = new ArrayList<String>();
		if (className == null || metaInfos.get(className) == null) {
			result.add(className);
			return result;
		}
		ClassInfo metaInfo = metaInfos.get(className);
		
		List<String> superNames = new ArrayList<String>();
		String superName = metaInfo.getSuperName();
		while (superName !=null) {
			superNames.add(0, superName);
			ClassInfo superInfo = metaInfos.get(superName);
			if (superInfo == null ) break;
			superName = superInfo.getSuperName();
		}
		String emptySpace = "                                                       ";
		for (int i=0; i<superNames.size(); i++) {
			String[] splitNames = splitClassName(superNames.get(i));
			String pkgName = splitNames[0];
			String binName = splitNames[1];
			
			String displayName =  emptySpace.substring(0,i*4) + binName + " - " + pkgName;
			result.add(displayName);
		}
		
		String[] splitNames = splitClassName(className);
		String pkgName = splitNames[0];
		String binName = splitNames[1];
		int offsetBase = superNames.size();
		result.add(emptySpace.substring(0,offsetBase*4) + binName + " - " + pkgName);
		
		buildSubTypeHierarchy(metaInfo, result,offsetBase);
		
		return result;
	}
	
	private void buildSubTypeHierarchy(ClassInfo metaInfo,List<String> result,int offsetBase) {
		String emptySpace = "                                                       ";
		for (String subName : metaInfo.getSubNames()) {
			String[] splitNames = splitClassName(subName);
			String pkgName = splitNames[0];
			String binName = splitNames[1];
			result.add(emptySpace.substring(0,(offsetBase+1)*4) + binName + " - " + pkgName);
			ClassInfo subMetaInfo = metaInfos.get(subName);
			buildSubTypeHierarchy(subMetaInfo,result,offsetBase+1);
		}
	}
	
	private String[] splitClassName(String className) {
		int dotPos = className.lastIndexOf(".");
		if (dotPos < 0 ) return new String[] {"",className};
		return new String[] {className.substring(0,dotPos) , className.substring(dotPos+1)};
	}
	
	public void constructSubNames(String className) {
		ClassInfo metaInfo = metaInfos.get(className);
		String superName = metaInfo.getSuperName();

		if (superName != null) {
			ClassInfo superInfo = metaInfos.get(superName);
			if (superInfo != null) {
				superInfo.addSubName(metaInfo.getName());
			}
		}

		String[] interfaces = metaInfo.getInterfaces();
		if (interfaces != null) {
			for (int i = 0; i < interfaces.length; i++) {
				ClassInfo itfInfo = metaInfos.get(interfaces[i]);
				if (itfInfo != null) {
					itfInfo.addSubName(metaInfo.getName());
				}
			}
		}
	}
	
	public void constructAllSubNames() {

		for (String name : metaInfos.keySet()) {
			constructSubNames(name);
		}
	}
	


	

}
