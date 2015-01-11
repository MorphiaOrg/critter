package com.antwerkz.critter;

import com.sun.tools.javac.code.Symbol.VarSymbol;
import com.sun.tools.javac.code.Type;
import static java.lang.String.format;
import org.mongodb.morphia.annotations.Id;

import javax.lang.model.element.Element;
import javax.lang.model.element.VariableElement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class CritterField implements Comparable<CritterField> {
    private final boolean id;
    private final String rawTypePackage;
    private String name;
    private String type;

    private String rawType;
    private List<String> parameters = new ArrayList<>();

    public CritterField(final VariableElement field) {
        this.name = field.getSimpleName().toString();
        this.type = field.asType().toString();
        id = field.getAnnotation(Id.class) != null;
        final Element enclosingElement = field.getEnclosingElement();
        if (field instanceof VarSymbol) {
            VarSymbol symbol = (VarSymbol) field;
            final List<Type> typeParameters = symbol.asType().getTypeArguments();
            if (typeParameters.isEmpty()) {
                rawType = type;
            } else {
                for (Type typeParameter : typeParameters) {
                    System.out.println("typeParameter = " + typeParameter);
                    parameters.add(typeParameter.toString());
                }
            }
            rawType = symbol.asType().asElement().getQualifiedName().toString();
            System.out.println("symbol = " + symbol);
        }
        rawTypePackage = extractPackage(rawType);
    }

    public boolean isId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getRawType() {
        return rawType;
    }

    public String getType() {
        return type;
    }

    private String extractPackage(final String type) {
        String pkg = type.substring(type.indexOf('$') + 1);
        pkg = pkg.substring(0, pkg.lastIndexOf('.'));
        return pkg;
    }

    public Boolean isContainerType() {
        try {
            return Collection.class.isAssignableFrom(Class.forName(rawType));
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    public Boolean isNumericType() {
        try {
            return Number.class.isAssignableFrom(Class.forName(getRawType()));
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    public String getCriteriaType() {
        final Class<?> aClass;
        try {
            aClass = Class.forName(isContainerType() && parameters.size() == 1 ? parameters.get(0) : rawType);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
        return format("%s.criteria.%sCriteria", aClass.getPackage().getName(), aClass.getSimpleName());
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("Field {");
        sb.append("name='").append(name).append('\'');
        sb.append(", type='").append(type).append('\'');
        sb.append('}');
        return sb.toString();
    }

    @Override
    public int compareTo(CritterField o) {
        return name.compareTo(o.name);
    }
}
