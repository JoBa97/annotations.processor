package com.spleefleague.annotations.processor.exception;

import javax.lang.model.element.Element;

/**
 *
 * @author jonas
 */
public class RedundantArgumentAnnotationException extends ParameterException{
    
    public RedundantArgumentAnnotationException(Element e) {
        super(e);
    }
    
    public RedundantArgumentAnnotationException(String msg, Element e) {
        super(msg, e);
    }
}

