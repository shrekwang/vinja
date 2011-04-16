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
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;

public class ClassMetaInfoManager  implements ClassVisitor , MethodVisitor {
	
	private Map<String,ClassMetaInfo> metaInfos = new ConcurrentHashMap<String,ClassMetaInfo>();
	
	public void cacheAllInfo(String dir) {
		loadAllMetaInfo(dir);
		constructSubNames();
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
				cr.accept(this, 0);
				fs.close();
			} catch (IOException e) {
			}
		}
	}
	
	public List<String> getTypeHierarchy(String className) {
		List<String> result = new ArrayList<String>();
		if (className == null || metaInfos.get(className) == null) {
			result.add(className);
			return result;
		}
		ClassMetaInfo metaInfo = metaInfos.get(className);
		
		List<String> superNames = new ArrayList<String>();
		String superName = metaInfo.superName;
		while (superName !=null) {
			superNames.add(0, superName);
			ClassMetaInfo superInfo = metaInfos.get(superName);
			if (superInfo == null ) break;
			superName = superInfo.superName;
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
		
		for (String subName : metaInfo.subNames) {
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
		return this;
	}

	class ClassMetaInfo { 
		public String name;
		public String superName;
		public String[] interfaces;
		public List<String> subNames = new ArrayList<String>();
		public List<Integer> lineNums = new ArrayList<Integer>();
	}

	@Override
	public AnnotationVisitor visitAnnotationDefault() {
		return null;
	}

	@Override
	public void visitCode() { }

	@Override
	public void visitFieldInsn(int arg0, String arg1, String arg2, String arg3) {
	}

	@Override
	public void visitFrame(int arg0, int arg1, Object[] arg2, int arg3,
			Object[] arg4) {
	}

	@Override
	public void visitIincInsn(int arg0, int arg1) { }

	@Override
	public void visitInsn(int arg0) { }

	@Override
	public void visitIntInsn(int arg0, int arg1) { }

	@Override
	public void visitJumpInsn(int arg0, Label arg1) { }

	@Override
	public void visitLabel(Label arg0) { }

	@Override
	public void visitLdcInsn(Object arg0) { }

	@Override
	public void visitLineNumber(int arg0, Label arg1) { }

	@Override
	public void visitLocalVariable(String arg0, String arg1, String arg2,
			Label arg3, Label arg4, int arg5) {
	}

	@Override
	public void visitLookupSwitchInsn(Label arg0, int[] arg1, Label[] arg2) { }

	@Override
	public void visitMaxs(int arg0, int arg1) { }

	@Override
	public void visitMethodInsn(int arg0, String arg1, String arg2, String arg3) { }

	@Override
	public void visitMultiANewArrayInsn(String arg0, int arg1) { }

	@Override
	public AnnotationVisitor visitParameterAnnotation(int arg0, String arg1,
			boolean arg2) {
		return null;
	}

	@Override
	public void visitTableSwitchInsn(int arg0, int arg1, Label arg2, Label[] arg3) { }

	@Override
	public void visitTryCatchBlock(Label arg0, Label arg1, Label arg2, String arg3) { }

	@Override
	public void visitTypeInsn(int arg0, String arg1) { }

	@Override
	public void visitVarInsn(int arg0, int arg1) { }
	

}
