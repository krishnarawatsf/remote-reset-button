package com.mock.exception;

public class InterfaceNotFoundException extends RuntimeException {
    public InterfaceNotFoundException(String name) {
        super("Network interface not found: " + name);
    }
}
