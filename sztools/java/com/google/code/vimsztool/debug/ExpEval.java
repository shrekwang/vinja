package com.google.code.vimsztool.debug;

import java.util.ArrayList;
import java.util.List;

import org.antlr.runtime.ANTLRStringStream;
import org.antlr.runtime.CommonTokenStream;
import org.antlr.runtime.tree.CommonTree;

import com.google.code.vimsztool.exception.ExpressionEvalException;
import com.google.code.vimsztool.parser.JavaLexer;
import com.google.code.vimsztool.parser.JavaParser;
import com.sun.jdi.AbsentInformationException;
import com.sun.jdi.ArrayReference;
import com.sun.jdi.Field;
import com.sun.jdi.IncompatibleThreadStateException;
import com.sun.jdi.IntegerValue;
import com.sun.jdi.LocalVariable;
import com.sun.jdi.LongValue;
import com.sun.jdi.ObjectReference;
import com.sun.jdi.ReferenceType;
import com.sun.jdi.StackFrame;
import com.sun.jdi.ThreadReference;
import com.sun.jdi.Value;
import com.sun.jdi.VirtualMachine;

public class ExpEval {

	public static void main(String[] args) {
		ExpEval app = new ExpEval();
		String aa = app.eval("aa.toUp");
		System.out.print(aa);
	}

	public String eval(String exp) {
		try {
			JavaLexer lex = new JavaLexer(new ANTLRStringStream(exp));
			CommonTokenStream tokens = new CommonTokenStream(lex);
			JavaParser parser = new JavaParser(tokens);
			CommonTree tree = (CommonTree) parser.expression().getTree();
			printTree(tree,0);
			System.out.println("===================================");
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
			
		case JavaParser.IDENT:
			return evalJdiVar(node.getText());
		case JavaParser.METHOD_CALL:
			return "method call";
			
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
	public static void printTree(CommonTree t, int indent) {
        if ( t != null ) {
            StringBuffer sb = new StringBuffer(indent);
            for ( int i = 0; i < indent; i++ )
                sb = sb.append("   ");
            for ( int i = 0; i < t.getChildCount(); i++ ) {
                System.out.println(sb.toString() + t.getChild(i).toString());
                printTree((CommonTree)t.getChild(i), indent+1);
            }
        }
    }
	
	private Value evalJdiVar(String name) {
		ThreadReference threadRef = checkAndGetCurrentThread();
		try {
			SuspendThreadStack threadStack = SuspendThreadStack.getInstance();
			StackFrame stackFrame = threadRef.frame(threadStack.getCurFrame());
			ObjectReference thisObj = stackFrame.thisObject();
			Value value = findValueInFrame(threadRef, name, thisObj);
			return value;
		} catch (IncompatibleThreadStateException e) {
			throw new ExpressionEvalException("eval expression error, caused by : " + e.getMessage());
		}
	}
	private static ThreadReference checkAndGetCurrentThread() {
		Debugger debugger = Debugger.getInstance();
		if (debugger.getVm() == null ) {
			throw new ExpressionEvalException("no virtual machine connected.");
		}
		
		SuspendThreadStack threadStack = SuspendThreadStack.getInstance();
		ThreadReference threadRef = threadStack.getCurThreadRef();
		if (threadRef == null ) {
			throw new ExpressionEvalException("no suspend thread.");
		}
		return threadRef;
	}
	

	public static Value findValueInFrame(ThreadReference threadRef, String name,
			ObjectReference thisObj)  {
		
		Value value = null;
		try {
			SuspendThreadStack threadStack = SuspendThreadStack.getInstance();
			StackFrame stackFrame = threadRef.frame(threadStack.getCurFrame());
			
			LocalVariable localVariable;
			localVariable = stackFrame.visibleVariableByName(name);
			if (localVariable != null) {
				return stackFrame.getValue(localVariable);
			}
			
			ReferenceType refType = stackFrame.location().declaringType();
			if (thisObj != null ) {
				refType = thisObj.referenceType();
			}
			Field field = refType.fieldByName(name);
			if (field == null ) {
				throw new ExpressionEvalException("eval expression error, field '" + name +"' can't be found."); 
			}
			if (thisObj != null) {
				value = thisObj.getValue(field);
			} else {
				value = refType.getValue(field);
			}
		} catch (IncompatibleThreadStateException e) {
			throw new ExpressionEvalException("eval expression error, caused by:" + e.getMessage());
		} catch (AbsentInformationException e) {
			throw new ExpressionEvalException("eval expression error, caused by:" + e.getMessage());
		}
		return value;
	}

}
