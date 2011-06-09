package com.google.code.vimsztool.omni;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Attribute;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;
import org.objectweb.asm.signature.SignatureReader;
import org.objectweb.asm.signature.SignatureVisitor;

import com.google.code.vimsztool.compiler.CompilerContext;

public class ClassMetaInfoReader implements AnnotationVisitor, SignatureVisitor, ClassVisitor,
		FieldVisitor, MethodVisitor {

	private ClassInfo classInfo;
	private PackageInfo packageInfo;


	public ClassMetaInfoReader(ClassInfo classInfo,CompilerContext ctx) {
		this.classInfo = classInfo;
		this.packageInfo = ctx.getPackageInfo();
	}

	public void visit(int version, int access, String name, String signature, String superName,
			String[] interfaces) {

		classInfo.setName(name.replace("/", "."));
		if (superName != null) {
			classInfo.setSuperName(superName.replace("/", "."));
		}

		if (interfaces != null) {
			for (int i = 0; i < interfaces.length; i++) {
				interfaces[i] = interfaces[i].replace("/", ".");
			}
			classInfo.setInterfaces(interfaces);
		}
		
		if (signature == null) {
			if (superName != null) {
				addInternalName(superName);
			}
			addInternalNames(interfaces);
		} else {
			addSignature(signature);
		}

	}

	public void visitLineNumber(int lineNum, Label arg1) {
		classInfo.addLineNum(lineNum);
	}

	public void visitCode() {
	}


	public AnnotationVisitor visitAnnotation(final String desc, final boolean visible) {
		addDesc(desc);
		return this;
	}

	public void visitAttribute(final Attribute attr) {
	}

	public FieldVisitor visitField(final int access, final String name, final String desc,
			final String signature, final Object value) {
		if (signature == null) {
			addDesc(desc);
		} else {
			addTypeSignature(signature);
		}
		if (value instanceof Type) {
			addType((Type) value);
		}
		return this;
	}

	public MethodVisitor visitMethod(final int access, final String name, final String desc,
			final String signature, final String[] exceptions) {
		if (signature == null) {
			addMethodDesc(desc);
		} else {
			addSignature(signature);
		}
		addInternalNames(exceptions);
		return this;
	}

	public void visitSource(final String source, final String debug) {
	}

	public void visitInnerClass(final String name, final String outerName, final String innerName,
			final int access) {
	}

	public void visitOuterClass(final String owner, final String name, final String desc) {
	}

	public AnnotationVisitor visitParameterAnnotation(final int parameter, final String desc,
			final boolean visible) {
		addDesc(desc);
		return this;
	}

	public void visitTypeInsn(final int opcode, final String type) {
		addType(Type.getObjectType(type));
	}

	public void visitFieldInsn(final int opcode, final String owner, final String name,
			final String desc) {
		addInternalName(owner);
		addDesc(desc);
	}

	public void visitMethodInsn(final int opcode, final String owner, final String name,
			final String desc) {
		addInternalName(owner);
		addMethodDesc(desc);
	}

	public void visitLdcInsn(final Object cst) {
		if (cst instanceof Type) {
			addType((Type) cst);
		}
	}

	public void visitMultiANewArrayInsn(final String desc, final int dims) {
		addDesc(desc);
	}

	public void visitLocalVariable(final String name, final String desc, final String signature,
			final Label start, final Label end, final int index) {
		addTypeSignature(signature);
	}

	public AnnotationVisitor visitAnnotationDefault() {
		return this;
	}

	public void visitFrame(final int type, final int nLocal, final Object[] local,
			final int nStack, final Object[] stack) {
	}

	public void visitInsn(final int opcode) {
	}

	public void visitIntInsn(final int opcode, final int operand) {
	}

	public void visitVarInsn(final int opcode, final int var) {
	}

	public void visitJumpInsn(final int opcode, final Label label) {
	}

	public void visitLabel(final Label label) {
	}

	public void visitIincInsn(final int var, final int increment) {
	}

	public void visitTableSwitchInsn(final int min, final int max, final Label dflt,
			final Label[] labels) {
	}

	public void visitLookupSwitchInsn(final Label dflt, final int[] keys, final Label[] labels) {
	}

	public void visitTryCatchBlock(final Label start, final Label end, final Label handler,
			final String type) {
		if (type != null) {
			addInternalName(type);
		}
	}

	public void visitMaxs(final int maxStack, final int maxLocals) {
	}

	// AnnotationVisitor

	public void visit(final String name, final Object value) {
		if (value instanceof Type) {
			addType((Type) value);
		}
	}

	public void visitEnum(final String name, final String desc, final String value) {
		addDesc(desc);
	}

	public AnnotationVisitor visitAnnotation(final String name, final String desc) {
		addDesc(desc);
		return this;
	}

	public AnnotationVisitor visitArray(final String name) {
		return this;
	}

	// SignatureVisitor

	String signatureClassName;

	public void visitFormalTypeParameter(final String name) {
	}

	public SignatureVisitor visitClassBound() {
		return this;
	}

	public SignatureVisitor visitInterfaceBound() {
		return this;
	}

	public SignatureVisitor visitSuperclass() {
		return this;
	}

	public SignatureVisitor visitInterface() {
		return this;
	}

	public SignatureVisitor visitParameterType() {
		return this;
	}

	public SignatureVisitor visitReturnType() {
		return this;
	}

	public SignatureVisitor visitExceptionType() {
		return this;
	}

	public void visitBaseType(final char descriptor) {
	}

	public void visitTypeVariable(final String name) {
	}

	public SignatureVisitor visitArrayType() {
		return this;
	}

	public void visitClassType(final String name) {
		signatureClassName = name;
		addInternalName(name);
	}

	public void visitInnerClassType(final String name) {
		signatureClassName = signatureClassName + "$" + name;
		addInternalName(signatureClassName);
	}

	public void visitTypeArgument() {
	}

	public SignatureVisitor visitTypeArgument(final char wildcard) {
		return this;
	}

	public void visitEnd() { }


	private void addName(final String name) {
		if (name == null) {
			return;
		}
		String tmpClassName = name.replace("/", ".");
		if (packageInfo.isClassInDst(tmpClassName)) {
			classInfo.addDependent(tmpClassName);
		}
	}

	private void addInternalName(final String name) {
		addType(Type.getObjectType(name));
	}

	private void addInternalNames(final String[] names) {
		for (int i = 0; names != null && i < names.length; i++) {
			addInternalName(names[i]);
		}
	}

	private void addDesc(final String desc) {
		addType(Type.getType(desc));
	}

	private void addMethodDesc(final String desc) {
		addType(Type.getReturnType(desc));
		Type[] types = Type.getArgumentTypes(desc);
		for (int i = 0; i < types.length; i++) {
			addType(types[i]);
		}
	}

	private void addType(final Type t) {
		switch (t.getSort()) {
		case Type.ARRAY:
			addType(t.getElementType());
			break;
		case Type.OBJECT:
			addName(t.getInternalName());
			break;
		}
	}

	private void addSignature(final String signature) {
		if (signature != null) {
			new SignatureReader(signature).accept(this);
		}
	}

	private void addTypeSignature(final String signature) {
		if (signature != null) {
			new SignatureReader(signature).acceptType(this);
		}
	}

}
