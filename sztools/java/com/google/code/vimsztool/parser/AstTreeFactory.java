package com.google.code.vimsztool.parser;

import org.antlr.runtime.ANTLRFileStream;
import org.antlr.runtime.ANTLRStringStream;
import org.antlr.runtime.CommonTokenStream;
import org.antlr.runtime.tree.CommonTree;

import com.google.code.vimsztool.util.LRUCache;

public class AstTreeFactory {
	
	public static LRUCache<String, ParseResult> expCache = new LRUCache<String,ParseResult>(120);
	
	public static ParseResult getExpressionAst(String exp) {
		ParseResult result = expCache.get(exp);
		if (result == null) {
			result = parseExpression(exp);
			expCache.put(exp, result);
		}
		return result;
	}
	
	
	private static ParseResult parseExpression(String exp) {
		ParseResult result = new ParseResult();
		try {
			JavaLexer lex = new JavaLexer(new ANTLRStringStream(exp));
			CommonTokenStream tokens = new CommonTokenStream(lex);
			JavaParser parser = new JavaParser(tokens);
			CommonTree tree = (CommonTree) parser.expression().getTree();
			result.setTree(tree);
			result.setError(false);
		} catch (Throwable e) {
			result.setError(true);
			result.setErrorMsg(e.getMessage());
		}
		return result;
	}
	
	public static ParseResult getJavaSourceAst(String fileName) {
		ParseResult result = new ParseResult();
		try {
			JavaLexer lex = new JavaLexer(new ANTLRFileStream(fileName));
			CommonTokenStream tokens = new CommonTokenStream(lex);
			JavaParser parser = new JavaParser(tokens);
			CommonTree tree = (CommonTree) parser.javaSource().getTree();
			result.setTree(tree);
			result.setError(false);
		} catch (Throwable e) {
			result.setError(true);
			result.setErrorMsg(e.getMessage());
		}
		return result;
	}
	
	

}
