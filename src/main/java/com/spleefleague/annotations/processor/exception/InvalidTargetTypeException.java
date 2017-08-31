package com.spleefleague.annotations.processor.exception;

/**
 *
 * @author jonas
 */
public class InvalidTargetTypeException extends RuntimeException{
    
    public InvalidTargetTypeException(String msg) {
        super(msg);
    }
}
