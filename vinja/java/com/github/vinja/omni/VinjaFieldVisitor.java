package com.github.vinja.omni;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Attribute;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.TypePath;

public class VinjaFieldVisitor extends FieldVisitor {
	
	private VinjaClassVisitor vinjaClassVisitor;

	public VinjaFieldVisitor(int paramInt,VinjaClassVisitor vinjaClassVisitor ) {
		super(paramInt);
		this.vinjaClassVisitor = vinjaClassVisitor;
	}

	public VinjaFieldVisitor(int paramInt, FieldVisitor paramFieldVisitor, VinjaClassVisitor vinjaClassVisitor) {
		super(paramInt, paramFieldVisitor);
		this.vinjaClassVisitor = vinjaClassVisitor;
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
	
	
}
