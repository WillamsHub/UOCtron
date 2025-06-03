package edu.uoc.uoctron.exception;

import java.lang.reflect.Type;

public abstract class AppException extends Exception{

    public Type field;

    public AppException(String message) {
        super("[ERROR] " + message);
    }
}
