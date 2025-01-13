/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2025 Neil C Smith.
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

import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.List;
import java.util.Objects;
import javax.imageio.ImageIO;
import javax.swing.Timer;
import org.netbeans.api.visual.border.BorderFactory;
import org.netbeans.api.visual.layout.LayoutFactory;
import org.netbeans.api.visual.widget.LabelWidget;
import org.netbeans.api.visual.widget.Scene;
import org.netbeans.api.visual.widget.Widget;
import org.openide.util.Exceptions;
import org.praxislive.core.ControlInfo;
import org.praxislive.core.Value;
import org.praxislive.core.Watch;
import org.praxislive.core.protocols.StartableProtocol;
import org.praxislive.core.types.PBoolean;
import org.praxislive.core.types.PBytes;
import org.praxislive.core.types.PMap;
import org.praxislive.ide.model.ComponentProxy;
import org.praxislive.ide.model.ContainerProxy;
import org.praxislive.ide.model.RootProxy;
import org.praxislive.ide.properties.PraxisProperty;
import org.praxislive.ide.pxr.graph.scene.LAFScheme;
import org.praxislive.ide.pxr.graph.scene.PraxisGraphScene;

/**
 *
 */
abstract class WatchDisplay extends Widget {

    private final ComponentProxy cmp;
    private final String control;
    private final Timer timer;

    WatchDisplay(Scene scene, ComponentProxy cmp,
            String control, String relatedPort) {
        super(scene);
        this.cmp = Objects.requireNonNull(cmp);
        this.control = Objects.requireNonNull(control);
        timer = new Timer(100, this::timerListener);
        setOpaque(true);
        setForeground(null);
        setBorder(BorderFactory.createRoundedBorder(4, 4, 2, 2, LAFScheme.BACKGROUND, null));
        setLayout(LayoutFactory.createVerticalFlowLayout());
        addChild(createLabel(scene, relatedPort.isBlank() ? control : relatedPort + " (watch)"));
    }

    private Widget createLabel(Scene scene, String text) {
        LabelWidget label = new LabelWidget(scene, text);
        label.setForeground(null);
        return label;
    }

    @Override
    protected void notifyAdded() {
        super.notifyAdded();
        timer.start();
    }

    @Override
    protected void notifyRemoved() {
        super.notifyRemoved();
        timer.stop();
    }

    private void timerListener(ActionEvent e) {
        if (getScene() instanceof PraxisGraphScene<?> pgs && pgs.isBelowLODThreshold()) {
            return;
        }
        if (!isRunning()) {
            return;
        }
        cmp.send(control, List.of()).thenAccept(ret -> {
            if (!ret.isEmpty()) {
                update(ret.get(0));
            }
        });
    }

    private boolean isRunning() {
        ContainerProxy parent = cmp.getParent();
        while (parent != null) {
            if (parent instanceof RootProxy root) {
                PraxisProperty<?> running = root.getProperty(StartableProtocol.IS_RUNNING);
                if (running != null) {
                    return PBoolean.from(running.getValue()).orElse(PBoolean.FALSE).value();
                } else {
                    return true;
                }
            }
            parent = parent.getParent();
        }
        return false;
    }

    abstract void update(Value value);

    static WatchDisplay createWidget(PraxisGraphScene<?> scene,
            ComponentProxy cmp, String control) {
        ControlInfo info = cmp.getInfo().controlInfo(control);
        if (info == null || !Watch.isWatch(info)) {
            return null;
        }
        PMap watchInfo = PMap.from(info.properties().get("watch")).orElse(PMap.EMPTY);
        String mime = watchInfo.getString(Watch.MIME_KEY, "application/octet-stream");
        String port = watchInfo.getString(Watch.RELATED_PORT_KEY, "");
        if ("image/png".equals(mime)) {
            return new ImageDisplay(scene, cmp, control, port);
        }
        return null;
    }

    private static class ImageDisplay extends WatchDisplay {

        private final ScaledImageWidget imageWidget;

        private PBytes data;
        private BufferedImage image;

        ImageDisplay(Scene scene, ComponentProxy cmp,
                String control, String relatedPort) {
            super(scene, cmp, control, relatedPort);
            imageWidget = new ScaledImageWidget(scene);
            addChild(imageWidget);
        }

        @Override
        void update(Value value) {
            PBytes newData = PBytes.from(value).orElse(PBytes.EMPTY);
            if (!Objects.equals(data, newData)) {
                this.data = newData;
                if (data.isEmpty()) {
                    this.image = null;
                    imageWidget.setImage(null);
                } else {
                    try {
                        image = ImageIO.read(data.asInputStream());
                        imageWidget.setImage(image);
                    } catch (IOException ex) {
                        Exceptions.printStackTrace(ex);
                        this.image = null;
                        imageWidget.setImage(null);
                    }
                }
            }
            getScene().validate();
        }

    }

    private static class ScaledImageWidget extends Widget {

        private final static int DEFAULT_SIZE = 140;

        private BufferedImage image;

        ScaledImageWidget(Scene scene) {
            super(scene);
        }

        void setImage(BufferedImage image) {
            this.image = image;
            revalidate();
        }

        @Override
        protected Rectangle calculateClientArea() {
            if (image == null) {
                return super.calculateClientArea();
            } else {
                double width = image.getWidth();
                double height = image.getHeight();
                double maxDim = Math.max(width, height);
                double scale = DEFAULT_SIZE / maxDim;
                return new Rectangle((int) (scale * width + 0.5),
                        (int) (scale * height + 0.5));
            }
        }

        @Override
        protected void paintWidget() {
            if (image == null) {
                return;
            }
            Graphics2D g = getGraphics();
            Rectangle area = getClientArea();
            RenderingHints renderingHints = g.getRenderingHints();
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                    RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.drawImage(image, area.x, area.y, area.width, area.height, null);
            g.setRenderingHints(renderingHints);
        }

    }

}
