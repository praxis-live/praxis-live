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
package org.praxislive.ide.pxr.api;

import org.praxislive.core.Value;
import org.praxislive.core.protocols.ComponentProtocol;
import org.praxislive.core.types.PString;

/**
 * Implementations of Attributes can be requested from component proxy lookup to
 * store key/value data for individual components.
 * <p>
 * In earlier versions of PraxisLIVE, attributes were stored as script comments.
 * They are now backed by the {@link ComponentProtocol#META} property. This API
 * still provides an easier way to merge and retrieve attributes. To listen for
 * changes, use a property listener on the meta property.
 */
public interface Attributes {

    /**
     * Set an attribute. A {@code null} or empty value will clear the attribute.
     *
     * @param key attribute key
     * @param value attribute value
     */
    public void setAttribute(String key, String value);

    /**
     * Get an attribute. If not set then {@code null} is returned.
     *
     * @param key attribute key
     * @return attribute value or null
     */
    public String getAttribute(String key);

    /**
     * Set an attribute as a {@link Value}, or clear the attribute if the value
     * is {@code null} or empty (see {@link Value#isEmpty()}).
     *
     * @param key attribute key
     * @param value attribute value
     */
    public default void setAttributeValue(String key, Value value) {
        if (value == null || value.isEmpty()) {
            setAttribute(key, null);
        } else {
            setAttribute(key, value.toString());
        }
    }

    /**
     * Get an attribute as the specified Value type. If the attribute is not set
     * or cannot be converted to the given type this method will return
     * {@code null}.
     *
     * @param <T> value type
     * @param type value type as class
     * @param key attribute key
     * @return attribute as value type or null
     */
    public default <T extends Value> T getAttributeValue(Class<T> type, String key) {
        String attr = getAttribute(key);
        if (attr == null) {
            return null;
        } else if (type == PString.class || type == Value.class) {
            return type.cast(PString.of(attr));
        } else {
            return Value.Type.of(type).converter().apply(PString.of(attr)).orElse(null);
        }
    }

}
