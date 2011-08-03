package com.google.code.vimsztool.exception;

public class ExpressionEvalException extends RuntimeException {

	private static final long serialVersionUID = 7507111356225293605L;

	public ExpressionEvalException() {
		super();
	}

	public ExpressionEvalException(String message, Throwable cause) {
		super(message, cause);
	}

	public ExpressionEvalException(String message) {
		super(message);
	}

	public ExpressionEvalException(Throwable cause) {
		super(cause);
	}

}


