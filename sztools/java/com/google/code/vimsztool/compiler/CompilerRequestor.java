
package com.google.code.vimsztool.compiler;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.core.compiler.IProblem;
import org.eclipse.jdt.internal.compiler.ClassFile;
import org.eclipse.jdt.internal.compiler.CompilationResult;
import org.eclipse.jdt.internal.compiler.ICompilerRequestor;

public class CompilerRequestor implements ICompilerRequestor {
	
	 private List<String> problemList ; 
	 private String outputDir;
	 
	 private static final String FIELD_SEPERATOR="::";
	 
	 public CompilerRequestor(String outputDir, List<String> problemList) {
		 this.outputDir=outputDir;
		 this.problemList=problemList;
	 }
	
    public void acceptResult(CompilationResult result) {
        try {
        	List<IProblem> errorList = new ArrayList<IProblem>();
            if (result.hasProblems()) {
                IProblem[] problems = result.getProblems();
                for (int i = 0; i < problems.length; i++) {
                	StringBuilder sb = new StringBuilder();
                    IProblem problem = problems[i];
                    if (problem.isError()) {
                    	sb.append("E").append(FIELD_SEPERATOR);
                    } else {
                    	sb.append("W").append(FIELD_SEPERATOR);
                    }
                    String filename=String.valueOf(problem.getOriginatingFileName());
                    sb.append(filename).append(FIELD_SEPERATOR);
                    sb.append(problem.getSourceLineNumber()).append(FIELD_SEPERATOR);
                    sb.append(problem.getMessage()).append("\n");
                    problemList.add(sb.toString());
                    
                    if (problem.isError()) {
                    	errorList.add(problem);
                    }
                }
            }
            if (errorList.isEmpty()) {
                ClassFile[] classFiles = result.getClassFiles();
                for (int i = 0; i < classFiles.length; i++) {
                    ClassFile classFile = classFiles[i];
                    char[][] compoundName = 
                        classFile.getCompoundName();
                    String className = "";
                    String sep = "";
                    for (int j = 0; 
                         j < compoundName.length; j++) {
                        className += sep;
                        className += new String(compoundName[j]);
                        sep = ".";
                    }
                    byte[] bytes = classFile.getBytes();
                    String outFile = outputDir + "/" + 
                        className.replace('.', '/') + ".class";
                    File parentFile=new File(outFile).getParentFile();
                    if (!parentFile.exists()) parentFile.mkdirs();
                    FileOutputStream fout = 
                        new FileOutputStream(outFile);
                    BufferedOutputStream bos = 
                        new BufferedOutputStream(fout);
                    bos.write(bytes);
                    bos.close();
                }
            }
        } catch (IOException exc) {
        	exc.printStackTrace();
        }
    }
}


