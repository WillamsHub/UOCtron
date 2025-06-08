package edu.uoc.uoctron.exception;

public class ModelMainException extends AppException {

    public static final String ERROR_NULL_PLANT = "The Plant cannot be null.";
    public ModelMainException(String message) {
        super(message);
    }
}
