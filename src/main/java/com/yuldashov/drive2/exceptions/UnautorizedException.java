package com.yuldashov.drive2.exceptions;

public class UnautorizedException extends RuntimeException {
    public UnautorizedException(String message) {
        super(message);
    }
}
