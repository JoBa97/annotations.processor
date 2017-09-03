package com.spleefleague.annotations.processor.exception;

import javax.lang.model.element.Element;

/**
 *
 * @author jonas
 */
public class UnknownArgumentException extends ParameterException{
    
    public UnknownArgumentException(String msg, Element e) {
        super(msg, e);
    }
}
