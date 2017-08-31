package com.spleefleague.annotations.processor.exception;

/**
 *
 * @author jonas
 */
public class RedundantArgumentAnnotationException extends RuntimeException{
    
     public RedundantArgumentAnnotationException() {
        super();
     }
    
    public RedundantArgumentAnnotationException(String msg) {
        super(msg);
    }
}
