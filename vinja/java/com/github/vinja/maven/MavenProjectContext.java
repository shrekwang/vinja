package com.github.vinja.maven;

import com.github.vinja.compiler.CompilerContext;
import java.util.HashMap;
import java.util.Map;

public class MavenProjectContext {


	private Map<String,CompilerContext> context = new HashMap<String,CompilerContext>();


    public String getProjectLocation(String name) {
       CompilerContext compilerCtx = context.get(name);
       if (compilerCtx == null ) {
           return null;
       }
       return compilerCtx.getProjectRoot();
    }

}
