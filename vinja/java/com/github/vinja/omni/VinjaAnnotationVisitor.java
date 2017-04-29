package com.github.vinja.omni;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Type;

public class VinjaAnnotationVisitor extends AnnotationVisitor {
	
	private VinjaClassVisitor vinjaClassVisitor;


	public VinjaAnnotationVisitor(int paramInt, VinjaClassVisitor vinjaClassVisitor) {
		super(paramInt);
		this.vinjaClassVisitor = vinjaClassVisitor;
	}
	

	public VinjaAnnotationVisitor(int paramInt, AnnotationVisitor annotationVisitor, VinjaClassVisitor vinjaClassVisitor ) {
		super(paramInt, annotationVisitor);
		this.vinjaClassVisitor = vinjaClassVisitor;
	}


	@Override
	public void visit(final String name, final Object value) {
		if (value instanceof Type) {
			vinjaClassVisitor.addType((Type) value);
		}
	}

	@Override
	public void visitEnum(final String name, final String desc, final String value) {
		vinjaClassVisitor.addDesc(desc);
	}

	@Override
	public AnnotationVisitor visitAnnotation(final String name, final String desc) {
		vinjaClassVisitor.addDesc(desc);
		return super.visitAnnotation(name, desc);
	}


}
