package com.meethub.exception;

public class VotingAccessDeniedException extends RuntimeException {
    public VotingAccessDeniedException(String message) {
        super(message);
    }
}