

package org.praxislive.ide.pxr;

import java.lang.reflect.InvocationTargetException;
import org.praxislive.core.ArgumentFormatException;
import org.praxislive.core.types.PBoolean;
import org.openide.nodes.Node;

/**
 *
 * @author Neil C Smith <http://neilcsmith.net>
 */
class BooleanPropertyWrapper extends Node.Property<Boolean> {
    private final BoundArgumentProperty wrapped;

    BooleanPropertyWrapper(BoundArgumentProperty wrapped) {
        super(Boolean.class);
        this.wrapped = wrapped;
    }

    @Override
    public boolean canRead() {
        return wrapped.canRead();
    }

    @Override
    public Boolean getValue() throws IllegalAccessException, InvocationTargetException {
        try {
            return PBoolean.coerce(wrapped.getValue()).value();
        } catch (ArgumentFormatException ex) {
            return false;
        }
    }

    @Override
    public boolean canWrite() {
        return wrapped.canWrite();
    }

    @Override
    public void setValue(Boolean val) throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        wrapped.setValue(val ? PBoolean.TRUE : PBoolean.FALSE);
    }

    @Override
    public String getName() {
        return wrapped.getName();
    }

    @Override
    public boolean supportsDefaultValue() {
        return wrapped.supportsDefaultValue();
    }

    @Override
    public void restoreDefaultValue() {
        wrapped.restoreDefaultValue();
    }

    @Override
    public String getHtmlDisplayName() {
        return wrapped.getHtmlDisplayName();
    }

    @Override
    public String getDisplayName() {
        return wrapped.getDisplayName();
    }
    
}
