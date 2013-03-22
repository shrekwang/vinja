
package com.google.code.vimsztool.compiler;

import java.io.InputStream;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.eclipse.jdt.internal.compiler.Compiler;
import org.eclipse.jdt.internal.compiler.DefaultErrorHandlingPolicies;
import org.eclipse.jdt.internal.compiler.ICompilerRequestor;
import org.eclipse.jdt.internal.compiler.IErrorHandlingPolicy;
import org.eclipse.jdt.internal.compiler.IProblemFactory;
import org.eclipse.jdt.internal.compiler.env.ICompilationUnit;
import org.eclipse.jdt.internal.compiler.env.INameEnvironment;
import org.eclipse.jdt.internal.compiler.impl.CompilerOptions;
import org.eclipse.jdt.internal.compiler.problem.DefaultProblemFactory;


public class JDTCompiler  {
	private CompilerContext ctx;
	  
	  
	public JDTCompiler(CompilerContext ctx) {
		this.ctx=ctx;
	}
	
	public CompileResultInfo generateClass( final String[] sourceFiles ) {
		return this.generateClass(sourceFiles, null);
	}
	

	public CompileResultInfo generateClass( final String[] sourceFiles,PrintWriter out ) {
        
        String[] fileNames = sourceFiles;
        String[] classNames = new String[sourceFiles.length];
        
    	for (int i=0; i< sourceFiles.length; i++) {
	    	String targetClassName=ctx.buildClassName(sourceFiles[i]);
	        classNames[i] = targetClassName ;
    	}
        
        final INameEnvironment env = new NameEnvironment(sourceFiles,ctx) ;

        final IErrorHandlingPolicy policy = 
            DefaultErrorHandlingPolicies.proceedWithAllProblems();

        final Map<String,String> settings = new HashMap<String,String>();
        settings.put(CompilerOptions.OPTION_PreserveUnusedLocal, CompilerOptions.PRESERVE);
        settings.put(CompilerOptions.OPTION_LineNumberAttribute, CompilerOptions.GENERATE);
        settings.put(CompilerOptions.OPTION_SourceFileAttribute, CompilerOptions.GENERATE);
        settings.put(CompilerOptions.OPTION_LocalVariableAttribute, CompilerOptions.GENERATE);
        settings.put(CompilerOptions.OPTION_ReportDeprecation, CompilerOptions.GENERATE);
        if (ctx.getEncoding() != null) {
            settings.put(CompilerOptions.OPTION_Encoding, ctx.getEncoding());
        }
       

        // Source JVM
        if(ctx.getSrcVM() != null ) {
            settings.put(CompilerOptions.OPTION_Source, ctx.getSrcVM());
        } else {
            // Default to 1.5
            settings.put(CompilerOptions.OPTION_Source, CompilerOptions.VERSION_1_5);
        }
        
        // Target JVM
        if(ctx.getDstVM() != null ) {
            settings.put(CompilerOptions.OPTION_TargetPlatform, ctx.getDstVM());
        } else {
            // Default to 1.5
            settings.put(CompilerOptions.OPTION_TargetPlatform, CompilerOptions.VERSION_1_5);
            settings.put(CompilerOptions.OPTION_Compliance, CompilerOptions.VERSION_1_5);
        }

        final IProblemFactory problemFactory = 
            new DefaultProblemFactory(Locale.getDefault());
        
        CompileResultInfo compileResult = new CompileResultInfo();
        out.println("total " + fileNames.length + " files.");
        out.println("");
        final ICompilerRequestor requestor = new CompilerRequestor(ctx,compileResult, out);

        ICompilationUnit[] compilationUnits = 
            new ICompilationUnit[classNames.length];
        for (int i = 0; i < compilationUnits.length; i++) {
            String className = classNames[i];
            compilationUnits[i] = new CompilationUnit(fileNames[i], className,ctx.getEncoding());
        }
        CompilerOptions options = new CompilerOptions(settings);
        Compiler compiler = new Compiler(env, policy, options, requestor, problemFactory);
        compiler.compile(compilationUnits);
        
       
        
        return compileResult;
        
    }
    
    
}
