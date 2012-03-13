package com.google.code.vimsztool.debug.eval;

import org.antlr.runtime.tree.CommonTree;

import com.sun.jdi.BooleanValue;

public class LogicalOr {
	
	public static Object operate(CommonTree leftOp, CommonTree rightOp) {
		Object leftValue = ExpEval.evalTreeNode(leftOp);
		Object rightValue = ExpEval.evalTreeNode(rightOp);
		Boolean lv = null; 
		Boolean rv = null; 
		
		if (leftValue instanceof BooleanValue 
				|| leftValue instanceof Boolean ){
			lv = Boolean.parseBoolean(leftValue.toString());
		} 
		if (rightValue instanceof BooleanValue 
				|| rightValue instanceof Boolean ){
			rv = Boolean.parseBoolean(rightValue.toString());
		}
		return lv ||  rv;
	}
}
