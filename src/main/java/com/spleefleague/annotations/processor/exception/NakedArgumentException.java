package com.spleefleague.annotations.processor.exception;

import javax.lang.model.element.Element;

/**
 *
 * @author jonas
 */
public class NakedArgumentException extends ParameterException{
    
    public NakedArgumentException(String msg, Element e) {
        super(msg, e);
    }
}
