package com.github.vinja.omni;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Attribute;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.TypePath;

public class VinjaMethodVisitor extends MethodVisitor {
	
	private VinjaClassVisitor vinjaClassVisitor;

	public VinjaMethodVisitor(int paramInt, MethodVisitor paramMethodVisitor, VinjaClassVisitor vinjaClassVisitor) {
		super(paramInt, paramMethodVisitor);
		this.vinjaClassVisitor = vinjaClassVisitor;
	}
	
	@Override
	public AnnotationVisitor visitParameterAnnotation(final int parameter, final String desc,
			final boolean visible) {
		vinjaClassVisitor.addDesc(desc);
		return super.visitParameterAnnotation(parameter, desc, visible);
	}

	@Override
	public void visitTypeInsn(final int opcode, final String type) {
		vinjaClassVisitor.addType(Type.getObjectType(type));
	}
	
	@Override
	public void visitFieldInsn(final int opcode, final String owner, final String name,
			final String desc) {
		vinjaClassVisitor.addInternalName(owner);
		vinjaClassVisitor.addDesc(desc);
	}


	@Override
	public void visitMethodInsn(final int opcode, final String owner, final String name,
			final String desc) {
		vinjaClassVisitor.addInternalName(owner);
		vinjaClassVisitor.addMethodDesc(desc);
	}

	@Override
	public void visitLdcInsn(final Object cst) {
		if (cst instanceof Type) {
			vinjaClassVisitor.addType((Type) cst);
		}
	}

	@Override
	public void visitMultiANewArrayInsn(final String desc, final int dims) {
		vinjaClassVisitor.addDesc(desc);
	}

	@Override
	public void visitLocalVariable(final String name, final String desc, final String signature,
			final Label start, final Label end, final int index) {
		vinjaClassVisitor.addTypeSignature(signature);
	}


	@Override
	public void visitTryCatchBlock(final Label start, final Label end, final Label handler,
			final String type) {
		if (type != null) {
			vinjaClassVisitor.addInternalName(type);
		}
	}

	@Override
	public void visitLineNumber(int lineNum, Label arg1) {
		this.vinjaClassVisitor.addLineNubmer(lineNum);
	}
	
	@Override
	public AnnotationVisitor visitAnnotation(final String desc, final boolean visible) {
		this.vinjaClassVisitor.addDesc(desc);
		return new VinjaAnnotationVisitor(Opcodes.ASM9, super.visitAnnotation(desc, visible), this.vinjaClassVisitor);
	}

	@Override
	public AnnotationVisitor visitTypeAnnotation(int typeRef, TypePath typePath, String desc, boolean visible) {
		return new VinjaAnnotationVisitor(Opcodes.ASM9, super.visitTypeAnnotation(typeRef, typePath, desc, visible),this.vinjaClassVisitor);
	}

	@Override
	public void visitAttribute(Attribute attr) {
		super.visitAttribute(attr);
	}

	@Override
	public AnnotationVisitor visitInsnAnnotation(int typeRef, TypePath typePath, String desc, boolean visible) {
		this.vinjaClassVisitor.addDesc(desc);
		return new VinjaAnnotationVisitor(Opcodes.ASM9, super.visitAnnotation(desc, visible), this.vinjaClassVisitor);
	}

	@Override
	public AnnotationVisitor visitLocalVariableAnnotation(int typeRef, TypePath typePath, Label[] start, Label[] end,
			int[] index, String desc, boolean visible) {
		this.vinjaClassVisitor.addDesc(desc);
		return new VinjaAnnotationVisitor(Opcodes.ASM9, super.visitAnnotation(desc, visible), this.vinjaClassVisitor);
	}
	
	
}
