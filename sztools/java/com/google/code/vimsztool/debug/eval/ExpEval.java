package com.google.code.vimsztool.debug.eval;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.antlr.runtime.tree.CommonTree;

import com.google.code.vimsztool.compiler.CompilerContext;
import com.google.code.vimsztool.debug.Debugger;
import com.google.code.vimsztool.debug.SuspendThreadStack;
import com.google.code.vimsztool.exception.ExpressionEvalException;
import com.google.code.vimsztool.exception.VariableOrFieldNotFoundException;
import com.google.code.vimsztool.parser.AstTreeFactory;
import com.google.code.vimsztool.parser.JavaParser;
import com.google.code.vimsztool.parser.ParseResult;
import com.sun.jdi.AbsentInformationException;
import com.sun.jdi.ArrayReference;
import com.sun.jdi.ArrayType;
import com.sun.jdi.BooleanType;
import com.sun.jdi.ByteType;
import com.sun.jdi.CharType;
import com.sun.jdi.ClassNotLoadedException;
import com.sun.jdi.ClassType;
import com.sun.jdi.DoubleType;
import com.sun.jdi.Field;
import com.sun.jdi.FloatType;
import com.sun.jdi.IncompatibleThreadStateException;
import com.sun.jdi.IntegerType;
import com.sun.jdi.IntegerValue;
import com.sun.jdi.InterfaceType;
import com.sun.jdi.InvalidTypeException;
import com.sun.jdi.InvocationException;
import com.sun.jdi.LocalVariable;
import com.sun.jdi.LongType;
import com.sun.jdi.Method;
import com.sun.jdi.ObjectReference;
import com.sun.jdi.PrimitiveType;
import com.sun.jdi.ReferenceType;
import com.sun.jdi.ShortType;
import com.sun.jdi.StackFrame;
import com.sun.jdi.ThreadReference;
import com.sun.jdi.Type;
import com.sun.jdi.Value;
import com.sun.jdi.VirtualMachine;

public class ExpEval {
	
	private static String[] primitiveTypeNames = { "boolean", "byte", "char",
		"short", "int", "long", "float", "double" };
	
	public static void main(String[] args) {
		ParseResult result = AstTreeFactory.getExpressionAst("new Date(2+3)");
		if (result.hasError()) {
			System.out.println(result.getErrorMsg());
		}
		System.out.println(result.getTree());
		
	}
	
	public static String setFieldValue(String name, String exp) {
		ThreadReference threadRef = checkAndGetCurrentThread();
		SuspendThreadStack threadStack = SuspendThreadStack.getInstance();
		Debugger debugger = Debugger.getInstance();
		VirtualMachine vm = debugger.getVm();
		
		try {
			
			ParseResult result = AstTreeFactory.getExpressionAst(exp);
			if (result.hasError()) {
				return result.getErrorMsg();
			}
			Object obj = evalTreeNode(result.getTree());
			Value value = null;
			if (obj instanceof Integer) {
				value = vm.mirrorOf(((Integer)obj).intValue());
			} else if (obj instanceof Boolean) {
				value = vm.mirrorOf(((Boolean)obj).booleanValue());
			} else if (obj instanceof String) {
				value = vm.mirrorOf((String)obj);
			} else {
				value = (Value)obj;
			}
			
			StackFrame stackFrame = threadRef.frame(threadStack.getCurFrame());
			LocalVariable localVariable;
			localVariable = stackFrame.visibleVariableByName(name);
			if (localVariable != null) {
				stackFrame.setValue(localVariable, value);
			} else {
				ObjectReference thisObj = stackFrame.thisObject();
				if (thisObj == null) {
					return "can't find field or variable with name '"+name+"'";
				}
				ReferenceType refType = thisObj.referenceType();
				Field field = refType.fieldByName(name);
				thisObj.setValue(field, value);
			}
		} catch (Throwable e) {
			return e.getMessage();
		}
		return "success";
	}

	public static String executeEvalCmd(String debugCmd,String debugCmdArgs) {
		String actionResult = null;
		try {
			if (debugCmd.equals("eval") || debugCmd.equals("print")) {
				String exp = debugCmdArgs.substring(debugCmd.length()+1);
				actionResult =  eval(exp);
			} else if (debugCmd.equals("inspect")) {
				String exp = debugCmdArgs.substring(debugCmd.length()+1);
				actionResult =  inspect(exp);
			} else if (debugCmd.equals("locals")) {
				actionResult = variables();
			} else if (debugCmd.equals("fields")) {
				actionResult = fields();
			} else if (debugCmd.equals("reftype")) {
				String exp = debugCmdArgs.substring(debugCmd.length()+1);
				actionResult = reftype(exp);
			}
			return actionResult;
		} catch (ExpressionEvalException e) {
			return e.getMessage();
		}
	}
	
	public static String fields() {
		ThreadReference threadRef = checkAndGetCurrentThread();
		SuspendThreadStack threadStack = SuspendThreadStack.getInstance();
		StringBuilder sb = new StringBuilder();
		try {
			StackFrame stackFrame = threadRef.frame(threadStack.getCurFrame());
			ObjectReference thisObj = stackFrame.thisObject();
			Map<Field, Value> values = thisObj.getValues(thisObj.referenceType().visibleFields());
			List<String> fieldNames = new ArrayList<String>();
			for (Field field : values.keySet()) {
				fieldNames.add(field.name());
			}
			int maxLen = getMaxLength(fieldNames)+2;
			for (Field field : values.keySet()) {
				sb.append(padStr(maxLen,field.name())).append(":");
				sb.append(getPrettyPrintStr(values.get(field)));
				sb.append("\n");
				//stackFrame = threadRef.frame(0);
			}
		} catch (IncompatibleThreadStateException e) {
		}

		return sb.toString();
	}

	
	public static String variables() {
		ThreadReference threadRef = checkAndGetCurrentThread();
		SuspendThreadStack threadStack = SuspendThreadStack.getInstance();
		int curFrame = threadStack.getCurFrame();
		StringBuilder sb = new StringBuilder();
		try {
			List<String> varNames = new ArrayList<String>();
			for (LocalVariable var : threadRef.frame(curFrame).visibleVariables()) {
				varNames.add(var.name());
			}
			int maxLen = getMaxLength(varNames)+2;
			for (LocalVariable var : threadRef.frame(curFrame).visibleVariables()) {
				Value value = threadRef.frame(curFrame).getValue(var);
				sb.append(padStr(maxLen,var.name())).append(":");
				sb.append(getPrettyPrintStr(value));
				sb.append("\n");
			}
		} catch (AbsentInformationException e) {
			e.printStackTrace();
		} catch (IncompatibleThreadStateException e) {
			e.printStackTrace();
		}
		return sb.toString();
	}
	
	public static String reftype(String exp) {
		ParseResult result = AstTreeFactory.getExpressionAst(exp);
		if (result.hasError()) {
			return result.getErrorMsg();
		}
		Value value = (Value)evalTreeNode(result.getTree());
		return value.type().name();
	}

	public static String inspect(String exp) {
		ParseResult result = AstTreeFactory.getExpressionAst(exp);
		if (result.hasError()) {
			return result.getErrorMsg();
		}
		Value value = (Value)evalTreeNode(result.getTree());
		
		StringBuilder sb = new StringBuilder();
		if (value instanceof ObjectReference) {
			sb.append(exp).append(" = ");
			sb.append(value.type().name()).append("\n");
			
			ObjectReference objRef = (ObjectReference) value;
			Map<Field, Value> values = objRef.getValues(objRef.referenceType().visibleFields());
			List<String> fieldNames = new ArrayList<String>();
			for (Field field : values.keySet()) {
				if (field.isStatic()) continue;
				fieldNames.add(field.name());
			}
			int maxLen = getMaxLength(fieldNames)+2;
			for (Field field : values.keySet()) {
				sb.append("    ");
				sb.append(padStr(maxLen,field.name())).append(":");
				sb.append(getPrettyPrintStr(values.get(field)));
				sb.append("\n");
			}
		}
		return sb.toString();
	}
	
	public static String eval(List<String> exps) {
		if (exps ==null || exps.size() == 0) return "";
		StringBuilder sb = new StringBuilder();
		int count = 0;
		int maxLen = getMaxLength(exps)+2;
		for (String exp :exps) {
			count ++;
			sb.append(padStr(maxLen,exp)).append(":");
			try {
				Object value = eval(exp);
				sb.append(getPrettyPrintStr(value)).append("\n");
			} catch (ExpressionEvalException e) {
				sb.append("exception in calc the value");
			}
		}
		return sb.toString();
	}

	public static String eval(String exp) {
		
		ParseResult result = AstTreeFactory.getExpressionAst(exp);
		if (result.hasError()) {
			return result.getErrorMsg();
		}
		
		Object value = evalTreeNode(result.getTree());
		return getPrettyPrintStr(value);
	}


	public static Object evalTreeNode(CommonTree node) {
		
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
			return LogicalNot.operate(subNode);
			
		case JavaParser.IDENT:
			return evalJdiVar(node.getText());
		case JavaParser.METHOD_CALL:
			return evalJdiInvoke(node);
		case JavaParser.ARRAY_ELEMENT_ACCESS:
			return evalJdiArray(node);
		case JavaParser.DOT:
			return evalJdiMember(node);
			
		case JavaParser.PLUS:
		case JavaParser.MINUS:
		case JavaParser.STAR:
		case JavaParser.DIV:
		case JavaParser.MOD:
			
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
		case JavaParser.CHARACTER_LITERAL:
			return Character.valueOf(node.getText().charAt(1));
		case JavaParser.STRING_LITERAL:
			String text = node.getText(); 
			return text.substring(1, text.length()-1);
		case JavaParser.FALSE :
			return Boolean.FALSE;
		case JavaParser.TRUE :
			return Boolean.TRUE;
		case JavaParser.NULL :
			return null;
			
		default:
			throw new ExpressionEvalException("parse expression error.");
		}
	}
	
	private static  Object evalTreeNode(CommonTree leftOp, CommonTree rightOp, int opType) {
		Object leftValue = evalTreeNode(leftOp);
		Object rightValue = evalTreeNode(rightOp);
		Object result = null;
		switch (opType) {
		
		case JavaParser.PLUS:
			return Plus.operate(leftOp, rightOp);
		case JavaParser.MINUS:
			return Minus.operate(leftOp, rightOp);
		case JavaParser.STAR:
			return Multi.operate(leftOp, rightOp);
		case JavaParser.DIV:
			return Divide.operate(leftOp, rightOp);
		case JavaParser.MOD:
			return Mod.operate(leftOp, rightOp);
			
		case JavaParser.NOT_EQUAL:
			return NotEqual.operate(leftOp, rightOp);
		case JavaParser.EQUAL:
			return Equal.operate(leftOp, rightOp);
		case JavaParser.GREATER_THAN:
			return Greater.operate(leftOp, rightOp);
		case JavaParser.GREATER_OR_EQUAL:
			return GreaterOrEqual.operate(leftOp, rightOp);
		case JavaParser.LESS_THAN:
			return Less.operate(leftOp, rightOp);
		case JavaParser.LESS_OR_EQUAL:
			return LessOrEqual.operate(leftOp, rightOp);
			
		case JavaParser.LOGICAL_AND:
			return LogicalAnd.operate(leftOp, rightOp);
		case JavaParser.LOGICAL_OR:
			return LogicalOr.operate(leftOp, rightOp);
		case JavaParser.AND:
			result = ((Integer) leftValue) & ((Integer) rightValue);
			break;
		case JavaParser.OR:
			result = ((Integer) leftValue) | ((Integer) rightValue);
			break;
		}
		return result;
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
	
	private static Value evalJdiInvoke(CommonTree node) {
		CommonTree dotNode = (CommonTree)node.getChild(0);
		CommonTree argNode = (CommonTree)node.getChild(1);
		int argCount = argNode.getChildCount();
		List<Value> arguments = new ArrayList<Value>();
		
		if (argCount > 0 ) {
			for (int i=0; i<argCount; i++) {
				Object result = evalTreeNode((CommonTree)argNode.getChild(i));
				arguments.add(getMirrorValue(result));
			}
			
		}
				
		Object var = null;
		try {
			var = evalTreeNode((CommonTree)dotNode.getChild(0));
		} catch (VariableOrFieldNotFoundException e) {
			var = getClassType(dotNode.getChild(0).getText());
			
		}
		String methodName = dotNode.getChild(1).getText();
		if (var instanceof ObjectReference || var instanceof ReferenceType) {
			return invoke(var,methodName,arguments);
		}
		if (var instanceof String) {
			Debugger debugger = Debugger.getInstance();
			VirtualMachine vm = debugger.getVm();
			return invoke(vm.mirrorOf((String)var),methodName,arguments);
		}
		return null;
	}
	
	
	private static Value getMirrorValue(Object value) {
		if (value == null) return null;
		if (value instanceof Value) return (Value)value;
		
		Debugger debugger = Debugger.getInstance();
		VirtualMachine vm = debugger.getVm();
		if (value instanceof Integer) {
			return vm.mirrorOf(((Integer)value).intValue());
		} else if (value instanceof Boolean) {
			return vm.mirrorOf(((Boolean)value).booleanValue());
		} else if (value instanceof Float) {
			return vm.mirrorOf(((Float)value).floatValue());
		} else if (value instanceof Byte) {
			return vm.mirrorOf(((Byte)value).byteValue());
		} else if (value instanceof Character) {
			return vm.mirrorOf(((Character)value).charValue());
		} else if (value instanceof Double) {
			return vm.mirrorOf(((Double)value).doubleValue());
		} else if (value instanceof Long) {
			return vm.mirrorOf(((Long)value).longValue());
		} else if (value instanceof Short) {
			return vm.mirrorOf(((Short)value).shortValue());
		} else if (value instanceof String) {
			return vm.mirrorOf(((String)value));
		}
		return null;
	}
	
	private static Value evalJdiVar(String name) {
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
	
	private static Value evalJdiMember(CommonTree node) {
		
		CommonTree objNode = (CommonTree)node.getChild(0);
		String memberName = node.getChild(1).getText();
		
		ThreadReference threadRef = checkAndGetCurrentThread();
		ObjectReference thisObj = null;
		
		try {
			thisObj = (ObjectReference)evalTreeNode(objNode);
			Value value = findValueInFrame(threadRef, memberName, thisObj);
			return value;
		} catch (VariableOrFieldNotFoundException e) {
			ReferenceType refType = getClassType(objNode.getText());
			Field field = refType.fieldByName(memberName);
			Value value = refType.getValue(field);
			return value;
		}
		
	}
	
	private static Value evalJdiArray(CommonTree node) {
		CommonTree arrayNode = (CommonTree)node.getChild(0);
		CommonTree indexExpNode = (CommonTree)node.getChild(1);
		
		ArrayReference array = (ArrayReference)evalTreeNode(arrayNode);
		Object arrayIdxValue = evalTreeNode((CommonTree)indexExpNode.getChild(0));
		if (arrayIdxValue instanceof IntegerValue ) {
			int idx = ((IntegerValue)arrayIdxValue).value();
			return  array.getValue(idx);
		} else if (arrayIdxValue instanceof Integer) {
			int idx = ((Integer)arrayIdxValue).intValue();
			return  array.getValue(idx);
		}  else {
			throw new ExpressionEvalException("eval expression error, array index is not int type.");
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
				throw new VariableOrFieldNotFoundException("eval expression error, field '" + name +"' can't be found."); 
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
	
	public static Value invoke(Object invoker, String methodName, List args) {
		SuspendThreadStack threadStack = SuspendThreadStack.getInstance();
		ThreadReference threadRef = threadStack.getCurThreadRef();
		Value value = null;
		Method matchedMethod = null;
		List<Method> methods = null;
		ClassType refType = null;
		ObjectReference obj  = null;
		if (invoker instanceof ClassType) {
			refType = (ClassType)invoker;
		    methods = refType.methodsByName(methodName);
		} else {
		   obj = (ObjectReference)invoker;
		   methods = obj.referenceType().methodsByName(methodName);
		}
		if (methods == null || methods.size() == 0) {
			throw new ExpressionEvalException("eval expression error, method '" + methodName + "' can't be found");
		}
		if (methods.size() == 1) {
			matchedMethod = methods.get(0);
		} else {
			matchedMethod = findMatchedMethod(methods, args);
		}
		try {
		    if (invoker instanceof ClassType) {
			   ClassType clazz = (ClassType)refType;
			   value = clazz.invokeMethod(threadRef, matchedMethod, args,
					ObjectReference.INVOKE_SINGLE_THREADED);
		    } else {
		    	value = obj.invokeMethod(threadRef, matchedMethod, args,
						ObjectReference.INVOKE_SINGLE_THREADED);
		    }
		} catch (InvalidTypeException e) {
			e.printStackTrace();
			throw new ExpressionEvalException("eval expression error, caused by :" + e.getMessage());
		} catch (ClassNotLoadedException e) {
			e.printStackTrace();
			throw new ExpressionEvalException("eval expression error, caused by :" + e.getMessage());
		} catch (IncompatibleThreadStateException e) {
			e.printStackTrace();
			throw new ExpressionEvalException("eval expression error, caused by :" + e.getMessage());
		} catch (InvocationException e) {
			e.printStackTrace();
			throw new ExpressionEvalException("eval expression error, caused by :" + e.getMessage());
		}
		return value;
	}
	private static Method findMatchedMethod(List<Method> methods, List arguments) {
		for (Method method : methods) {
			try {
				List argTypes = method.argumentTypes();
				if (argumentsMatch(argTypes, arguments))
					return method;
			} catch (ClassNotLoadedException e) {
			}
		}
		return null;
	}
	
	private static boolean argumentsMatch(List argTypes, List arguments) {
		if (argTypes.size() != arguments.size()) {
			return false;
		}
		Iterator typeIter = argTypes.iterator();
		Iterator valIter = arguments.iterator();
		while (typeIter.hasNext()) {
			Type argType = (Type) typeIter.next();
			Value value = (Value) valIter.next();
			if (value == null) {
				if (isPrimitiveType(argType.name()))
					return false;
			}
			if (!value.type().equals(argType)) {
				if (isAssignableTo(value.type(), argType)) {
					return true;
				} else {
					return false;
				}
			}
		}
		return true;
	}

	private static boolean isPrimitiveType(String name) {
		for (String primitiveType : primitiveTypeNames) {
			if (primitiveType.equals(name))
				return true;
		}
		return false;
	}
	private static boolean isAssignableTo(Type fromType, Type toType) {

		if (fromType.equals(toType))
			return true;
		if (fromType instanceof BooleanType && toType instanceof BooleanType)
			return true;
		if (toType instanceof BooleanType)
			return false;
		
		if (fromType instanceof BooleanType && toType instanceof BooleanType) return true;
		if (fromType instanceof ByteType && toType instanceof ByteType) return true;
		if (fromType instanceof CharType && toType instanceof CharType) return true;
		if (fromType instanceof IntegerType && toType instanceof IntegerType) return true;
		if (fromType instanceof LongType && toType instanceof LongType) return true;
		if (fromType instanceof ByteType && toType instanceof ByteType) return true;
		if (fromType instanceof FloatType && toType instanceof FloatType) return true;
		if (fromType instanceof DoubleType && toType instanceof DoubleType) return true;
		if (fromType instanceof ShortType && toType instanceof ShortType) return true;

		if (fromType instanceof ArrayType) {
			return isArrayAssignableTo((ArrayType) fromType, toType);
		}
		List interfaces = null;;
		if (fromType instanceof ClassType) {
			ClassType superclazz = ((ClassType) fromType).superclass();
			if ((superclazz != null) && isAssignableTo(superclazz, toType)) {
				return true;
			}
			interfaces = ((ClassType) fromType).interfaces();
		} else if (fromType instanceof InterfaceType) {
			interfaces = ((InterfaceType) fromType).superinterfaces();
		}
		if (interfaces == null ) return false;
		
		Iterator iter = interfaces.iterator();
		while (iter.hasNext()) {
			InterfaceType interfaze = (InterfaceType) iter.next();
			if (isAssignableTo(interfaze, toType)) {
				return true;
			}
		}
		return false;
	}

	static boolean isArrayAssignableTo(ArrayType fromType, Type toType) {
		if (toType instanceof ArrayType) {
			try {
				Type toComponentType = ((ArrayType) toType).componentType();
				return isComponentAssignable(fromType.componentType(),
						toComponentType);
			} catch (ClassNotLoadedException e) {
				return false;
			}
		}
		if (toType instanceof InterfaceType) {
			return toType.name().equals("java.lang.Cloneable");
		}
		return toType.name().equals("java.lang.Object");
	}

	private static boolean isComponentAssignable(Type fromType, Type toType) {
		if (fromType instanceof PrimitiveType) {
			return fromType.equals(toType);
		}
		if (toType instanceof PrimitiveType) {
			return false;
		}
		return isAssignableTo(fromType, toType);
	}
	
	
	private static int getMaxLength(List<String> names) {
		int max = 0;
		if (names == null || names.size() ==0) return 0;
		for (String name : names ) {
			int len = name.length();
			if (len > max) max = len;
		}
		return max;
	}
	private static String padStr(int maxLen, String origStr) {
		if (origStr  == null) return "";
		int len = origStr.length();
		if (len >= maxLen ) return origStr;
		for (int i=0; i< (maxLen-len); i++) {
			origStr = origStr + " ";
		}
		return origStr;
	}
	
	public static String getPrettyPrintStr(Object var) {
		if (var == null)
			return "null";
		if (var instanceof ArrayReference) {
			StringBuilder sb = new StringBuilder("[");
			ArrayReference arrayObj = (ArrayReference) var;
			if (arrayObj.length() == 0)
				return "[]";
			List<Value> values = arrayObj.getValues();
			for (Value value : values) {
				sb.append(getPrettyPrintStr(value)).append(",");
			}
			sb.deleteCharAt(sb.length() - 1);
			sb.append("]");
			return sb.toString();
		} else if (var instanceof ObjectReference) {
			Value strValue = invoke((ObjectReference) var, "toString",
					new ArrayList());
			return strValue.toString();
		} else if (var instanceof String) {
			return "\"" + (String)var + "\"";
			
		} else {
			return var.toString();
		}
	}

	
    private static ReferenceType getClassType(String className) {
		
		try {
			ThreadReference threadRef = checkAndGetCurrentThread();
			Debugger debugger = Debugger.getInstance();
			SuspendThreadStack threadStack = SuspendThreadStack.getInstance();
			StackFrame stackFrame = threadRef.frame(threadStack.getCurFrame());
			CompilerContext ctx = debugger.getCompilerContext();
			VirtualMachine vm = debugger.getVm();
			List<ReferenceType> refTypes = vm.classesByName("java.lang."+className);
			if (refTypes !=null && refTypes.size() >0 ) {
				return refTypes.get(0);
			}
			String locSourcePath = stackFrame.location().sourcePath();
			String abPath = ctx.findSourceFile(locSourcePath);
			BufferedReader br = new BufferedReader(new FileReader(abPath));
			Pattern pat = Pattern.compile("import\\s+(.*\\."+className+")\\s*;\\s*$");
			String qualifiedClass = null;
			
			int pkgIndex = locSourcePath.lastIndexOf(File.separator);
			String packageName = "";
			if (pkgIndex != -1 ){
				packageName = locSourcePath.substring(0,pkgIndex).replace(File.separator, ".");
			}
			
			while (true) {
				String tmp = br.readLine();
				if (tmp == null) break;
				tmp = tmp.trim();
				Matcher matcher = pat.matcher(tmp);
				if (matcher.matches()) {
					qualifiedClass = matcher.group(1);
					break;
				} 
			}
			br.close();
			if (qualifiedClass == null && !packageName.equals("")) {
				qualifiedClass = packageName + "." + className;
			} else {
				qualifiedClass = className;
			}
			refTypes = vm.classesByName(qualifiedClass);
			if (refTypes !=null && refTypes.size() >0 ) {
				return refTypes.get(0);
			}
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
	
}
