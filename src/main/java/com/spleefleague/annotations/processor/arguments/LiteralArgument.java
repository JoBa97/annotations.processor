package com.spleefleague.annotations.processor.arguments;

import com.spleefleague.annotations.DispatchResultType;
import com.squareup.javapoet.MethodSpec;
import com.sun.tools.javac.code.Attribute.Constant;
import java.util.List;
import java.util.Map;
import javax.lang.model.type.TypeMirror;

/**
 *
 * @author balsfull
 */
public class LiteralArgument extends CommandArgument {

    private final String value;
    private final String[] aliases;
    
    public LiteralArgument(Map<String, Object> values, TypeMirror type) {
        super(values, type);
        value = super.getValue("value");
        List<Constant> l = super.getValue("aliases");
        aliases = new String[l.size()];
        for (int i = 0; i < l.size(); i++) {
            aliases[i] = (String)l.get(i).getValue();
        }
        if(value == null) {
            throw new RuntimeException("Value can not be null");
        }
    }

    @Override
    public void generateCode(MethodSpec.Builder builder, int paramId) {
        consumeSafe(builder);
        builder.addStatement("param$L = arg", paramId);
        builder.addCode("boolean valid = param$L.equalsIgnoreCase($S)", paramId, value);
        for(String alias : aliases) {
            builder.addCode("\n|| (param$L.equalsIgnoreCase($S))", paramId, alias);
        }
        builder.addCode(";\n");
        builder.addCode("if(!valid) ");
        returnResult(builder, null, DispatchResultType.NO_VALID_ROUTE);
    }
}