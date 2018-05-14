/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2014 Neil C Smith.
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

package org.praxislive.ide.pxr;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.praxislive.core.services.ComponentFactory;
import org.praxislive.core.ComponentType;
import org.praxislive.ide.components.api.Components;
import org.praxislive.meta.TypeRewriter;
import org.praxislive.ide.pxr.PXRParser.ComponentElement;
import org.praxislive.ide.pxr.PXRParser.RootElement;



/**
 *
 * @author Neil C Smith <http://neilcsmith.net>
 */
class ElementRewriter {
    
    private final RootElement root;
    private final List<String> warnings;
    private final Map<ComponentType, Info> INFO_MAP;
    private final Info EMPTY;
    
    ElementRewriter(RootElement root, List<String> warnings) {
        this.root = root;
        this.warnings = warnings;
        INFO_MAP = new HashMap<>();
        EMPTY = new Info();             
    }
    
    void process() {
        rewriteRoot(root);
        processChildren(root);
    }
    
    private void processChildren(ComponentElement component) {
        for (ComponentElement child : component.children) {
            rewriteComponent(child);
            processChildren(child);
        }
    }
    
    private void rewriteRoot(RootElement root) {
        
    }
    
    private void rewriteComponent(ComponentElement component) {
        Info info = getInfo(component);
        if (info != EMPTY
                && info.newType != null 
                && info.rewriter != null 
                && TypeRewriter.isIdentity(info.rewriter)) {
//            warn("Rewriting deprecated " + component.type + " as " + info.newType);
            component.type = info.newType;
            
        }
    }
    
    private Info getInfo(ComponentElement component) {
        Info info = INFO_MAP.get(component.type);
        if (info == null) {
            ComponentFactory.MetaData<?> data =
                    Components.getMetaData(component.type);
            if (data != null && data.isDeprecated() && data.findReplacement().isPresent()) {
                ComponentType newType = data.findReplacement().get();
                TypeRewriter rewriter = data.getLookup().find(TypeRewriter.class).orElse(null);
                if (newType != null && rewriter != null && TypeRewriter.isIdentity(rewriter)) {
                    info = new Info();
                    info.newType = newType;
                    info.rewriter = rewriter;
                    INFO_MAP.put(component.type, info);
                    warn("Rewriting deprecated " + component.type + " as " + info.newType);
                    return info;
                }
            }
            INFO_MAP.put(component.type, EMPTY);
            info = EMPTY;
        }
        return info;
    }
    
    
    private void warn(String msg) {
        if (warnings != null) {
            warnings.add(msg);
        }
    }
    
    private static class Info {
        private ComponentType newType;
        private TypeRewriter rewriter;
    }
    
}
