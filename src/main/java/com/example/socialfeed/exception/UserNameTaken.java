package com.example.socialfeed.exception;

public class UserNameTaken extends RuntimeException {
    public UserNameTaken(String message) {
        super(message);
    }
}
