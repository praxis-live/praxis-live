/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2011 Neil C Smith.
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
package net.neilcsmith.praxis.live.pxr;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import net.neilcsmith.praxis.core.Argument;
import net.neilcsmith.praxis.core.ComponentAddress;
import net.neilcsmith.praxis.core.ComponentType;
import net.neilcsmith.praxis.core.PortAddress;
import net.neilcsmith.praxis.core.syntax.Token;
import static net.neilcsmith.praxis.core.syntax.Token.Type.*;
import net.neilcsmith.praxis.core.syntax.Tokenizer;
import net.neilcsmith.praxis.core.types.PString;

/**
 *
 * @author Neil C Smith (http://neilcsmith.net)
 */
class PXRParser {

    private final static String AT = "@";
    private final static String CONNECT = "~";
    private final static String ATTRIBUTE_PREFIX = "%";
    private final static String PROPERTY_PREFIX = ".";
    private final static String RELATIVE_ADDRESS_PREFIX = "./";
    private final static AttributeElement[] EMPTY_ATTRS = new AttributeElement[0];
    private final static PropertyElement[] EMPTY_PROPS = new PropertyElement[0];
    private final static ComponentElement[] EMPTY_COMPS = new ComponentElement[0];
    private final static ConnectionElement[] EMPTY_CONS = new ConnectionElement[0];
//    private final static Argument[] EMPTY_ARGS = new Argument[0];
    private final String script;
    private final ComponentAddress context;

    private PXRParser(String script) {
        this(null, script);
    }
    
    private PXRParser(ComponentAddress context, String script) {
        this.script = script;
        this.context = context;
    }
    
    private RootElement doParse() throws ParseException {
        if (context == null) {
            return parseFullGraph();
        } else {
            return parseSubGraph();
        }
    }

    private RootElement parseFullGraph() throws ParseException {
        try {
            Iterator<Token> tokens = new Tokenizer(script).iterator();
            RootElement root = null;

            // ignore comments and white space at beginning of file

            Token t = nextNonCommentOrWhiteSpace(tokens);
            if (t != null && t.getType() == PLAIN && AT.equals(t.getText())) {
                root = new RootElement();
            }

            if (root == null) {
                throw new IllegalArgumentException("No root found in script.");
            }

            parseRootElement(root, tokens);

            // ignore comments and white space at end of file
            t = nextNonCommentOrWhiteSpace(tokens);

            if (t != null) {
                throw new IllegalArgumentException("Unexpected commands found after root.");
            }

            return root;

        } catch (Exception ex) {
            throw new ParseException(ex);
        }
    }
    
    private RootElement parseSubGraph() throws ParseException {
        
        try {
            RootElement root = new RootElement();
            root.address = context;
            parseComponentBody(root, script);
            return root;
            
        } catch (Exception ex) {
            throw new ParseException(ex);
        }
        
    }
    

    private static Token nextNonCommentOrWhiteSpace(Iterator<Token> tokens) {
        while (tokens.hasNext()) {
            Token t = tokens.next();
            if (t.getType() == COMMENT
                    || t.getType() == EOL) {
                continue;
            }
            return t;
        }
        return null;
    }

    private static Token[] tokensToEOL(Iterator<Token> tokens) {
        List<Token> tks = new ArrayList<Token>();
        while (tokens.hasNext()) {
            Token t = tokens.next();
            if (t.getType() == EOL) {
                break;
            }
            tks.add(t);
        }
        return tks.toArray(new Token[tks.size()]);
    }

    private void parseRootElement(RootElement root, Iterator<Token> tokens) throws Exception {

        Token t;
        if (tokens.hasNext() && (t = tokens.next()).getType() == PLAIN) {
            root.address = ComponentAddress.valueOf(t.getText());
        } else {
            throw new IllegalArgumentException("No root address found.");
        }
        if (tokens.hasNext() && (t = tokens.next()).getType() == PLAIN) {
            root.type = ComponentType.valueOf(t.getText());
        } else {
            throw new IllegalArgumentException("No root type found.");
        }

        if (tokens.hasNext()) {
            t = tokens.next();
            if (t.getType() == BRACED) {
                parseComponentBody(root, t.getText());
                return;
            } else if (t.getType() == EOL) {
                parseComponentBody(root, null);
                return;
            }
        }
        throw new IllegalArgumentException("Root body format error");
    }

    private void parseComponentBody(ComponentElement element, String body) throws Exception {
        // parse empty body
        if (body == null || body.trim().isEmpty()) {
            element.attributes = EMPTY_ATTRS;
            element.properties = EMPTY_PROPS;
            element.children = EMPTY_COMPS;
            element.connections = EMPTY_CONS;
            return;
        }

        // build helper lists - can't be fields because of recursion!
        List<AttributeElement> attrs = new ArrayList<AttributeElement>();
        List<PropertyElement> props = new ArrayList<PropertyElement>();
        List<ComponentElement> comps = new ArrayList<ComponentElement>();
        List<ConnectionElement> cons = new ArrayList<ConnectionElement>();
        Iterator<Token> tokens = new Tokenizer(body).iterator();
        while (tokens.hasNext()) {
            Token t = tokens.next();
            String txt = t.getText();
            switch (t.getType()) {
                case COMMENT:
                    // comment start is trimmed of whitespace by tokenizer
                    if (txt.startsWith(ATTRIBUTE_PREFIX)) {
//                        parseAttribute(attrs, element.address, t.getText());
                        parseAttribute(attrs, element, t.getText());
                    }
                    break;
                case PLAIN:
                    if (txt.startsWith(PROPERTY_PREFIX) && txt.length() > 1) {
                        parseProperty(props, element, txt.substring(1), tokensToEOL(tokens));
//                        parseProperty(props, element.address, txt.substring(1), tokensToEOL(tokens));
                    } else if (AT.equals(txt)) {
                        parseComponent(element, comps, tokensToEOL(tokens));
                    } else if (CONNECT.equals(txt)) {
                        parseConnection(element, cons, tokensToEOL(tokens));
                    } else {
                        throw new IllegalArgumentException("Unexpected PLAIN token : " + txt);
                    }
                    break;
                case EOL:
                    // no op
                    break;
                default:
                    throw new IllegalArgumentException(
                            "Unexpected token of type : " + t.getType() + " , body : " + txt);

            }
        }
        element.attributes = attrs.toArray(EMPTY_ATTRS);
        element.properties = props.toArray(EMPTY_PROPS);
        element.children = comps.toArray(EMPTY_COMPS);
        element.connections = cons.toArray(EMPTY_CONS);

    }

//    private void parseAttribute(List<AttributeElement> attrs, ComponentAddress component, String body) throws Exception {
    private void parseAttribute(List<AttributeElement> attrs, ComponentElement component, String body) throws Exception {

        Iterator<Token> tokens = new Tokenizer(body).iterator();
        String key = null;
        String value = null;
        Token t;
        if (tokens.hasNext()) {
            t = tokens.next();
            if (t.getType() == PLAIN && t.getText().length() > 1) {
                key = t.getText().substring(1);
            }
        }
        if (tokens.hasNext()) {
            t = tokens.next();
            if (t.getType() != EOL) {
                value = t.getText();
            }
        }
        if (key != null && value != null) {
            AttributeElement a = new AttributeElement();
            a.component = component;
            a.key = key;
            a.value = value;
            attrs.add(a);
        } else {/*?*/

        }
    }

    private void parseProperty(List<PropertyElement> props, ComponentElement component,
            String property, Token[] tokens) throws Exception {
        if (tokens.length == 0) {
            throw new IllegalArgumentException("Empty tokens passed to parseProperty " + component + "." + property);
        }
//        PropertyElement p = new PropertyElement();
//        p.address = ControlAddress.create(component, property);

        Argument[] args = new Argument[tokens.length];
        Token t;
        for (int i = 0; i < args.length; i++) {
            t = tokens[i];
            switch (t.getType()) {
                case PLAIN:
                case QUOTED:
                case BRACED:
                    // do proper evaluation of plain tokens for numbers, etc.
                    args[i] = PString.valueOf(t.getText());
                    break;
                case SUBCOMMAND:
                    args[i] = new SubCommandArgument(t.getText());
                    break;
                default:
                    throw new IllegalArgumentException("Unexpected token type in parseProperty "
                            + component.address + "." + property);
            }
        }
        PropertyElement p = new PropertyElement();
        p.component = component;
        p.property = property;
        p.args = args;
        props.add(p);
    }

    private void parseComponent(ComponentElement parent, List<ComponentElement> comps, Token[] tokens) throws Exception {
        if (tokens.length < 2 || tokens.length > 3) {
            throw new IllegalArgumentException("Unexpected number of tokens in parseComponent child of " + parent.address);
        }
        // next token should be relative component address
        ComponentAddress address = null;
        ComponentType type = null;
        Token t = tokens[0];
        if (t.getType() == PLAIN && t.getText().startsWith(RELATIVE_ADDRESS_PREFIX)) {
            address = ComponentAddress.create(parent.address,
                    t.getText().substring(RELATIVE_ADDRESS_PREFIX.length()));
        }
        t = tokens[1];
        if (t.getType() == PLAIN) {
            type = ComponentType.create(t.getText());
        }
        if (address == null || type == null) {
            throw new IllegalArgumentException("Invalid component creation line : " + address);
        }
        ComponentElement comp = new ComponentElement();
        comp.address = address;
        comp.type = type;
        if (tokens.length == 2) {
            parseComponentBody(comp, null);
        } else if (tokens[2].getType() == BRACED) {
            parseComponentBody(comp, tokens[2].getText());
        } else {
            throw new IllegalArgumentException("Invalid token at end of component line : " + address);
        }
        comps.add(comp);
    }

    private void parseConnection(ComponentElement parent, List<ConnectionElement> cons, Token[] tokens) {
        if (tokens.length != 2) {
            throw new IllegalArgumentException("Unexpected number of tokens in parseConnection of " + parent.address);
        }
        PortAddress p1 = parsePortAddress(parent.address, tokens[0]);
        PortAddress p2 = parsePortAddress(parent.address, tokens[1]);
        ConnectionElement con = new ConnectionElement();
        con.container = parent;
        con.component1 = p1.getComponentAddress().getID();
        con.port1 = p1.getID();
        con.component2 = p2.getComponentAddress().getID();
        con.port2 = p2.getID();
        cons.add(con);
    }

    private PortAddress parsePortAddress(ComponentAddress context, Token token) {
        if (token.getType() == PLAIN) {
            String txt = token.getText();
            if (txt.startsWith(RELATIVE_ADDRESS_PREFIX)) {
                return PortAddress.create(context + txt.substring(1));
            }
        }
        throw new IllegalArgumentException("Invalid token in parsePortAddress() - context : " + context + " , token : " + token.getText());
    }

    public static RootElement parse(String script) throws ParseException {
        if (script == null) {
            throw new NullPointerException();
        }
        return new PXRParser(script).doParse();

    }
    
    public static RootElement parseInContext(ComponentAddress context, String script) throws ParseException {
        if (context == null || script == null) {
            throw new NullPointerException();
        }
        return new PXRParser(context, script).doParse();
        
    }

    public static class ParseException extends Exception {

        public ParseException(Throwable cause) {
            super(cause);
        }
    }

    public static class Element {
    }

    public static class ComponentElement extends Element {

        public ComponentAddress address;
        public ComponentType type;
        public AttributeElement[] attributes;
        public PropertyElement[] properties;
        public ComponentElement[] children;
        public ConnectionElement[] connections;
    }

    public static class RootElement extends ComponentElement {
    }

    public static class AttributeElement extends Element {

        public ComponentElement component;
        public String key;
        public String value;
    }

    public static class PropertyElement extends Element {

        public ComponentElement component;
        public String property;
        public Argument[] args;
    }

    public static class ConnectionElement extends Element {

        public ComponentElement container;
        public String component1;
        public String port1;
        public String component2;
        public String port2;
    }
}
