package com.spleefleague.annotations.processor.arguments;

import com.squareup.javapoet.MethodSpec.Builder;
import java.lang.reflect.InvocationTargetException;
import java.util.AbstractMap.SimpleEntry;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import com.spleefleague.annotations.DoubleArg;
import com.spleefleague.annotations.IntArg;
import com.spleefleague.annotations.LiteralArg;
import com.spleefleague.annotations.StringArg;

/**
 *
 * @author jonas
 */
public abstract class CommandArgument {
    
    private final Map<String, Object> values;
    private final TypeMirror type;
    
    public CommandArgument(Map<String, Object> values, TypeMirror type) {
        this.values = values;
        this.type = type;
    }
    
    protected <T> T getValue(String key) {
        return (T)values.get(key);
    }
    
    public abstract void generateCode(Builder builder, int paramId);
    
    public static CommandArgument create(TypeMirror type, AnnotationMirror annotation) throws NoSuchMethodException, InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        String name = ((TypeElement)annotation.getAnnotationType().asElement()).getQualifiedName().toString();
        Class<? extends CommandArgument> c =  argumentClasses.get(name);
        Map<? extends ExecutableElement, ? extends AnnotationValue> values = annotation.getElementValues();
        //Default values are not included
        Map<String, Object> simplifiedMap = values
                .entrySet()
                .stream()
                .map(e -> new SimpleEntry<String, Object>(
                        e.getKey().getSimpleName().toString(),
                        e.getValue().getValue()))
                .collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue()));
        //Read default values
        TypeElement elem = (TypeElement) annotation.getAnnotationType().asElement();
        for(Element e : elem.getEnclosedElements()) {
            ExecutableElement ee = (ExecutableElement)e;
            String key = ee.getSimpleName().toString();
            if(!simplifiedMap.containsKey(key)) {
                simplifiedMap.put(key, ee.getDefaultValue().getValue());
            }
        }
        return c.getConstructor(Map.class, TypeMirror.class).newInstance(simplifiedMap, type);
    }
    
    private static final Map<String, Class<? extends CommandArgument>> argumentClasses;
    
    public static final void registerArgumentClass(Class annotation, Class<? extends CommandArgument> argument) {
        argumentClasses.put(annotation.getName(), argument);
    }
    
    static {
        argumentClasses = new HashMap<>();
        argumentClasses.put(IntArg.class.getName(), IntArgument.class);
        argumentClasses.put(StringArg.class.getName(), StringArgument.class);
        argumentClasses.put(LiteralArg.class.getName(), LiteralArgument.class);
        argumentClasses.put(DoubleArg.class.getName(), DoubleArgument.class);
    }

    /**
     * @return the type
     */
    public TypeMirror getType() {
        return type;
    }
}
