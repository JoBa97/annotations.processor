package com.spleefleague.annotations.processor.arguments;

import com.spleefleague.annotations.DispatchResult;
import com.spleefleague.annotations.DispatchResultType;
import com.squareup.javapoet.MethodSpec;
import java.util.Map;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;

/**
 *
 * @author balsfull
 */
public class StringArgument extends CommandArgument {

    public StringArgument(Map<String, Object> values, TypeMirror type) {
        super(values, type);
    }

    @Override
    public void generateCode(MethodSpec.Builder builder, int paramId) {
        if(this.getType().getKind() == TypeKind.ARRAY) {
            generateArray(builder, paramId);
        }
        else {
            generateSingle(builder, paramId);
        }
    }
    
    private void generateArray(MethodSpec.Builder builder, int paramId) {
        builder.addStatement("param$L = new String[args.length - position]", paramId);
        builder.addStatement("if(param$L.length == 0) return new $T(null, $T.$L)", 
                paramId,
                DispatchResult.class,
                DispatchResultType.class,
                DispatchResultType.NO_VALID_ROUTE
        );
        builder.addStatement("System.arraycopy(args, position, param$L, 0, param$L.length)", paramId, paramId);
        builder.addStatement("position = args.length");
    }
    
    private void generateSingle(MethodSpec.Builder builder, int paramId) {
        consumeSafe(builder);
        builder.addStatement("param$L = arg", paramId);
    }
}