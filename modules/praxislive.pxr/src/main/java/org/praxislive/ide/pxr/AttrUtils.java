/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2024 Neil C Smith.
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
 * Please visit https://www.praxislive.org if you need additional information or
 * have any questions.
 */
package org.praxislive.ide.pxr;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.praxislive.core.protocols.ComponentProtocol;
import org.praxislive.core.types.PMap;
import org.praxislive.project.GraphBuilder;
import org.praxislive.project.GraphElement;
import org.praxislive.project.GraphModel;

/**
 *
 */
class AttrUtils {

    private AttrUtils() {
    }

    static GraphModel rewriteAttr(GraphModel model) {
        return model.withTransform(r -> rewriteAttrImpl(r));
    }

    private static void rewriteAttrImpl(GraphBuilder.Base<?> cmp) {
        PMap.Builder mb = PMap.builder();
        for (GraphElement.Comment comment : cmp.comments()) {
            String txt = comment.text().strip();
            if (txt.startsWith("%")) {
                int delim = txt.indexOf(" ");
                if (delim > 1) {
                    mb.put(txt.substring(1, delim), unescape(txt.substring(delim + 1)));
                }
            }
        }
        PMap attr = mb.build();
        cmp.transformProperties(props -> {
            List<Map.Entry<String, GraphElement.Property>> result = new ArrayList<>(props.toList());
            int index = -1;
            for (int i = 0; i < result.size(); i++) {
                if ("meta".equals(result.get(i).getKey())) {
                    index = i;
                    break;
                }
            }
            if (index > -1) {
                PMap existing = PMap.from(result.get(index).getValue().value()).orElseThrow();
                result.set(index, Map.entry(ComponentProtocol.META,
                        GraphElement.property(PMap.merge(existing, attr, PMap.REPLACE))));
            } else {
                result.add(0, Map.entry(ComponentProtocol.META, GraphElement.property(attr)));
            }
            return result;
        });

        cmp.transformComments(comments -> comments
                .filter(c -> !c.text().strip().startsWith("%"))
                .toList());

        cmp.transformChildren(children
                -> children.map(e -> {
                    GraphBuilder.Component child = GraphBuilder.component(e.getValue());
                    rewriteAttrImpl(child);
                    return Map.entry(e.getKey(), child.build());
                }).toList()
        );

    }

    private static String unescape(String text) {
        if (!text.contains("\\")) {
            return text;
        }
        int len = text.length();
        StringBuilder sb = new StringBuilder(len);
        for (int i = 0; i < len; i++) {
            char c = text.charAt(i);
            if (c == '\\') {
                i++;
                c = text.charAt(i);
                switch (c) {
                    case 'n':
                        sb.append('\n');
                        continue;
                    case 't':
                        sb.append('\t');
                        continue;
                    case 'r':
                        continue;
                }
            }
            sb.append(c);
        }
        return sb.toString();
    }

}
