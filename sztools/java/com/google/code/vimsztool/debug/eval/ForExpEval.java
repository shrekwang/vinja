package com.google.code.vimsztool.debug.eval;

import java.util.ArrayList;
import java.util.List;

import org.antlr.runtime.tree.CommonTree;

import com.google.code.vimsztool.parser.AstTreeFactory;
import com.google.code.vimsztool.parser.ParseResult;
import com.sun.jdi.ArrayType;
import com.sun.jdi.ClassType;
import com.sun.jdi.InterfaceType;
import com.sun.jdi.ObjectReference;
import com.sun.jdi.Value;

public class ForExpEval {
	
	
	private String colExp = null;
	private String itemName = null;
	private String evalExp = null;
	private String testExp = null;
	private ExpEval expEval = null;
	
	private int colStartIdx = 0;
	private int colEndIdx = 999;
	
	public String eval(ExpEval expEval, String exp) {
		this.expEval = expEval;
		doSimpleParse(exp);
		String result = evalForExp();
		return result;
	}
	
	private int parseInt(String str, int defaultValue) {
		str = str.trim();
		if (str.equals("")) return defaultValue;
		return Integer.parseInt(str);
	}
	
	private void doSimpleParse(String exp) {
		exp = exp.trim().substring(1, exp.length()-1);
		evalExp = exp.substring(0, exp.indexOf(" for "));
		itemName = exp.substring(exp.indexOf(" for ") + 5 , exp.indexOf(" in ")).trim();
		int ifIdx = exp.indexOf(" if ");
		if (ifIdx < 0 ) {
			colExp = exp.substring(exp.indexOf(" in ") + 4 );
		} else {
			colExp = exp.substring(exp.indexOf(" in ") + 4, ifIdx);
			testExp = exp.substring(ifIdx + 4 );
		}
		colExp = colExp.trim();
		if (colExp.indexOf("[") > 0) {
			String idxExp = colExp.substring(colExp.indexOf("[")+1, colExp.length()-1);
			String[] arr = idxExp.split(":");
			if (arr.length == 1) {
				colStartIdx = parseInt(arr[0], 0);
			} else {
				colStartIdx = parseInt(arr[0], 0);
				colEndIdx = parseInt(arr[1], 999);
			}
			colExp = colExp.substring(0, colExp.indexOf("["));
		}
		
	}
	
	
	
	private String evalForExp() {
		
		ParseResult parseResult = AstTreeFactory.getExpressionAst(colExp);
		CommonTree node = parseResult.getTreeList().get(0);
		Object value = expEval.evalTreeNode(node);
		
		boolean iterable = false;
		if (value instanceof ObjectReference) {
			ObjectReference objRef = (ObjectReference)value;
			if (objRef.referenceType() instanceof ClassType) {
				ClassType classType = (ClassType)objRef.referenceType();
				for (InterfaceType itfType : classType.allInterfaces()) {
					if (itfType.name().equals("java.util.Collection")) {
						iterable = true;
						break;
					}
				}
			}
			if (objRef.referenceType() instanceof ArrayType) {
				//FIXME add arrayType iterate support in iterateValue();
				System.out.println("yes,arraytype");
			}
			if (objRef.referenceType() instanceof InterfaceType) {
				InterfaceType itfType = (InterfaceType)objRef.referenceType();
				for (InterfaceType superType : itfType.superinterfaces()) {
					if (superType.name().equals("java.util.Collection")) {
						iterable = true;
						break;
					}
				}
			}
			
		}
		if (iterable) return iterateValue(value);
		return "";
	}
	
	@SuppressWarnings("all")
	private String iterateValue(Object value ) {
		List<Value> arguments = new ArrayList<Value>();
		Value v = expEval.invoke(value, "iterator", arguments);
		ParseResult evalExpResult = AstTreeFactory.getExpressionAst(evalExp);
		
		ParseResult testExpResult = null ;
		if (testExp != null) {
			testExpResult = AstTreeFactory.getExpressionAst(testExp);
		}
		
		List<String> result = new ArrayList<String>();
		
		int count = -1;
		
		int size = Integer.parseInt(expEval.invoke(value, "size", arguments).toString());
		if (colStartIdx < 0 ) colStartIdx = size + colStartIdx;
		if (colEndIdx < 0) colEndIdx = size + colEndIdx; 
		
		
		while (true) {
			
			Value hasNext = expEval.invoke(v, "hasNext", arguments);
			if (hasNext.toString().equals("false")) break;
			Value item = expEval.invoke(v, "next", arguments);
			
			count++;
			if (count < colStartIdx || count >= colEndIdx ) continue;
			
			expEval.putVar(itemName, item);
			for (int i=0; i< evalExpResult.getExpList().size(); i++) {
				CommonTree node = evalExpResult.getTreeList().get(i);
				
				if (testExpResult !=null) {
					CommonTree testExpNode = testExpResult.getTreeList().get(0);
					String testValue = expEval.evalTreeNodeToStr(testExpNode);
					if (testValue == null || testValue.equals("false")) continue;
				} 
				
				String evaledValue = expEval.evalTreeNodeToStr(node);
				if (evaledValue != null	) result.add(evaledValue);
			}
			expEval.removeVar(itemName);
		}
		return printResult(result);
		
	}
	private String printResult(List<String> result) {
		StringBuilder sb = new StringBuilder("[");
		for (String v : result ) {
			sb.append(v);
			sb.append(",");
		}
		if (sb.charAt(sb.length()-1) == ',') {
			sb.deleteCharAt(sb.length()-1);
		}
		sb.append("]");
		return sb.toString();
	}
		
}
