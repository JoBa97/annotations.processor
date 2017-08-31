package com.spleefleague.annotations.processor;

import com.spleefleague.annotations.Dispatcher;
import com.spleefleague.annotations.Command;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import com.spleefleague.annotations.CommandSource;

/**
 *
 * @author jonas
 */
public class AnnotatedCommandClass {
    
    private final TypeElement typeElement;
    private final List<CommandEndpoint> commandEndpoints;
    private boolean generated = false;
    
    public AnnotatedCommandClass(TypeElement typeElement) {
        this.typeElement = typeElement;
        this.commandEndpoints = new ArrayList<>();
    }
    
    public void addCommandEndpoint(CommandEndpoint ce) {
        getCommandEndpoints().add(ce);
    }
    
    public JavaFile generateDispatcher() {
        Collections.sort(commandEndpoints);
        List<MethodSpec> methods = new ArrayList<>();
        for (int i = 0; i < commandEndpoints.size(); i++) {
            methods.add(commandEndpoints.get(i).generateDispatchMethod(i));
        }
        TypeSpec dispatcherClass = TypeSpec.classBuilder(typeElement.getSimpleName().toString() + "Dispatcher")
                .addSuperinterface(Dispatcher.class)
                .addModifiers(Modifier.PUBLIC)
                .addMethod(MethodSpec.constructorBuilder().addModifiers(Modifier.PUBLIC).build())
//                .addField(tn, "instance", Modifier.FINAL, Modifier.PRIVATE)
//                .addMethod(generateConstructor(tn))
                .addMethod(generateDispatchHandler(commandEndpoints))
                .addMethods(methods)
                .build();
        JavaFile javaFile = JavaFile.builder("me.joba.command.dispatch", dispatcherClass)
                .build();
        return javaFile;
    }
    
    private MethodSpec generateDispatchHandler(List<CommandEndpoint> endpoints) {
        MethodSpec.Builder builder = MethodSpec.methodBuilder("dispatch")
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(Override.class)
                .addParameter(Command.class, "rawInstance")
                .addParameter(CommandSource.class, "src")
                .addParameter(String[].class, "args")
                .returns(TypeName.BOOLEAN);
        builder.addStatement("$T instance = ($T)rawInstance", TypeName.get(typeElement.asType()), TypeName.get(typeElement.asType()));
        builder.addStatement("boolean valid");
        for (int i = 0; i < endpoints.size(); i++) {
            CommandEndpoint endpoint = endpoints.get(i);
            builder.addCode("valid = false");
            for(CommandSource src : endpoint.getSources()) {
                builder.addCode("\n|| src == $T.$L", CommandSource.class, src);
            }
            builder.addCode(";\n");
            builder.beginControlFlow("if(valid && $L$L(instance, args))", endpoint.getName(), i);
            builder.addStatement("return true");
            builder.endControlFlow();
        }
        builder.addStatement("return false");
        return builder.build();
    }
    
    /**
     * @return the typeElement
     */
    public TypeElement getTypeElement() {
        return typeElement;
    }

    /**
     * @return the commandEndpoints
     */
    public List<CommandEndpoint> getCommandEndpoints() {
        return commandEndpoints;
    }

    /**
     * @return the generated
     */
    public boolean isGenerated() {
        return generated;
    }

    /**
     * @param generated the generated to set
     */
    public void setGenerated(boolean generated) {
        this.generated = generated;
    }
}
