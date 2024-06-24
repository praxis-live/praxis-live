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
package org.praxislive.ide.pxr.graph;

import java.awt.Point;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;
import org.praxislive.core.types.PMap;
import org.praxislive.ide.pxr.spi.ModelTransform;
import org.praxislive.project.GraphBuilder;
import org.praxislive.project.GraphElement;
import org.praxislive.project.GraphModel;

/**
 *
 */
abstract sealed class PositionTransform {

    private static final String META = "meta";

    final GraphEditor editor;

    PositionTransform(GraphEditor editor) {
        this.editor = editor;
    }

    List<Point> findPositions(GraphModel model) {
        return model.root().children().values().stream()
                .flatMap(c -> findPosition(c).stream())
                .toList();
    }

    Optional<Point> findPosition(GraphElement.Component cmp) {
        return Optional.ofNullable(cmp.properties().get(META))
                .flatMap(p -> PMap.from(p.value()))
                .filter(m -> m.keys().containsAll(
                List.of(
                        GraphEditor.ATTR_GRAPH_X, GraphEditor.ATTR_GRAPH_Y)))
                .map(m -> new Point(m.getInt(GraphEditor.ATTR_GRAPH_X, 0),
                m.getInt(GraphEditor.ATTR_GRAPH_Y, 0)));
    }

    GraphModel offset(GraphModel model, Point offset) {
        return model.withTransform(r -> {
            r.transformChildren(children -> children
                    .map(e -> Map.entry(e.getKey(), offset(e.getValue(), offset)))
                    .toList());
        });
    }

    private GraphElement.Component offset(GraphElement.Component cmp, Point offset) {
        GraphBuilder.Component builder = GraphBuilder.component(cmp);
        GraphElement.Property metaProp = builder.properties().get(META);
        if (metaProp != null) {
            PMap existing = PMap.from(metaProp.value()).orElse(PMap.EMPTY);
            int x = existing.getInt(GraphEditor.ATTR_GRAPH_X, 0) + offset.x;
            int y = existing.getInt(GraphEditor.ATTR_GRAPH_Y, 0) + offset.y;
            PMap meta = PMap.merge(existing,
                    PMap.of(GraphEditor.ATTR_GRAPH_X, x, GraphEditor.ATTR_GRAPH_Y, y),
                    PMap.REPLACE);
            builder.transformProperties(props -> props
                    .map(e -> META.equals(e.getKey())
                    ? Map.entry(META, GraphElement.property(meta))
                    : e)
                    .toList());
        } else {
            PMap meta = PMap.of(GraphEditor.ATTR_GRAPH_X, offset.x,
                    GraphEditor.ATTR_GRAPH_Y, offset.y);
            builder.transformProperties(props -> Stream.concat(
                    Stream.of(Map.entry(META, GraphElement.property(meta))),
                            props)
                            .toList());
        }
        return builder.build();
    }

    static final class CopyExport extends PositionTransform
            implements ModelTransform.Copy, ModelTransform.Export {

        CopyExport(GraphEditor editor) {
            super(editor);
        }

        @Override
        public GraphModel apply(GraphModel model) {
            List<Point> positions = findPositions(model);
            int minX = positions.stream().mapToInt(p -> p.x).min().orElse(0);
            int minY = positions.stream().mapToInt(p -> p.y).min().orElse(0);
            return offset(model, new Point(-minX, -minY));
        }

    }

    static final class ImportPaste extends PositionTransform
            implements ModelTransform.Import, ModelTransform.Paste {

        ImportPaste(GraphEditor editor) {
            super(editor);
        }

        @Override
        public GraphModel apply(GraphModel model) {
            return offset(model, editor.getActivePoint());
        }

    }

}
