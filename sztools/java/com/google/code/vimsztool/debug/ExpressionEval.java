package com.google.code.vimsztool.debug;

import java.util.List;

import com.sun.jdi.AbsentInformationException;
import com.sun.jdi.Field;
import com.sun.jdi.IncompatibleThreadStateException;
import com.sun.jdi.LocalVariable;
import com.sun.jdi.ReferenceType;
import com.sun.jdi.StackFrame;
import com.sun.jdi.ThreadReference;
import com.sun.jdi.Value;

public class ExpressionEval {
	
	public static String eval(String exp) {
		SuspendThreadStack threadStack = SuspendThreadStack.getInstance();
		ThreadReference threadRef = threadStack.getCurThreadRef();
		if (threadRef == null) {
			return "no suspend thread";
		}
		ReferenceType refType = threadStack.getCurRefType();
		StackFrame stackFrame;
		try {
			stackFrame = threadRef.frame(0);
			LocalVariable localVariable;
			localVariable = stackFrame.visibleVariableByName(exp);
			Value value = null;
			if (localVariable !=null ) {
				value = stackFrame.getValue(localVariable);
			} else {
				List<Field> fields = refType.visibleFields();
				for (Field field : fields ){
					if (field.name().equals(exp)) {
						value = refType.getValue(field);
						break;
					}
				}
				if (value ==null) {
					return "can't find variable " + exp;
				}
			}
			return value.toString();
		} catch (IncompatibleThreadStateException e) {
		} catch (AbsentInformationException e) {
		}
		return "eval expression error";
	}

}
