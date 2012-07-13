package com.google.code.vimsztool.exception;

public class LocationNotFoundException extends RuntimeException {

	private static final long serialVersionUID = 7507111356225293605L;

	public LocationNotFoundException() {
		super();
	}

	public LocationNotFoundException(String message, Throwable cause) {
		super(message, cause);
	}

	public LocationNotFoundException(String message) {
		super(message);
	}

	public LocationNotFoundException(Throwable cause) {
		super(cause);
	}

}


