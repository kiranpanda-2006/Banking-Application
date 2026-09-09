package com.banking.exception;

public class ContractViolationException extends RuntimeException {
    public ContractViolationException(String message) {
        super(message);
    }
}
