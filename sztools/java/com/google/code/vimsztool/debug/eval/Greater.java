package com.google.code.vimsztool.debug.eval;

import org.antlr.runtime.tree.CommonTree;

import com.google.code.vimsztool.exception.ExpressionEvalException;
import com.sun.jdi.ByteValue;
import com.sun.jdi.CharValue;
import com.sun.jdi.DoubleValue;
import com.sun.jdi.FloatValue;
import com.sun.jdi.IntegerValue;
import com.sun.jdi.LongValue;
import com.sun.jdi.ShortValue;

public class Greater {
	
	public static Object operate(CommonTree leftOp, CommonTree rightOp) {
		Object leftValue = ExpEval.evalTreeNode(leftOp);
		Object rightValue = ExpEval.evalTreeNode(rightOp);
		if ((leftValue instanceof Integer 
				|| leftValue instanceof Character
				|| leftValue instanceof Byte
				|| leftValue instanceof Short 
				|| leftValue instanceof IntegerValue 
				|| leftValue instanceof CharValue 
				|| leftValue instanceof ByteValue 
				|| leftValue instanceof ShortValue 
				)
				&& ( rightValue instanceof Integer
				 ||	rightValue instanceof Character 
				 ||	rightValue instanceof Byte 
				 ||	rightValue instanceof Short 
				 ||	rightValue instanceof IntegerValue 
				 ||	rightValue instanceof CharValue 
				 ||	rightValue instanceof ByteValue 
				 ||	rightValue instanceof ShortValue )
				 ) {
			
			int left =0; 
			int right = 0;
			if (leftValue instanceof Character ) {
				left =  ((Character)leftValue).charValue();
			} else if (leftValue instanceof CharValue) {
				left = ((CharValue)leftValue).charValue();
			} else {
				left = Integer.parseInt(leftValue.toString()) ;
			}
				
			if (rightValue instanceof Character) {
				right =  ((Character)rightValue).charValue();
			} else if (rightValue instanceof CharValue) {
				right = ((CharValue)rightValue).charValue();
			} else {
				right = Integer.parseInt(rightValue.toString());
			}
			return left > right;
		} 
		
		
		if (leftValue instanceof Long || leftValue instanceof LongValue) {
			return Long.parseLong(leftValue.toString()) 
					> Long.parseLong(rightValue.toString());
		}
		if (leftValue instanceof Float || leftValue instanceof FloatValue) {
			return Float.parseFloat(leftValue.toString()) 
					> Float.parseFloat(rightValue.toString());
		}
		if (leftValue instanceof Double || leftValue instanceof DoubleValue) {
			return Float.parseFloat(leftValue.toString()) 
					> Float.parseFloat(rightValue.toString());
		}
		throw new ExpressionEvalException("'>' operation can't be done.");
	}
}
