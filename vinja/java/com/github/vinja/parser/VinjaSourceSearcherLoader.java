package com.github.vinja.parser;

import java.util.concurrent.Callable;

import com.github.vinja.compiler.CompilerContext;
import com.github.vinja.parser.VinjaJavaSourceSearcher.NameType;

public class VinjaSourceSearcherLoader  implements Callable<VinjaJavaSourceSearcher>  {

	private String name;
	private NameType nameType;
	private boolean parseImport;
	private int priority;
	private CompilerContext ctx;

	public VinjaSourceSearcherLoader(final String name, final CompilerContext ctx, final NameType nameType, boolean parseImport, VinjaSourceLoadPriority priority) {
		this.name = name;
		this.nameType = nameType;
		this.parseImport = parseImport;
		this.ctx = ctx;
		this.priority = priority.value;
	}

	public VinjaJavaSourceSearcher call() throws InterruptedException {
		String filename = name;
		if (nameType == NameType.CLASS) {
			filename = this.ctx.findSourceOrBinPath(name);
		}
		VinjaJavaSourceSearcher result = new VinjaJavaSourceSearcher(filename, this.ctx, parseImport);
		return result;
	}
	
	public int getPriority() {
		return this.priority;
	}
}
