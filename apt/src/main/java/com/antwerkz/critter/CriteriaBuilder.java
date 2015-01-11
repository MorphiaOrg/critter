/**
 * Copyright (C) 2012-2013 Justin Lee <jlee@antwerkz.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.antwerkz.critter;

import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.Type.ClassType;
import freemarker.cache.ClassTemplateLoader;
import freemarker.template.Configuration;
import freemarker.template.DefaultObjectWrapper;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import org.mongodb.morphia.annotations.Embedded;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Reference;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static java.util.Arrays.asList;

public class CriteriaBuilder extends AbstractProcessor {

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.RELEASE_7;
    }

    @Override
    public Set<String> getSupportedOptions() {
        return Collections.singleton("outputDirectory");
    }

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        return new HashSet<>(asList(Entity.class.getName(), Embedded.class.getName()));
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        final TypeElement element = processingEnv.getElementUtils().getTypeElement(Entity.class.getName());
        if (annotations.contains(element)) {
            try {
                Configuration cfg = new Configuration();
                cfg.setObjectWrapper(new DefaultObjectWrapper());
                cfg.setTemplateLoader(new ClassTemplateLoader(getClass(), "/templates"));

                for (TypeElement typeElement : ElementFilter.typesIn(roundEnv.getElementsAnnotatedWith(Entity.class))) {
                    new CritterClass(processingEnv, cfg.getTemplate("criteria.ftl"), typeElement).generate();
                }

                for (TypeElement typeElement : ElementFilter.typesIn(roundEnv.getElementsAnnotatedWith(Embedded.class))) {
                    new CritterClass(processingEnv, cfg.getTemplate("embedded.ftl"), typeElement).generate();
                }
                return true;
            } catch (IOException | TemplateException e) {
                throw new RuntimeException(e.getMessage(), e);
            }
        } else {
            return false;
        }
    }

    private String encodeEmbedName(VariableElement field) {
        TypeMirror typeMirror = field.asType();
        ClassType classType = (ClassType) typeMirror;
        List<Type> typeArguments = classType.getTypeArguments();
        String[] parts;
        if (typeArguments.size() == 1) {
            parts = typeArguments.get(0).toString().split("\\.");
        } else {
            parts = typeMirror.toString().split("\\.");
        }
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < parts.length; i++) {
            String part = parts[i];
            if (builder.length() != 0) {
                if (i == parts.length - 1) {
                    builder.append(".criteria.");
                } else {
                    builder.append(".");
                }
            }
            builder.append(part);
        }
        return builder.toString();
    }
}
