

package org.praxislive.ide.pxr.palette;

import org.openide.nodes.FilterNode;
import org.openide.nodes.Node;

/**
 *
 * @author Neil C Smith <http://neilcsmith.net>
 */
class PaletteFilterNode extends FilterNode {

        PaletteFilterNode(Node original) {
            super(original, original.isLeaf() ? FilterNode.Children.LEAF
                    : new Children(original));
//            String html = getHtmlDisplayName();
//            if (html != null) {
//                setDisplayName("<html>" + html);
//            }
        }

        @Override
        public String getDisplayName() {
            String html = getHtmlDisplayName();
            if (html != null) {
                return "<html>" + html;
            } else {
                String name = super.getDisplayName().replace("_", ":");
                int index = name.lastIndexOf(":");
                if (isLeaf() && index > 0 && index < name.length() - 1) {
                    return name.substring(index + 1);
                } else {
                    return name;
                }
            }
        }

        @Override
        public String getHtmlDisplayName() {
            return getOriginal().getHtmlDisplayName();
        }


    private static class Children extends FilterNode.Children {

        private Children(Node original) {
            super(original);
        }

        @Override
        protected Node copyNode(Node original) {
            return new PaletteFilterNode(original);
        }
    }
    
}
