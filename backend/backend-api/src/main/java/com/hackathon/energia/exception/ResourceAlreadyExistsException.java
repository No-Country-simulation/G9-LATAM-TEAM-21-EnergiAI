package com.hackathon.energia.exception;

public class ResourceAlreadyExistsException extends RuntimeException{
    public ResourceAlreadyExistsException(String string) {
        super(string);
    }
}
