package com.google.code.vimsztool.omni;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Attribute;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;

public class ClassMetaInfoManager  implements ClassVisitor {
	
	private Map<String,ClassMetaInfo> metaInfos = new ConcurrentHashMap<String,ClassMetaInfo>();
	
	public static void main(String[] args) throws Exception {
		
		long start = System.currentTimeMillis();
		ClassMetaInfoManager app = new ClassMetaInfoManager();
		app.cacheAllInfo("/project/scds/web/WEB-INF/classes/");
		ClassMetaInfo info =app.getTypeHierarchy("net.zdsoft.stusys.abnormal.dao.AbnormalApplyDao");
		System.out.println(info.name);
		System.out.println(info.superName);
		for (String sub : info.subNames) {
			System.out.println(sub);
		}
		
		long end = System.currentTimeMillis();
		System.out.println("takes " + (end-start));
		
		
	}
	
	public void cacheAllInfo(String dir) {
		loadAllMetaInfo(dir);
		constructSubNames();
	}
	
	private void loadAllMetaInfo(String dir) {
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
	
	private void loadSingleMetaInfo(File classFile) {
		if  (classFile.getName().endsWith(".class") ) {
			try {
				FileInputStream fs = new FileInputStream(classFile);
				ClassReader cr = new ClassReader(fs);
				cr.accept(this, 0);
				fs.close();
			} catch (IOException e) {
			}
		}
	}
	
	public ClassMetaInfo getTypeHierarchy(String className) {
		List<String> result = new ArrayList<String>();
		ClassMetaInfo metaInfo = metaInfos.get(className);
		return metaInfo;
	}
	
	
	private void constructSubNames() {

		for (String name : metaInfos.keySet()) {
			ClassMetaInfo metaInfo = metaInfos.get(name);
			String superName = metaInfo.superName;

			if (superName != null) {
				ClassMetaInfo superInfo = metaInfos.get(superName);
				if (superInfo != null) {
					superInfo.subNames.add(metaInfo.name);
				}
			}

			String[] interfaces = metaInfo.interfaces;
			if (interfaces != null) {
				for (int i = 0; i < interfaces.length; i++) {
					ClassMetaInfo itfInfo = metaInfos.get(interfaces[i]);
					if (itfInfo != null) {
						itfInfo.subNames.add(metaInfo.name);
					}
				}
			}

		}
	}
	
	public void visit(int version, int access, String name, String signature,
			String superName, String[] interfaces) {
		
		if (name.indexOf("$") > -1) return;
		
		ClassMetaInfo metaInfo = new ClassMetaInfo();
		metaInfo.name =  name.replace("/", ".");
		if (superName !=null) {
			metaInfo.superName = superName.replace("/", ".");
		}
		
		if (interfaces!=null) {
			for (int i=0; i<interfaces.length; i++) {
				interfaces[i] = interfaces[i].replace("/", ".");
			}
			metaInfo.interfaces = interfaces;
		}
		
		metaInfos.put(metaInfo.name, metaInfo);
		
		
	}

	public void visitAttribute(Attribute attr) { }
	public void visitEnd() { }
	public void visitSource(String source, String debug) { }
	public void visitOuterClass(String owner, String name, String desc) { }
	public void visitInnerClass(String name, String outerName, String innerName, int access) { }

	
	public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
		return null;
	}

	public FieldVisitor visitField(int access, String name, String desc,
			String signature, Object value) {
		return null;
	}

	public MethodVisitor visitMethod(int access, String name, String desc,
			String signature, String[] exceptions) {
		return null;
	}

	class ClassMetaInfo { 
		public String name;
		public String superName;
		public String[] interfaces;
		public List<String> subNames = new ArrayList<String>();
	}

}
