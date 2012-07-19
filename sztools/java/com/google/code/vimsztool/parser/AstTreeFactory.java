package com.google.code.vimsztool.parser;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.antlr.runtime.ANTLRFileStream;
import org.antlr.runtime.ANTLRInputStream;
import org.antlr.runtime.ANTLRStringStream;
import org.antlr.runtime.CommonTokenStream;
import org.antlr.runtime.tree.CommonTree;

import com.google.code.vimsztool.util.LRUCache;

public class AstTreeFactory {
	
	public static LRUCache<String, ParseResult> expCache = new LRUCache<String,ParseResult>(120);
	
	private static final String DEFAULT_ENCODING="utf-8";
	
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
			CommonTree tree = (CommonTree) parser.expressionList().getTree();
			//if type is EXPR, the exp is just single expression
			//else this is a multiple expression seperated by ","
			if (tree.getType() == JavaParser.EXPR) {
				result.setTree(tree);
				result.setExp(exp);
			} else {
				List<CommonTree> trees = new ArrayList<CommonTree>();
				List<String> expList = new ArrayList<String>();
				int p = 0;
				for (int i=0; i<tree.getChildCount(); i++) {
					CommonTree c = (CommonTree)tree.getChild(i);
					trees.add(c);
					int tmp = findLeftMostIndex(c);
					if (i> 0) {
						String basicExp = exp.substring(p,tmp).trim();
						if (basicExp.endsWith(",")) basicExp = basicExp.substring(0,basicExp.length()-1);
						expList.add(basicExp);
					}
					p = tmp;
				}
				
				String basicExp = exp.substring(p);
				if (basicExp.endsWith(",")) basicExp = basicExp.substring(0,basicExp.length()-1);
				expList.add(basicExp);
				result.setTreeList(trees);
				result.setExpList(expList);
			}
			result.setError(false);
		} catch (Throwable e) {
			result.setError(true);
			result.setErrorMsg(e.getMessage());
		}
		return result;
	}
	
	public static int findLeftMostIndex(CommonTree t) {
        int start = t.getCharPositionInLine();
        for (int i=0; i< t.getChildCount(); i++) {
            CommonTree c = (CommonTree) t.getChild(i);
            int cs = findLeftMostIndex(c);
            if (start > cs) start = cs;
        }
        return start;
    }
	
	
	public static ParseResult getJavaSourceAst(String fileName) {
		return getJavaSourceAst(fileName,DEFAULT_ENCODING);
	}
	
	public static ParseResult getJavaSourceAst(String fileName,String encoding) {
		ParseResult result = new ParseResult();
		try {
			JavaLexer lex = new JavaLexer(new ANTLRFileStream(fileName,encoding));
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
	
	public static ParseResult getJavaSourceAst(InputStream is,String encoding) {
		ParseResult result = new ParseResult();
		try {
			JavaLexer lex = new JavaLexer(new ANTLRInputStream(is, encoding));
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
