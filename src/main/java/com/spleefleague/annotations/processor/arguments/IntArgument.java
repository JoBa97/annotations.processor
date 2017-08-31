package com.spleefleague.annotations.processor.arguments;

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
public class IntArgument extends CommandArgument {

    private final int min;
    private final int max;

    public IntArgument(Map<String, Object> values, TypeMirror type) {
        super(values, type);
        min = super.getValue("min");
        max = super.getValue("max");
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
            builder.addStatement("$T<Integer> parsed = new $T<>()", List.class, ArrayList.class);    
            builder.beginControlFlow("while(position < args.length)");
                builder.beginControlFlow("try");
                    builder.addStatement("int n = Integer.parseInt(args[position++])");
                    if (min > Integer.MIN_VALUE) {
                        builder.addStatement("if(n < $L) break", min);
                    }
                    if (max < Integer.MAX_VALUE) {
                        builder.addStatement("if(n > $L) break", max);
                    }
                    builder.addStatement("parsed.add(n)");
                builder.endControlFlow();
                builder.beginControlFlow("catch (NumberFormatException e)");
                    builder.addStatement("break");
                builder.endControlFlow();
            builder.endControlFlow();
            builder.addStatement("param$L = new int[parsed.size()]", paramId);
            builder.beginControlFlow("for(int i = 0; i < parsed.size(); i++)");
                builder.addStatement("param$L[i] = parsed.get(i)", paramId);
            builder.endControlFlow();
        builder.endControlFlow();
    }
    
    public void generateSingle(Builder builder, int paramId) {
        builder.beginControlFlow("try");
            builder.addStatement("param$L = Integer.parseInt(args[position++])", paramId);
            if (min > Integer.MIN_VALUE) {
                builder.addStatement("if(param$L < $L) return false", paramId, min);
            }
            if (max < Integer.MAX_VALUE) {
                builder.addStatement("if(param$L > $L) return false", paramId, max);
            }
        builder.endControlFlow();
        builder.beginControlFlow("catch (NumberFormatException e)");
            builder.addStatement("return false");
        builder.endControlFlow();
    }
}
