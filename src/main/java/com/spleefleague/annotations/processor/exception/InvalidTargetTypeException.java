package com.spleefleague.annotations.processor.exception;

import javax.lang.model.element.Element;

/**
 *
 * @author jonas
 */
public class InvalidTargetTypeException extends ParameterException{
    
    public InvalidTargetTypeException(String msg, Element e) {
        super(msg, e);
    }
}
