package com.mock.exception;

public class InterfaceAlreadyExistsException extends RuntimeException {
    public InterfaceAlreadyExistsException(String name) {
        super("Network interface already exists: " + name);
    }
}
