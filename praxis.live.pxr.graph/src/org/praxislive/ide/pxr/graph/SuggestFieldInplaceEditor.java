
package org.praxislive.ide.pxr.graph;

import java.util.List;
import org.netbeans.api.visual.action.TextFieldInplaceEditor;
import org.netbeans.api.visual.widget.Widget;

/**
 *
 */
interface SuggestFieldInplaceEditor extends TextFieldInplaceEditor {
    
    public List<String> getSuggestedValues(Widget widget);
    
}
