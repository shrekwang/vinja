package com.google.code.vimsztool.exception;

public class NoConnectedVmException extends RuntimeException {

	private static final long serialVersionUID = 7507111356225293605L;

	public NoConnectedVmException() {
		super();
	}

	public NoConnectedVmException(String message, Throwable cause) {
		super(message, cause);
	}

	public NoConnectedVmException(String message) {
		super(message);
	}

	public NoConnectedVmException(Throwable cause) {
		super(cause);
	}

}


