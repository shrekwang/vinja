package com.google.code.vimsztool.debug;

import org.antlr.runtime.ANTLRStringStream;
import org.antlr.runtime.CommonTokenStream;
import org.antlr.runtime.tree.CommonTree;

import com.google.code.vimsztool.parser.JavaLexer;
import com.google.code.vimsztool.parser.JavaParser;
import com.sun.jdi.LongValue;
import com.sun.jdi.Value;

public class ExpEval {

	public static void main(String[] args) {
		ExpEval app = new ExpEval();
		String aa = app.eval("aa");
		aa = app.eval("22");
		System.out.print(aa);
	}

	public String eval(String exp) {
		try {
			JavaLexer lex = new JavaLexer(new ANTLRStringStream(exp));
			CommonTokenStream tokens = new CommonTokenStream(lex);
			JavaParser parser = new JavaParser(tokens);
			CommonTree tree = (CommonTree) parser.expression().getTree();
			Object result = evalTreeNode(tree);
			if (result == null)
				return null;
			return result.toString();
		} catch (Throwable e) {
			e.printStackTrace();
			return "error in eval expression.";
		}

	}

	private Object evalTreeNode(CommonTree node) {
		
		System.out.println(node.getText());
		CommonTree subNode = null;
		CommonTree leftOp = null;
		CommonTree rightOp = null;
		
		switch (node.getType()) {
		
		case JavaParser.PARENTESIZED_EXPR:
		case JavaParser.EXPR:
			subNode = (CommonTree) node.getChild(0);
			return evalTreeNode(subNode);
		case JavaParser.LOGICAL_NOT:
			subNode = (CommonTree) node.getChild(0);
			Object value = evalTreeNode(subNode);
			return ! (Boolean)value;
		case JavaParser.PLUS:
		case JavaParser.MINUS:
		case JavaParser.STAR:
		case JavaParser.DIV:
			
		case JavaParser.EQUAL:
		case JavaParser.NOT_EQUAL:
			
		case JavaParser.GREATER_THAN:
		case JavaParser.GREATER_OR_EQUAL:
		case JavaParser.LESS_THAN:
		case JavaParser.LESS_OR_EQUAL:
			
		case JavaParser.LOGICAL_AND:
		case JavaParser.LOGICAL_OR:
			
		case JavaParser.AND:
		case JavaParser.OR:
			leftOp = (CommonTree) node.getChild(0);
			rightOp = (CommonTree) node.getChild(1);
			return evalTreeNode(leftOp,rightOp,node.getType());
		case JavaParser.DECIMAL_LITERAL :
			return Integer.valueOf(node.getText());
		case JavaParser.FLOATING_POINT_LITERAL:
			return Float.valueOf(node.getText());
		default:
			return null;
		}
	}
	
	private Object evalTreeNode(CommonTree leftOp, CommonTree rightOp, int opType) {
		Object leftValue = evalTreeNode(leftOp);
		Object rightValue = evalTreeNode(rightOp);
		Object result = null;
		switch (opType) {
		case JavaParser.PLUS:
			result = ((Integer) leftValue) + ((Integer) rightValue);
			break;
		case JavaParser.MINUS:
			result = ((Integer) leftValue) - ((Integer) rightValue);
			break;
		case JavaParser.STAR:
			result = ((Integer) leftValue) * ((Integer) rightValue);
			break;
		case JavaParser.DIV:
			result = ((Integer) leftValue) / ((Integer) rightValue);
			break;
		case JavaParser.NOT_EQUAL:
			result = ((Integer) leftValue) != ((Integer) rightValue);
			break;
		case JavaParser.EQUAL:
			result = ((Integer) leftValue) == ((Integer) rightValue);
			break;
		case JavaParser.GREATER_THAN:
			result = ((Integer) leftValue) > ((Integer) rightValue);
			break;
		case JavaParser.GREATER_OR_EQUAL:
			result = ((Integer) leftValue) >= ((Integer) rightValue);
			break;
		case JavaParser.LESS_THAN:
			result = ((Integer) leftValue) < ((Integer) rightValue);
			break;
		case JavaParser.LESS_OR_EQUAL:
			result = ((Integer) leftValue) <= ((Integer) rightValue);
			break;
		case JavaParser.LOGICAL_AND:
			result = ((Boolean) leftValue) && ((Boolean) rightValue);
			break;
		case JavaParser.LOGICAL_OR:
			result = ((Boolean) leftValue) || ((Boolean) rightValue);
			break;
		case JavaParser.AND:
			result = ((Integer) leftValue) & ((Integer) rightValue);
			break;
		case JavaParser.OR:
			result = ((Integer) leftValue) | ((Integer) rightValue);
			break;
		}
		return result;
	}
	
	private Object operatePlus(CommonTree leftOp, CommonTree rightOp) {
		return null;
	}
	

}
