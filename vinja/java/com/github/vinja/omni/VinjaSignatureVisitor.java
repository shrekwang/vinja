package com.github.vinja.omni;

import org.objectweb.asm.signature.SignatureVisitor;

public class VinjaSignatureVisitor extends SignatureVisitor {
	
	private VinjaClassVisitor vinjaClassVistor;
	private String signatureClassName;

	public VinjaSignatureVisitor(int paramInt,  VinjaClassVisitor vinjaClassVistor) {
		super(paramInt);
		this.vinjaClassVistor = vinjaClassVistor;
	}
	
	public void visitClassType(final String name) {
		signatureClassName = name;
		vinjaClassVistor.addInternalName(name);
	}

	public void visitInnerClassType(final String name) {
		signatureClassName = signatureClassName + "$" + name;
		vinjaClassVistor.addInternalName(signatureClassName);
	}
	


}
