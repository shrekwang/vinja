package com.github.vinja.omni;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Attribute;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.TypePath;
import org.objectweb.asm.signature.SignatureReader;

import com.github.vinja.compiler.CompilerContext;

public class VinjaClassVisitor extends ClassVisitor {

	private ClassInfo classInfo;
	private PackageInfo packageInfo;


	public VinjaClassVisitor(ClassInfo classInfo,CompilerContext ctx) {
		super(Opcodes.ASM9);
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

	


	@Override
	public AnnotationVisitor visitAnnotation(final String desc, final boolean visible) {
		addDesc(desc);
		return new VinjaAnnotationVisitor(Opcodes.ASM9, super.visitAnnotation(desc, visible), this);
	}


	@Override
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
		return new VinjaFieldVisitor(Opcodes.ASM9, super.visitField(access, name, desc, signature, value), this);
	}

	@Override
	public MethodVisitor visitMethod(final int access, final String name, final String desc,
			final String signature, final String[] exceptions) {
		if (signature == null) {
			addMethodDesc(desc);
		} else {
			addSignature(signature);
		}
		addInternalNames(exceptions);
		return new VinjaMethodVisitor(Opcodes.ASM9, super.visitMethod(access, name, desc, signature, exceptions), this);
	}

	@Override
	public void visitSource(final String source, final String debug) {
		this.classInfo.setSourceName(source);
	}

	@Override
	public void visitInnerClass(final String name, final String outerName, final String innerName,
			final int access) {
		this.classInfo.addInnerClass(name.replace("/", "."));
	}

	@Override
	public void visitOuterClass(final String owner, final String name, final String desc) {
	}

	public void addName(final String name) {
		if (name == null) {
			return;
		}
		String tmpClassName = name.replace("/", ".");
		if (packageInfo.isClassInDst(tmpClassName)) {
			classInfo.addDependent(tmpClassName);
		}
	}

	public void addInternalName(final String name) {
		addType(Type.getObjectType(name));
	}

	public void addInternalNames(final String[] names) {
		for (int i = 0; names != null && i < names.length; i++) {
			addInternalName(names[i]);
		}
	}

	public void addDesc(final String desc) {
		addType(Type.getType(desc));
	}

	public void addMethodDesc(final String desc) {
		addType(Type.getReturnType(desc));
		Type[] types = Type.getArgumentTypes(desc);
		for (int i = 0; i < types.length; i++) {
			addType(types[i]);
		}
	}

	public void addType(final Type t) {
		switch (t.getSort()) {
		case Type.ARRAY:
			addType(t.getElementType());
			break;
		case Type.OBJECT:
			addName(t.getInternalName());
			break;
		}
	}
	
	public void addSignature(final String signature) {
		if (signature != null) {
			new SignatureReader(signature).accept(new VinjaSignatureVisitor(Opcodes.ASM9, this));
		}
	}
	
	public void addLineNubmer(int lineNum) {
		this.classInfo.addLineNum(lineNum);
	}
	

	public void addTypeSignature(final String signature) {
		if (signature != null) {
			new SignatureReader(signature).accept(new VinjaSignatureVisitor(Opcodes.ASM9, this));
		}
	}

	@Override
	public AnnotationVisitor visitTypeAnnotation(int typeRef, TypePath typePath, String desc, boolean visible) {
		return new VinjaAnnotationVisitor(Opcodes.ASM9, super.visitTypeAnnotation(typeRef, typePath, desc, visible),this);
	}

	@Override
	public void visitAttribute(Attribute attr) {
		super.visitAttribute(attr);
	}
	
	

}
