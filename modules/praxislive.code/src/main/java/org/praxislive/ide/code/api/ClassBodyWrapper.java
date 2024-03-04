/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2022 Neil C Smith.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 3 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 3 for more details.
 *
 * You should have received a copy of the GNU General Public License version 3
 * along with this work; if not, see http://www.gnu.org/licenses/
 *
 *
 * Please visit http://neilcsmith.net if you need additional information or
 * have any questions.
 */
package org.praxislive.ide.code.api;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Wrap a provided class body with the specified class name, extended type,
 * implemented interfaces, and default imports.
 * <p>
 * Will parse out import statements in the body and add to default import
 * statements. Will parse out extends statement and override the configured
 * extended type unless set to ignore extends.
 * <p>
 * A filter can be added to control
 */
public final class ClassBodyWrapper {

    private static final String NEW_LINE = "\n";
    private static final String DEFAULT_CLASS_NAME = "$";
    private static final Filter DEFAULT_FILTER = new DefaultFilter();

    private List<String> defaultImports;
    private String className;
    private String extendedType;
    private List<String> implementedTypes;
    private boolean ignoreExtends;
    private Filter filter;

    private ClassBodyWrapper() {
        defaultImports = List.of();
        className = DEFAULT_CLASS_NAME;
        implementedTypes = List.of();
        ignoreExtends = false;
        filter = DEFAULT_FILTER;
    }

    public ClassBodyWrapper className(String className) {
        this.className = Objects.requireNonNull(className);
        return this;
    }

    public ClassBodyWrapper defaultImports(List<String> defaultImports) {
        this.defaultImports = List.copyOf(defaultImports);
        return this;
    }

    public ClassBodyWrapper extendsType(String extendedType) {
        this.extendedType = extendedType;
        return this;
    }

    public ClassBodyWrapper implementsTypes(List<String> implementedTypes) {
        this.implementedTypes = List.copyOf(implementedTypes);
        return this;
    }

    public ClassBodyWrapper ignoreExtends(boolean ignore) {
        this.ignoreExtends = ignore;
        return this;
    }

    public ClassBodyWrapper filter(Filter filter) {
        this.filter = filter == null ? DEFAULT_FILTER : filter;
        return this;
    }

    public String wrap(String source) {
        StringBuilder sb = new StringBuilder();

        Map<LineCategory, List<String>> partitionedSource = source.lines()
                .collect(Collectors.groupingBy(ClassBodyWrapper::categorize,
                        () -> new EnumMap<>(LineCategory.class),
                        Collectors.toList()));

        String superClass = extendedType;
        if (!ignoreExtends) {
            List<String> sourceExtends = partitionedSource.getOrDefault(
                    LineCategory.EXTENDS, List.of());
            if (!sourceExtends.isEmpty()) {
                Matcher matcher = EXTENDS_STATEMENT_PATTERN.matcher(sourceExtends.get(0));
                if (matcher.lookingAt()) {
                    superClass = matcher.group(1);
                }
            }
        }

        List<String> sourceImports = partitionedSource.getOrDefault(
                LineCategory.IMPORT, List.of());
        List<String> sourceBody = partitionedSource.getOrDefault(
                LineCategory.BODY, List.of());

        // Break the class name up into package name and simple class name.
        String packageName; // null means default package.
        String simpleClassName;
        int idx = this.className.lastIndexOf('.');
        if (idx == -1) {
            packageName = "";
            simpleClassName = this.className;
        } else {
            packageName = this.className.substring(0, idx);
            simpleClassName = this.className.substring(idx + 1);
        }

        filter.writePackage(sb, packageName);
        filter.writeDefaultImports(sb, defaultImports);
        filter.writeSourceImports(sb, sourceImports);
        filter.writeClassDeclaration(sb, simpleClassName, superClass, implementedTypes);
        filter.writeSourceBody(sb, sourceBody);
        filter.writeClassEnding(sb);

        return sb.toString();

    }

    public static ClassBodyWrapper create() {
        return new ClassBodyWrapper();
    }

    private static final Pattern IMPORT_STATEMENT_PATTERN = Pattern.compile(
            "\\s*import\\s+"
            + "("
            + "(?:static\\s+)?"
            + "\\p{javaJavaIdentifierStart}\\p{javaJavaIdentifierPart}*"
            + "(?:\\.\\p{javaJavaIdentifierStart}\\p{javaJavaIdentifierPart}*)*"
            + "(?:\\.\\*)?"
            + ");"
    );

    private static final Pattern EXTENDS_STATEMENT_PATTERN = Pattern.compile(
            "\\s*extends\\s+"
            + "("
            + "\\p{javaJavaIdentifierStart}\\p{javaJavaIdentifierPart}*"
            + "(?:\\.\\p{javaJavaIdentifierStart}\\p{javaJavaIdentifierPart}*)*"
            + "(?:\\.\\*)?"
            + ");"
    );

    private static enum LineCategory {
        BODY, IMPORT, EXTENDS
    };

    private static LineCategory categorize(String line) {
        if (IMPORT_STATEMENT_PATTERN.matcher(line).lookingAt()) {
            return LineCategory.IMPORT;
        } else if (EXTENDS_STATEMENT_PATTERN.matcher(line).lookingAt()) {
            return LineCategory.EXTENDS;
        } else {
            return LineCategory.BODY;
        }
    }

    /**
     * A filter implementation can be used to override the default textual
     * output of the various sections of the class. All methods have a default
     * implementation that can be overridden or wrapped.
     */
    public static interface Filter {

        /**
         * Write the package declaration.
         *
         * @param sb StringBuilder to append to.
         * @param packageName package name to declare
         */
        public default void writePackage(StringBuilder sb, String packageName) {
            if (!packageName.isEmpty()) {
                sb.append("package ").append(packageName).append(";").append(NEW_LINE);
            }
        }

        /**
         * Write the default imports. These are the imports provided by the
         * wrapper as opposed to those declared in the source body.
         * <p>
         * Each import String does not include the keyword <code>import</code>
         * or the ending semicolon. It might contain the keyword
         * <code>static</code>.
         *
         * @param sb StringBuilder to append to.
         * @param imports list of imports.
         */
        public default void writeDefaultImports(StringBuilder sb, List<String> imports) {
            if (!imports.isEmpty()) {
                imports.forEach(i
                        -> sb.append("import ").append(i).append(";").append(NEW_LINE)
                );
            }
        }

        /**
         * Write the import lines included in the source body. Each line is
         * included as written, with <code>import</code> and semicolon.
         *
         * @param sb StringBuilder to append to.
         * @param imports import lines
         */
        public default void writeSourceImports(StringBuilder sb, List<String> imports) {
            imports.forEach(line -> sb.append(line).append(NEW_LINE));
        }

        /**
         * Write the class declaration.
         *
         * @param sb StringBuilder to append to.
         * @param className name of the class.
         * @param extendedType name of the super class (may be null).
         * @param implementedTypes list of implemented interfaces.
         */
        public default void writeClassDeclaration(StringBuilder sb, String className,
                String extendedType, List<String> implementedTypes) {
            sb.append("public class ").append(className);
            if (extendedType != null) {
                sb.append(" extends ").append(extendedType);
            }
            if (!implementedTypes.isEmpty()) {
                sb.append(" implements ");
                sb.append(implementedTypes.stream()
                        .collect(Collectors.joining(", ")));
            }
            sb.append(" {").append(NEW_LINE);
        }

        /**
         * Write the actual source body. Each line is as written, with any
         * <code>import</code> and <code>extends</code> lines removed.
         *
         * @param sb StringBuilder to append to.
         * @param body lines of the source body.
         */
        public default void writeSourceBody(StringBuilder sb, List<String> body) {
            body.forEach(line -> sb.append(line).append(NEW_LINE));
        }

        /**
         * Write the class ending (eg. closing brace).
         * 
         * @param sb StringBuilder to append to.
         */
        public default void writeClassEnding(StringBuilder sb) {
            sb.append("}").append(NEW_LINE);
        }

    }

    private static class DefaultFilter implements Filter {
    }
}
