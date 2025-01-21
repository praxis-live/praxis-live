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

import java.awt.EventQueue;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletionStage;
import javax.imageio.ImageIO;
import javax.swing.Timer;
import org.netbeans.api.visual.border.Border;
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

    private static final long FORCE_REFRESH_TIME = 5_000_000_000L;

    private final ComponentProxy cmp;
    private final String control;
    private final Timer timer;

    private CompletionStage<?> pending;
    private long pendingSent;

    WatchDisplay(Scene scene, ComponentProxy cmp,
            String control, String relatedPort) {
        super(scene);
        this.cmp = Objects.requireNonNull(cmp);
        this.control = Objects.requireNonNull(control);
        timer = new Timer(100, this::timerListener);
        setOpaque(true);
        setForeground(null);
        setBorder(BorderFactory.createRoundedBorder(4, 4, 2, 2, LAFScheme.NODE_BACKGROUND, null));
        setLayout(LayoutFactory.createVerticalFlowLayout());
        addChild(createLabel(scene, relatedPort.isBlank() ? control : relatedPort + " (watch)"));
    }

    private Widget createLabel(Scene scene, String text) {
        LabelWidget label = new LabelWidget(scene, text);
        label.setForeground(null);
        label.setBorder(new LabelBorderImpl());
        return label;
    }

    @Override
    protected void notifyAdded() {
        super.notifyAdded();
        pendingSent = System.nanoTime() - FORCE_REFRESH_TIME;
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
        if (pending != null && System.nanoTime() - pendingSent < FORCE_REFRESH_TIME) {
            return;
        }
        CompletionStage<List<Value>> staged = cmp.send(control, List.of());
        staged.whenComplete((args, ex) -> {
            assert EventQueue.isDispatchThread();
            if (args != null && !args.isEmpty()) {
                update(args.get(0));
            }
            if (pending == staged) {
                pending = null;
            }
        });
        pending = staged;
        pendingSent = System.nanoTime();

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

    private class LabelBorderImpl implements Border {

        private final Insets INSETS = new Insets(1, 1, 1, 1);

        @Override
        public Insets getInsets() {
            return INSETS;
        }

        @Override
        public void paint(Graphics2D g, Rectangle bounds) {
            g.setColor(getParentWidget().getForeground());
            int y = bounds.y + bounds.height - 1;
            g.drawLine(bounds.x, y, bounds.x + bounds.width, y);
        }

        @Override
        public boolean isOpaque() {
            return false;
        }

    }

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
        } else {
            return new TextDisplay(scene, cmp, control, port);
        }
    }

    private static class TextDisplay extends WatchDisplay {

        private final List<LabelWidget> lines;

        private String text;

        TextDisplay(Scene scene, ComponentProxy cmp,
                String control, String relatedPort) {
            super(scene, cmp, control, relatedPort);
            text = "";
            lines = List.of(
                    createLineWidget(),
                    createLineWidget(),
                    createLineWidget(),
                    createLineWidget(),
                    createLineWidget()
            );
            for (LabelWidget line : lines) {
                line.setVisible(false);
                addChild(line);
            }
        }

        @Override
        void update(Value value) {
            String newText = value.toString();
            if (!Objects.equals(text, newText)) {
                this.text = newText;
                List<String> ll = text.lines()
                        .limit(lines.size())
                        .map(this::truncate)
                        .toList();
                for (LabelWidget line : lines) {
                    line.setVisible(false);
                }
                for (int i = 0; i < ll.size(); i++) {
                    String txt = ll.get(i);
                    LabelWidget widget = lines.get(i);
                    widget.setLabel(txt);
                    widget.setVisible(true);
                }
                getScene().validate();
            }
        }

        private LabelWidget createLineWidget() {
            LabelWidget lineWidget = new LabelWidget(getScene());
            lineWidget.setForeground(null);
            return lineWidget;
        }

        private String truncate(String line) {
            return line.length() > 40 ? line.substring(0, 38) + "..." : line;
        }

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
