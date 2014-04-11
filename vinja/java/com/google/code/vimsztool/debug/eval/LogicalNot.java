package com.google.code.vimsztool.debug.eval;

import org.antlr.runtime.tree.CommonTree;

import com.google.code.vimsztool.exception.ExpressionEvalException;
import com.sun.jdi.BooleanValue;

public class LogicalNot {
	
	public static Object operate(ExpEval expEval, CommonTree leftOp) {
		Object leftValue = expEval.evalTreeNode(leftOp);
		Boolean lv = null; 
		
		if (leftValue instanceof BooleanValue 
				|| leftValue instanceof Boolean ){
			lv = Boolean.parseBoolean(leftValue.toString());
			return !lv ;
		} 
		throw new ExpressionEvalException("'!' operation can't be done.");
	}
}
