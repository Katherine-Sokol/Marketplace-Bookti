package com.teamchallenge.bookti.exception;

public class RefreshTokenAlreadyRevokedException extends RuntimeException {
    public RefreshTokenAlreadyRevokedException(String message) {
        super(message);
    }
}
