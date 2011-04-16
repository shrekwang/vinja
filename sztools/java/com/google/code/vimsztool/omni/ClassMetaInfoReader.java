package com.google.code.vimsztool.omni;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Attribute;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;

public class ClassMetaInfoReader  implements ClassVisitor , MethodVisitor {
	
	private ClassInfo classInfo ;
	
	public ClassMetaInfoReader(ClassInfo classInfo) {
		this.classInfo = classInfo;
	}
	
	
	public void visit(int version, int access, String name, String signature,
			String superName, String[] interfaces) {
		
		//if (name.indexOf("$") > -1) return;
		
		
		classInfo.setName(name.replace("/", "."));
		if (superName !=null) {
			classInfo.setSuperName(superName.replace("/", "."));
		}
		
		if (interfaces!=null) {
			for (int i=0; i<interfaces.length; i++) {
				interfaces[i] = interfaces[i].replace("/", ".");
			}
			classInfo.setInterfaces(interfaces);
		}
		
		
	}

	
	@Override
	public void visitLineNumber(int lineNum, Label arg1) {
		classInfo.addLineNum(lineNum);
	}
	
    public void visitEnd() {	}
	public void visitAttribute(Attribute attr) { }
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
