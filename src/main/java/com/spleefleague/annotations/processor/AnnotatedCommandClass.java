package com.spleefleague.annotations.processor;

import com.spleefleague.annotations.Dispatcher;
import com.spleefleague.annotations.DispatchableCommand;
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
import com.spleefleague.annotations.DispatchResult;
import com.spleefleague.annotations.DispatchResultType;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.WildcardTypeName;
import java.util.Arrays;
import org.bukkit.command.CommandSender;

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
                .addMethod(generateDispatchHandler(commandEndpoints))
                .addMethods(methods)
                .build();
        String packageName = typeElement.getQualifiedName().toString();
        packageName = packageName.substring(0, packageName.length() - typeElement.getSimpleName().toString().length() - 1);
        JavaFile javaFile = JavaFile.builder(packageName, dispatcherClass)
                .build();
        return javaFile;
    }
    
    private MethodSpec generateDispatchHandler(List<CommandEndpoint> endpoints) {
        TypeName genericFreeTypeName;
        if(typeElement.getTypeParameters().isEmpty()) {
            genericFreeTypeName = TypeName.get(typeElement.asType());
        }
        else {
            TypeName wildcard = WildcardTypeName.subtypeOf(Object.class);
            TypeName[] wildcards = new TypeName[typeElement.getTypeParameters().size()];
            Arrays.fill(wildcards, wildcard);
            genericFreeTypeName = ParameterizedTypeName.get(ClassName.get(typeElement), wildcards);
        }
        MethodSpec.Builder builder = MethodSpec.methodBuilder("dispatch")
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(Override.class)
                .addParameter(DispatchableCommand.class, "rawInstance")
                .addParameter(CommandSender.class, "sender")
                .addParameter(CommandSource.class, "src")
                .addParameter(String[].class, "args")
                .returns(TypeName.get(DispatchResult.class));
        builder.addStatement("$T instance = ($T)rawInstance", genericFreeTypeName, genericFreeTypeName);
        builder.addStatement("boolean valid");
        builder.addStatement("$T result = new $T($T.$L)", DispatchResult.class, DispatchResult.class, DispatchResultType.class, DispatchResultType.NO_ROUTE);
        for (int i = 0; i < endpoints.size(); i++) {
            CommandEndpoint endpoint = endpoints.get(i);
            builder.addCode("valid = false");
            for(CommandSource src : endpoint.getSources()) {
                builder.addCode("\n|| src == $T.$L", CommandSource.class, src);
            }
            builder.addCode(";\n");
            builder.beginControlFlow(("if(valid)"));
                builder.addStatement("result = $L$L(sender, instance, args)", endpoint.getName(), i);
                builder.beginControlFlow("if(result.getType() == $T.$L)", DispatchResultType.class, DispatchResultType.SUCCESS);
                    builder.addStatement("return result");
                builder.endControlFlow();
            builder.endControlFlow();
        }
        builder.addStatement("return result");
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
