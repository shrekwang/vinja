package com.github.vinja.parser;

import org.antlr.runtime.ANTLRFileStream;
import org.antlr.runtime.CommonTokenStream;
import org.antlr.runtime.tree.CommonTree;

public class ParserTimeTest {

	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {

		long start = System.currentTimeMillis();
		String fileName = "D:\\github\\mine\\jsearch\\src\\main\\java\\com\\github\\jsearch\\JavaSourceSearch.java";
		JavaLexer lex = new JavaLexer(new ANTLRFileStream(fileName,"utf-8"));
		CommonTokenStream tokens = new CommonTokenStream(lex);
		JavaParser parser = new JavaParser(tokens);
		CommonTree tree = (CommonTree) parser.javaSource().getTree();
		long end = System.currentTimeMillis();
		
		System.out.println("takes " + (end- start) + "ms");
	}

}
