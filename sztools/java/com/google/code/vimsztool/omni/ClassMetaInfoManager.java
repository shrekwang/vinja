package com.google.code.vimsztool.omni;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.objectweb.asm.ClassReader;

public class ClassMetaInfoManager  {
	
	private Map<String,ClassInfo> metaInfos = new ConcurrentHashMap<String,ClassInfo>();
	
	
	public void cacheAllInfo(String dir) {
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
	
	public void loadSingleMetaInfo(File classFile) {
		if  (classFile.getName().endsWith(".class") ) {
			try {
				FileInputStream fs = new FileInputStream(classFile);
				ClassReader cr = new ClassReader(fs);
				ClassInfo classInfo = new ClassInfo();
				ClassMetaInfoReader classInfoReader = new ClassMetaInfoReader(classInfo);
				cr.accept(classInfoReader, 0);
				fs.close();
				if (metaInfos.get(classInfo.getName()) != null) {
					ClassInfo oldInfo = metaInfos.get(classInfo.getName());
					ClassInfo superInfo = metaInfos.get(oldInfo.getSuperName());
					if (superInfo != null) {
					   superInfo.getSubNames().remove(classInfo.getName());
					}
				}
				metaInfos.put(classInfo.getName(), classInfo);
			} catch (IOException e) {
			}
		}
	}
	
	public ClassInfo getMetaInfo(String className) {
		ClassInfo metaInfo = metaInfos.get(className);
		return metaInfo;
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
		
		for (String subName : metaInfo.getSubNames()) {
			splitNames = splitClassName(subName);
			pkgName = splitNames[0];
			binName = splitNames[1];
			result.add(emptySpace.substring(0,(offsetBase+1)*4) + binName + " - " + pkgName);
		}
		
		return result;
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
