package com.Attendify.Attendify.exception;

@SuppressWarnings("serial")
public class UsernameAlreadyTakenException extends RuntimeException {

	public UsernameAlreadyTakenException(String message) {
        super(message);
    }
}
