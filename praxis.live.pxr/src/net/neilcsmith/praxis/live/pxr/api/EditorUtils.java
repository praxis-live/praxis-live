/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2013 Neil C Smith.
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
 * Please visit http://neilcsmith.net if you need additional information or
 * have any questions.
 */
package net.neilcsmith.praxis.live.pxr.api;

import java.util.Set;
import net.neilcsmith.praxis.core.ComponentType;

/**
 * Support functions for editors
 *
 * @author Neil C Smith
 */
public class EditorUtils {
    
    private EditorUtils(){}
    
    public static String extractBaseID(ComponentType type) {
        String base = type.toString();
        return (base.substring(base.lastIndexOf(":") + 1));
    }
    
    public static String findFreeID(Set<String> existing, String baseID, boolean forceSuffix) {
        if (!forceSuffix && !existing.contains(baseID)) {
            // easy case - return value passed in
            return baseID;
        }
        
        if (forceSuffix) {
            if (Character.isDigit(baseID.charAt(baseID.length() - 1))) {
                baseID += "-";
            }
        } else {
            int len = baseID.length();
            for (; len > 0; len--) {
                if (!Character.isDigit(baseID.charAt(len-1))) {
                    break;
                }
            }
            if (len == 0) {
                return "";
            }
            if (len != baseID.length()) {
                baseID = baseID.substring(0, len);
            }
        }
        
        for (int i=1; i<1000; i++) {
            String id = baseID + i;
            if (!existing.contains(id)) {
                return id;
            }
        }
        
        return "";
    }
    
    
}
