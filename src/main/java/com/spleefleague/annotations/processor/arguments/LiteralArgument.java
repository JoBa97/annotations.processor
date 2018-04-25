package com.spleefleague.annotations.processor.arguments;

import com.spleefleague.annotations.DispatchResultType;
import com.squareup.javapoet.MethodSpec;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
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
        //Real type is: com.sun.tools.javac.code.Attribute$Constant
        //It is accessed through reflection, because this class (and it's module, jdk.compiler) 
        //is not exposed in Java 9
        List<?> l = super.getValue("aliases");
        aliases = new String[l.size()];
        for (int i = 0; i < l.size(); i++) {
            try {
                Object aliasConstant = l.get(i);
                Method m = aliasConstant.getClass().getMethod("getValue");
                aliases[i] = (String)m.invoke(aliasConstant);
            } catch (Exception ex) {
                Logger.getLogger(LiteralArgument.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        if(value == null) {
            throw new RuntimeException("Value can not be null");
        }
    }

    @Override
    public void generateCode(MethodSpec.Builder builder, int paramId) {
        consumeSafe(builder);
        builder.addStatement("param$L = arg", paramId);
        builder.addCode("boolean valid$L = param$L.equalsIgnoreCase($S)", paramId, paramId, value);
        for(String alias : aliases) {
            builder.addCode("\n|| (param$L.equalsIgnoreCase($S))", paramId, alias);
        }
        builder.addCode(";\n");
        builder.addCode("if(!valid" + paramId + ") ");
        returnResult(builder, null, DispatchResultType.NO_VALID_ROUTE);
    }
}