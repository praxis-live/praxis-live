/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package net.neilcsmith.praxis.live.graph;

import java.util.Set;
import org.netbeans.api.visual.model.ObjectSceneEvent;
import org.netbeans.api.visual.model.ObjectSceneListener;
import org.netbeans.api.visual.model.ObjectState;

/**
 *
 * @author Neil C Smith (http://neilcsmith.net)
 */
public class ObjectSceneAdaptor implements ObjectSceneListener {

    @Override
    public void objectAdded(ObjectSceneEvent event, Object addedObject) {
    }

    @Override
    public void objectRemoved(ObjectSceneEvent event, Object removedObject) {
    }

    @Override
    public void objectStateChanged(ObjectSceneEvent event, Object changedObject, ObjectState previousState, ObjectState newState) {
    }

    @Override
    public void selectionChanged(ObjectSceneEvent event, Set<Object> previousSelection, Set<Object> newSelection) {
    }

    @Override
    public void highlightingChanged(ObjectSceneEvent event, Set<Object> previousHighlighting, Set<Object> newHighlighting) {
    }

    @Override
    public void hoverChanged(ObjectSceneEvent event, Object previousHoveredObject, Object newHoveredObject) {
    }

    @Override
    public void focusChanged(ObjectSceneEvent event, Object previousFocusedObject, Object newFocusedObject) {
    }

}
