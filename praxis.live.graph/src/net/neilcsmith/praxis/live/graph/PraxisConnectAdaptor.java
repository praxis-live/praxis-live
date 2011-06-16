/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.neilcsmith.praxis.live.graph;

import java.awt.Point;
import org.netbeans.api.visual.action.ConnectProvider;
import org.netbeans.api.visual.action.ConnectorState;
import org.netbeans.api.visual.widget.Scene;
import org.netbeans.api.visual.widget.Widget;

/**
 *
 * @author Neil C Smith (http://neilcsmith.net)
 */
public abstract class PraxisConnectAdaptor implements ConnectProvider {

    @Override
    public boolean isSourceWidget(Widget sourceWidget) {
        return sourceWidget instanceof PinWidget;
    }

    @Override
    public ConnectorState isTargetWidget(Widget sourceWidget, Widget targetWidget) {
        if (sourceWidget instanceof PinWidget && targetWidget instanceof PinWidget) {
            if (canConnect((PinWidget) sourceWidget, (PinWidget) targetWidget)) {
                return ConnectorState.ACCEPT;
            }
        }
        return ConnectorState.REJECT;

    }

    public boolean canConnect(PinWidget source, PinWidget target) {
        return true;
    }

    @Override
    public boolean hasCustomTargetWidgetResolver(Scene scene) {
        return false;
    }

    @Override
    public Widget resolveTargetWidget(Scene scene, Point sceneLocation) {
        return null;
    }

    @Override
    public void createConnection(Widget sourceWidget, Widget targetWidget) {
        createConnection((PinWidget) sourceWidget, (PinWidget) targetWidget);
    }

    public abstract void createConnection(PinWidget source, PinWidget target);

}
