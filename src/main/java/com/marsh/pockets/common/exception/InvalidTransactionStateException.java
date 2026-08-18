package com.marsh.pockets.common.exception;

public class InvalidTransactionStateException extends RuntimeException {
    public InvalidTransactionStateException(String message) {
        super(message);
    }
}
