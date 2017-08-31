package com.spleefleague.annotations.processor.exception;

/**
 *
 * @author jonas
 */
public class NakedArgumentException extends RuntimeException{
    
     public NakedArgumentException() {
        super();
     }
    
    public NakedArgumentException(String msg) {
        super(msg);
    }
}
