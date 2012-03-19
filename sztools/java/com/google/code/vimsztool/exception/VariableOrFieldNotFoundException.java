package com.google.code.vimsztool.exception;

public class VariableOrFieldNotFoundException  extends ExpressionEvalException {

	private static final long serialVersionUID = 7507111356225293605L;

	public VariableOrFieldNotFoundException() {
		super();
	}

	public VariableOrFieldNotFoundException(String message, Throwable cause) {
		super(message, cause);
	}

	public VariableOrFieldNotFoundException(String message) {
		super(message);
	}

	public VariableOrFieldNotFoundException(Throwable cause) {
		super(cause);
	}

}

