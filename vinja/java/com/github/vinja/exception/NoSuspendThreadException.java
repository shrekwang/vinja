package com.github.vinja.exception;

public class NoSuspendThreadException extends RuntimeException {

	private static final long serialVersionUID = 7507111356225293605L;

	public NoSuspendThreadException() {
		super();
	}

	public NoSuspendThreadException(String message, Throwable cause) {
		super(message, cause);
	}

	public NoSuspendThreadException(String message) {
		super(message);
	}

	public NoSuspendThreadException(Throwable cause) {
		super(cause);
	}

}


