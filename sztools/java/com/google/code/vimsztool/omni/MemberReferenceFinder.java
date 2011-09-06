package com.google.code.vimsztool.omni;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.objectweb.asm.ClassAdapter;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodAdapter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.commons.EmptyVisitor;
import org.objectweb.asm.commons.Method;

public class MemberReferenceFinder {
    private String targetClass;
    private Method targetMethod;

    private AppClassVisitor cv = new AppClassVisitor();

    private ArrayList<ReferenceLocation> locations = new ArrayList<ReferenceLocation>();

        
    public ArrayList<ReferenceLocation> getReferenceLocations() {
    	return this.locations;
    }

    private class AppMethodVisitor extends MethodAdapter {

        int line;

        public AppMethodVisitor() { super(new EmptyVisitor()); }

        public void visitMethodInsn(int opcode, String owner, String name, String desc) {
            if (owner.equals(targetClass)
                    && name.equals(targetMethod.getName())
                    && desc.equals(targetMethod.getDescriptor())) {
                locations.add(new ReferenceLocation(cv.className, cv.methodName, cv.methodDesc, cv.source, line));
            }
        }

        public void visitLineNumber(int line, Label start) {
            this.line = line;
        }

    }

    private class AppClassVisitor extends ClassAdapter {

        private AppMethodVisitor mv = new AppMethodVisitor();

        public String source;
        public String className;
        public String methodName;
        public String methodDesc;

        public AppClassVisitor() { super(new EmptyVisitor()); }

        public void visit(int version, int access, String name,
                          String signature, String superName, String[] interfaces) {
            className = name;
        }

        public void visitSource(String source, String debug) {
            this.source = source;
        }

        public MethodVisitor visitMethod(int access, String name, 
                                         String desc, String signature,
                                         String[] exceptions) {
            methodName = name;
            methodDesc = desc;

            return mv;
        }
    }


    public void findCallingMethodsInJar(String jarPath, String targetClass,
                                        String targetMethodDeclaration) throws Exception {

        this.targetClass = targetClass;
        this.targetMethod = Method.getMethod(targetMethodDeclaration);


        JarFile jarFile = new JarFile(jarPath);
        Enumeration<JarEntry> entries = jarFile.entries();

        while (entries.hasMoreElements()) {
            JarEntry entry = entries.nextElement();

            if (entry.getName().endsWith(".class")) {
                InputStream stream = new BufferedInputStream(jarFile.getInputStream(entry), 1024);
                ClassReader reader = new ClassReader(stream);
                reader.accept(cv, 0);
                stream.close();
            }
        }
    }
    
    public void findCallingMethodInDir(String dir,String targetClass, String memberDesc) throws Exception {

        this.targetClass = targetClass;
        this.targetMethod = Method.getMethod(memberDesc);

		File file = new File(dir);
		File[] classes = file.listFiles(); 
		for (File classFile : classes ) {
			if (classFile.isDirectory()) {
				findCallingMethodInDir(classFile.getAbsolutePath(),targetClass, memberDesc);
			} else {
				FileInputStream fs = new FileInputStream(classFile);
				ClassReader cr = new ClassReader(fs);
                cr.accept(cv, 0);
                fs.close();
			}
		}
	}


    public static void main( String[] args ) {
        try {
            MemberReferenceFinder app = new MemberReferenceFinder();

            String jar = "E:\\work\\vim-sztool\\sztools\\lib\\szjde.jar";
            String targetClass = "com/google/code/vimsztool/omni/ClassInfo";
            app.findCallingMethodsInJar(jar, targetClass,"String getName()");

            for (ReferenceLocation c : app.locations) {
                System.out.println(c.source+":"+c.line+" "+c.className+" "+c.methodName+" "+c.methodDesc);
            }

            System.out.println("--\n"+app.locations.size()+" methods invoke "+
                    app.targetClass+" "+
                    app.targetMethod.getName()+" "+app.targetMethod.getDescriptor());
        } catch(Exception x) {
            x.printStackTrace();
        }
    }
}
