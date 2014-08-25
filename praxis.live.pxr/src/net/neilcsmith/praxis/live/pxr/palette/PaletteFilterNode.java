

package net.neilcsmith.praxis.live.pxr.palette;

import org.openide.loaders.DataFolder;
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
                return super.getDisplayName();
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
