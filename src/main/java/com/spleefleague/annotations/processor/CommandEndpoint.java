package com.spleefleague.annotations.processor;

import com.spleefleague.annotations.Argument;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.MethodSpec.Builder;
import com.squareup.javapoet.TypeName;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.MirroredTypeException;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import com.spleefleague.annotations.CommandSource;
import com.spleefleague.annotations.DispatchResult;
import com.spleefleague.annotations.DispatchResultType;
import com.spleefleague.annotations.Endpoint;
import com.spleefleague.annotations.processor.arguments.CommandArgument;
import com.spleefleague.annotations.processor.exception.InvalidTargetTypeException;
import com.spleefleague.annotations.processor.exception.NakedArgumentException;
import com.spleefleague.annotations.processor.exception.RedundantArgumentAnnotationException;
import org.bukkit.command.CommandSender;

/**
 *
 * @author jonas
 */
public class CommandEndpoint implements Comparable<CommandEndpoint> {

    private final ExecutableElement method;
    private final int priority;
    private final CommandSource[] sources;
    private final List<CommandArgument> commandArguments;
    private final TypeElement enclosingClass;
    private final VariableElement senderCastTarget;
    
    public CommandEndpoint(ExecutableElement method, TypeElement enclosingClass, Elements elementUtils, Types typeUtils) {
        this.method = method;
        this.enclosingClass = enclosingClass;
        this.commandArguments = new ArrayList<>();
        Endpoint endpoint = method.getAnnotation(Endpoint.class);
        this.priority = endpoint.priority();
        this.sources = endpoint.target();
        List<? extends VariableElement> params = method.getParameters();
        if(params.isEmpty()) {
            senderCastTarget = null;
            return;
        }
        VariableElement senderParam = params.get(0);
        int start = 0;
        if(getParamAnnotation(senderParam, params.size() == 1, elementUtils, typeUtils) == null) {
            if(isCommandSenderArgument(senderParam, elementUtils, typeUtils)) {
                start = 1;
                senderCastTarget = senderParam;
            }
            else {    
                throw new NakedArgumentException(senderParam.getSimpleName().toString() + " has no argument annotation", senderParam);
            }
        }
        else {
            senderCastTarget = null;
        }
        for (int i = start; i < params.size(); i++) {
            try {
                VariableElement param = params.get(i);
                AnnotationMirror paramAnnotation = getParamAnnotation(param, i == params.size() - 1, elementUtils, typeUtils);
                if(paramAnnotation == null) {
                    throw new NakedArgumentException(param.getSimpleName().toString() + " has no argument annotation", param);
                }
                commandArguments.add(CommandArgument.create(param.asType(), paramAnnotation));
            } catch (NoSuchMethodException | InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) {
                ex.printStackTrace();
                throw new RuntimeException(ex.getMessage());
            }
        }
    }
    
    private boolean isCommandSenderArgument(VariableElement param, Elements elementUtils, Types typeUtils) {
        TypeElement cselement = elementUtils.getTypeElement(CommandSender.class.getCanonicalName());
        return typeUtils.isAssignable(param.asType(), cselement.asType());
    }
    
    public String getName() {
        return method.getSimpleName().toString();
    }

    private AnnotationMirror getParamAnnotation(VariableElement param, boolean arrayAllowed, Elements elementUtils, Types typeUtils) {
        AnnotationMirror paramAnnotation = null;
        TypeMirror paramTypeUnconverted = param.asType();
            
        for (AnnotationMirror mirror : param.getAnnotationMirrors()) {
            Argument arg = mirror.getAnnotationType().asElement().getAnnotation(Argument.class);
            if (arg == null) {
                continue;
            }
            TypeMirror paramType = paramTypeUnconverted;
            //Unwrapping T[] to T
            if(arrayAllowed && arg.allowArray() && paramType.getKind() == TypeKind.ARRAY) {
                paramType = ((ArrayType)paramType).getComponentType();
            }
            TypeMirror annotationTargetType;
            try {
                Class c = arg.target();
                annotationTargetType = elementUtils.getTypeElement(c.getName()).asType();
            } catch (MirroredTypeException ex) {
                annotationTargetType = ex.getTypeMirror();
            }
            
            if (!typeUtils.isAssignable(paramType, annotationTargetType)) {
                throw new InvalidTargetTypeException(paramType.toString() + " is not a supertype of " + annotationTargetType.toString(), param);
            }
            if (paramAnnotation != null) {
                throw new RedundantArgumentAnnotationException(param);
            }
            paramAnnotation = mirror;
        }
        return paramAnnotation;
    }

    /**
     * @return the method
     */
    public ExecutableElement getMethod() {
        return method;
    }

    public MethodSpec generateDispatchMethod(int id) {
        Builder builder = MethodSpec.methodBuilder(this.method.getSimpleName().toString() + id);
        builder
                .addModifiers(Modifier.PRIVATE)
                .returns(DispatchResult.class)
                .addParameter(CommandSender.class, "sender")
                .addParameter(TypeName.get(enclosingClass.asType()), "instance")
                .addParameter(String[].class, "args");
        generateStaticHeader(builder);
        for (int i = 0; i < commandArguments.size(); i++) {
            commandArguments.get(i).generateCode(builder, i);
        }
        generateStaticReturn(builder);
        return builder.build();
    }

    private void generateStaticHeader(Builder builder) {
        builder.addStatement("int position = 0");
        builder.addStatement("String arg");
        for (int i = 0; i < commandArguments.size(); i++) {
            CommandArgument carg = commandArguments.get(i);
            builder.addStatement("$T param$L", carg.getType(), i);
        }
    }

    private void generateStaticReturn(Builder builder) {
        builder.addStatement("if(position != args.length) return new $T($T.$L)", 
                DispatchResult.class, 
                DispatchResultType.class, 
                DispatchResultType.NO_VALID_ROUTE
        );
        StringJoiner sj = new StringJoiner(", ");
        String methodName = method.getSimpleName().toString();
        if(senderCastTarget != null) {
            builder.addStatement("$T senderCasted = (($T)sender)", senderCastTarget, senderCastTarget);
            sj.add("senderCasted");
        }
        for (int i = 0; i < commandArguments.size(); i++) {
            sj.add("param" + i);
        }
        builder.addStatement("instance.$L($L)", methodName, sj.toString());
        builder.addStatement("return new $T($T.$L)", DispatchResult.class, DispatchResultType.class, DispatchResultType.SUCCESS);
    }
    
    /**
     * @return the priority
     */
    public int getPriority() {
        return priority;
    }

    @Override
    public int compareTo(CommandEndpoint o) {
        return Integer.compare(o.priority, priority);
    }

    /**
     * @return the sources
     */
    public CommandSource[] getSources() {
        return sources;
    }
}
