/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.spleefleague.annotations.processor.exception;

import javax.lang.model.element.Element;

/**
 *
 * @author jonas
 */
public class ParameterException extends RuntimeException {
    
    private final Element element;
    
    public ParameterException(Element element) {
        super();
        this.element = element;
    }
    
    public ParameterException(String msg, Element element) {
        super(msg);
        this.element = element;
    }

    /**
     * @return the element
     */
    public Element getElement() {
        return element;
    }
}
