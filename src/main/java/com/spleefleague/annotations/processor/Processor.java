/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.spleefleague.annotations.processor;

import com.google.auto.service.AutoService;
import com.spleefleague.annotations.Argument;
import com.squareup.javapoet.JavaFile;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.TypeParameterElement;
import javax.lang.model.type.NoType;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;
import com.spleefleague.annotations.Endpoint;
import com.spleefleague.annotations.processor.exception.ParameterException;

/**
 *
 * @author jonas
 */
@AutoService(Processor.class)
public class Processor extends AbstractProcessor {
    
    public static Messager messager;
    private Types typeUtils;
    private Elements elementUtils;
    private Filer filer;
    private Map<TypeElement, AnnotatedCommandClass> commands;
    
    
    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        typeUtils = processingEnv.getTypeUtils();
        elementUtils = processingEnv.getElementUtils();
        filer = processingEnv.getFiler();
        messager = processingEnv.getMessager();
        commands = new HashMap<>();
    }

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        return SUPPORTED_ANNOTATIONS;
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        for(Element annotatedElement : roundEnv.getElementsAnnotatedWith(Endpoint.class)) {
            if(annotatedElement.getKind() != ElementKind.METHOD) {
                messager.printMessage(Diagnostic.Kind.ERROR, "Annotated object must be a method", annotatedElement);
                return false;
            }
            try {
                boolean success = processElement((ExecutableElement) annotatedElement);
                if(!success) {
                    messager.printMessage(Diagnostic.Kind.ERROR, "Error processing element", annotatedElement);
                }
            } catch(ParameterException e) {
                Element elem = e.getElement();
                if(elem == null) {
                    elem = annotatedElement;
                }
                messager.printMessage(Diagnostic.Kind.ERROR, e.getMessage(), elem);
            } catch(Exception e) {
                messager.printMessage(Diagnostic.Kind.ERROR, e.getMessage(), annotatedElement);
                return false;
            }
        }
        for(AnnotatedCommandClass acc : commands.values()) {
            if(!acc.isGenerated()) {
                try {
                    JavaFile file = acc.generateDispatcher();
                    file.writeTo(filer);
                    acc.setGenerated(true);
                } catch (Exception ex) {
                    Logger.getLogger(Processor.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
        return true;
    }
    
    private boolean processElement(ExecutableElement element) {
        if(!isValidElement(element)) {
            return false;
        }
        Element parent = element.getEnclosingElement();
        while(parent.getKind() != ElementKind.CLASS) {
            parent = parent.getEnclosingElement();
        }
        TypeElement enclosingClass = (TypeElement)parent;
        
        AnnotatedCommandClass acc;
        if(commands.containsKey(enclosingClass)) {
            acc = commands.get(enclosingClass);
        }
        else {
            acc = new AnnotatedCommandClass(enclosingClass);
            commands.put(enclosingClass, acc);
        }
        CommandEndpoint ce = new CommandEndpoint(element, enclosingClass, elementUtils, typeUtils);
        acc.addCommandEndpoint(ce);
        messager.printMessage(Diagnostic.Kind.NOTE, "Found endpoint " + element.getSimpleName().toString());
        return true;
    }

    private boolean isValidElement(ExecutableElement element) {
        for(TypeParameterElement tpe : element.getTypeParameters()) {
            if(tpe.getAnnotation(Argument.class) == null) {
                return false;
            }
        }
        return element.getReturnType() instanceof NoType;//Method returns void
    }
    
    private static final Set<String> SUPPORTED_ANNOTATIONS = new HashSet<>();
    
    static {
        SUPPORTED_ANNOTATIONS.add(Endpoint.class.getCanonicalName());
    }
    
}
