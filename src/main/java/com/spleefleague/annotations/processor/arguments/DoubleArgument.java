package com.spleefleague.annotations.processor.arguments;

import com.spleefleague.annotations.DispatchResult;
import com.spleefleague.annotations.DispatchResultType;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.MethodSpec.Builder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;

/**
 *
 * @author jonas
 */
public class DoubleArgument extends CommandArgument {
    
    private final double min;
    private final double max;
    private final boolean allowNaN;
    private final boolean allowInfinity;
    
    public DoubleArgument(Map<String, Object> values, TypeMirror type) {
        super(values, type);
        min = super.getValue("min");
        max = super.getValue("max");
        allowNaN = super.getValue("allowNaN");
        allowInfinity = super.getValue("allowInfinity");
    }

    @Override
    public void generateCode(Builder builder, int paramId) {
        if(this.getType().getKind() == TypeKind.ARRAY) {
            generateArray(builder, paramId);
        }
        else {
            generateSingle(builder, paramId);
        }
    }
    
    public void generateArray(Builder builder, int paramId) {
        builder.beginControlFlow("");
            builder.addStatement("$T<Double> parsed = new $T<>()", List.class, ArrayList.class);    
            builder.beginControlFlow("while(position < args.length)");
                builder.beginControlFlow("try");
                    builder.addStatement("double d = Double.parseDouble(args[position++])");
                    if (!allowNaN) {
                        builder.addStatement("if(Double.isNaN(d)) break");
                    }
                    if (!allowInfinity) {
                        builder.addStatement("if(Double.isInfinite(d)) break");
                    }
                    if (min > Integer.MIN_VALUE) {
                        builder.addStatement("if(d < $L) break", min);
                    }
                    if (max < Integer.MAX_VALUE) {
                        builder.addStatement("if(d > $L) break", max);
                    }
                    builder.addStatement("parsed.add(d)");
                builder.endControlFlow();
                builder.beginControlFlow("catch (NumberFormatException e)");
                    builder.addStatement("break");
                builder.endControlFlow();
            builder.endControlFlow();
            builder.addStatement("if(parsed.isEmpty()) return new $T(null, $T.$L)", 
                paramId,
                DispatchResult.class,
                DispatchResultType.class,
                DispatchResultType.NO_VALID_ROUTE
        );
            builder.addStatement("param$L = new double[parsed.size()]", paramId);
            builder.beginControlFlow("for(int i = 0; i < parsed.size(); i++)");
                builder.addStatement("param$L[i] = parsed.get(i)", paramId);
            builder.endControlFlow();
        builder.endControlFlow();
    }
    
    public void generateSingle(Builder builder, int paramId) {
        builder.beginControlFlow("try");
            consumeSafe(builder);
            builder.addStatement("param$L = Double.parseDouble(arg)", paramId);
            if (!allowNaN) {
                builder.addStatement("if(Double.isNaN(param$L)) return new $T(param$L + $S, $T.$L)", 
                        paramId,
                        DispatchResult.class,
                        paramId,
                        " is not a valid value.",
                        DispatchResultType.class,
                        DispatchResultType.OTHER
                );
            }
            if (!allowInfinity) {
                builder.addStatement("if(Double.isInfinite(param$L)) return new $T(param$L + $S, $T.$L)", 
                        paramId,
                        DispatchResult.class,
                        paramId,
                        " is not a valid value.",
                        DispatchResultType.class,
                        DispatchResultType.OTHER
                );
            }
            if (min > Integer.MIN_VALUE) {
                builder.addCode("if(param$L < $L) ", paramId, min);
                rangeError(builder);
            }
            if (max < Integer.MAX_VALUE) {
                builder.addCode("if(param$L > $L) ", paramId, max);
                rangeError(builder);
            }
        builder.endControlFlow();
        builder.beginControlFlow("catch (NumberFormatException e)");
            returnResult(builder, null, DispatchResultType.NO_VALID_ROUTE);
        builder.endControlFlow();
    }
    
    private void rangeError(MethodSpec.Builder builder) {
        if(min > Double.MIN_VALUE && max < Double.MAX_VALUE) {
            builder.addStatement("return new $T($S, $T.$L)", 
                    DispatchResult.class,
                    "Please use a value between " + min + " and " + max + ".",
                    DispatchResultType.class,
                    DispatchResultType.OTHER
            );
        }
        else if(min > Double.MIN_VALUE) {
            builder.addStatement("return new $T($S, $T.$L)", 
                    DispatchResult.class,
                    "Please use a value larger than " + min + ".",
                    DispatchResultType.class,
                    DispatchResultType.OTHER
            );
        }
        else {
            builder.addStatement("return new $T($S, $T.$L)", 
                    DispatchResult.class,
                    "Please use a value smaller than " + max + ".",
                    DispatchResultType.class,
                    DispatchResultType.OTHER
            );
        }
    }
}
