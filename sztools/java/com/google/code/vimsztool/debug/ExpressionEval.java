package com.google.code.vimsztool.debug;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.sun.jdi.ArrayReference;
import com.sun.jdi.ArrayType;
import com.sun.jdi.BooleanType;
import com.sun.jdi.ClassNotLoadedException;
import com.sun.jdi.ClassType;
import com.sun.jdi.Field;
import com.sun.jdi.IncompatibleThreadStateException;
import com.sun.jdi.InterfaceType;
import com.sun.jdi.InvalidTypeException;
import com.sun.jdi.InvocationException;
import com.sun.jdi.LocalVariable;
import com.sun.jdi.Method;
import com.sun.jdi.ObjectReference;
import com.sun.jdi.PrimitiveType;
import com.sun.jdi.ReferenceType;
import com.sun.jdi.StackFrame;
import com.sun.jdi.ThreadReference;
import com.sun.jdi.Type;
import com.sun.jdi.Value;
import com.sun.jdi.VirtualMachine;

public class ExpressionEval {

	private static String[] primitiveTypeNames = { "boolean", "byte", "char",
			"short", "int", "long", "float", "double" };
	
	public static String eval(String expXmlStr) {
		SuspendThreadStack threadStack = SuspendThreadStack.getInstance();
		ThreadReference threadRef = threadStack.getCurThreadRef();
		if (threadRef == null) {
			return "no suspend thread";
		}
		Expression exp = Expression.parseExpXmlStr(expXmlStr);
		try {
			StackFrame stackFrame =  threadRef.frame(0);
			ObjectReference thisObj = stackFrame.thisObject();
			Value value = eval(threadRef,exp,thisObj);
			return getPrettyPrintStr(value);
		} catch (IncompatibleThreadStateException e) {
		}
		return "eval expression error";
	}
	
	public static Value eval(ThreadReference threadRef , Expression exp, ObjectReference thisObj) {
		
		if (exp == null) return null;
		VirtualMachine vm = threadRef.virtualMachine();
		String expType = exp.getExpType();
		String expName = exp.getName();
		
		if (expType.equals(Expression.EXP_TYPE_BOOL)) {
			if (expName.equals("true")) 
				return vm.mirrorOf(true);
			else 
				return vm.mirrorOf(false);
		} else if (expType.equals(Expression.EXP_TYPE_STR)) {
			return vm.mirrorOf(expName);
		} else if (expType.equals(Expression.EXP_TYPE_NULL)) {
			return null;
		} else if (expType.equals(Expression.EXP_TYPE_NUM)) {
			if (expName.indexOf(".") > -1 ) {
				return vm.mirrorOf(Float.parseFloat(expName));
			} else {
				return vm.mirrorOf(Integer.parseInt(expName));
			}
		}
		
		Value basicExpValue = null;
		if (! exp.isMethod()) {
			basicExpValue =findValueInFrame(threadRef, expName);
		} else {
			List<Expression> params = exp.getParams();
			List<Value> arguments = new ArrayList<Value>();
			if (params.size() !=0) {
				for (Expression param : params) {
					Value paramValue = eval(threadRef, param, thisObj);
					arguments.add(paramValue);
				}
			}
			basicExpValue = invoke((ObjectReference)thisObj,expName,arguments);
		}
		
		List<Expression> members = exp.getMembers();
		if (members.size() == 0) return basicExpValue;
		
		for (Expression member: members	 ) {
			basicExpValue =eval(threadRef,member,(ObjectReference)basicExpValue);
		}
		return basicExpValue;
	}
	
	public static Value findValueInFrame(ThreadReference threadRef , String name) {
		Value value = null;
		try {
			StackFrame stackFrame = threadRef.frame(0);
			LocalVariable localVariable;
			localVariable = stackFrame.visibleVariableByName(name);
			if (localVariable != null) {
				return  stackFrame.getValue(localVariable);
			}
			ObjectReference thisObj = stackFrame.thisObject();
			ReferenceType refType = thisObj.referenceType();
			List<Field> fields = refType.visibleFields();
			for (Field field : fields) {
				if (field.name().equals(name)) {
					value = thisObj.getValue(field);
					break;
				}
			}
		} catch (Throwable e) {
		}
		return value;
	}


	public static String getPrettyPrintStr(Value var) {
		if (var == null) return "null";
		if (var instanceof ArrayReference) {
			StringBuilder sb = new StringBuilder("[");
			ArrayReference arrayObj = (ArrayReference)var;
			if (arrayObj.length() == 0) return "[]";
			List<Value> values = 	arrayObj.getValues();
			for (Value value : values ) {
				sb.append(getPrettyPrintStr(value)).append(",");
			}
			sb.deleteCharAt(sb.length()-1);
			sb.append("]");
			return sb.toString();
		} else if (var instanceof ObjectReference) {
			Value strValue = invoke((ObjectReference)var,"toString",new ArrayList());
			return strValue.toString();
		} else {
			return  var.toString();
		}
	}

	public static Value invoke(ObjectReference obj, String methodName, List args) {
		SuspendThreadStack threadStack = SuspendThreadStack.getInstance();
		ThreadReference threadRef = threadStack.getCurThreadRef();
		Value value = null;
		Method matchedMethod = null;
		List<Method> methods = obj.referenceType().methodsByName(methodName);
		if (methods == null || methods.size() == 0)
			return null;
		if (methods.size() == 1) {
			matchedMethod = methods.get(0);
		} else {
			matchedMethod = findMatchedMethod(methods, args) ;
		}
		try {
			value = obj.invokeMethod(threadRef, matchedMethod, args, ObjectReference.INVOKE_SINGLE_THREADED);
		} catch (InvalidTypeException e) {
			e.printStackTrace();
		} catch (ClassNotLoadedException e) {
			e.printStackTrace();
		} catch (IncompatibleThreadStateException e) {
			e.printStackTrace();
		} catch (InvocationException e) {
			e.printStackTrace();
		}
		return value;
	}

	private static Method findMatchedMethod(List<Method> methods, List arguments) {
		for (Method method : methods ) {
			try {
				List argTypes = method.argumentTypes();
				if (argumentsMatch(argTypes, arguments)) return method;
			} catch (ClassNotLoadedException e) {
			}
		}
		return null;
	}

	private static boolean isPrimitiveType(String name) {
		for (String primitiveType : primitiveTypeNames) {
			if (primitiveType.equals(name))
				return true;
		}
		return false;
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

	private static boolean isAssignableTo(Type fromType, Type toType) {

		if (fromType.equals(toType))
			return true;
		if (fromType instanceof BooleanType && toType instanceof BooleanType)
			return true;
		if (toType instanceof BooleanType)
			return false;
		if (fromType instanceof PrimitiveType
				&& toType instanceof PrimitiveType)
			return true;
		if (toType instanceof PrimitiveType)
			return false;

		if (fromType instanceof ArrayType) {
			return isArrayAssignableTo((ArrayType) fromType, toType);
		}
		List interfaces;
		if (fromType instanceof ClassType) {
			ClassType superclazz = ((ClassType) fromType).superclass();
			if ((superclazz != null) && isAssignableTo(superclazz, toType)) {
				return true;
			}
			interfaces = ((ClassType) fromType).interfaces();
		} else {
			interfaces = ((InterfaceType) fromType).superinterfaces();
		}
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

}
