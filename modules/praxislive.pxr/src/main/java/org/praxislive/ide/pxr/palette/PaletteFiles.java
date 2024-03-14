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
 * Please visit http://neilcsmith.net if you need additional information or
 * have any questions.
 */
package org.praxislive.ide.pxr.palette;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Set;
import java.util.TreeSet;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileSystem;
import org.openide.filesystems.FileUtil;
import org.openide.loaders.DataFolder;
import org.openide.util.Exceptions;
import org.praxislive.core.ComponentType;
import org.praxislive.ide.core.api.DynamicFileSystem;

/**
 *
 */
class PaletteFiles {

    final static String FOLDER = "PXR/Palette/";

    private static final PaletteFiles INSTANCE = new PaletteFiles();

    private final Set<ComponentType> knownTypes;
    private final FileSystem typeFileSystem;

    private PaletteFiles() {
        this.knownTypes = new TreeSet<>(Comparator.comparing(ComponentType::toString));
        this.typeFileSystem = FileUtil.createMemoryFileSystem();
        try {
            FileUtil.createFolder(typeFileSystem.getRoot(), FOLDER);
        } catch (IOException ex) {
            Exceptions.printStackTrace(ex);
        }
        DynamicFileSystem.getDefault().mount(typeFileSystem);
    }

    synchronized void addTypes(Collection<ComponentType> types) {
        if (knownTypes.containsAll(types)) {
            return;
        }
        Set<ComponentType> toAdd = new TreeSet<>(Comparator.comparing(ComponentType::toString));
        toAdd.addAll(types);
        toAdd.removeAll(knownTypes);
        try {
            typeFileSystem.runAtomicAction(() -> {
                toAdd.stream().forEachOrdered(this::addType);
                refreshCategoryOrder();
            });
        } catch (IOException ex) {
            Exceptions.printStackTrace(ex);
        }

    }

    DataFolder paletteFolder() {
        return DataFolder.findFolder(FileUtil.getConfigFile(FOLDER));
    }

    private void addType(ComponentType type) {
        try {
            String typeString = type.toString();
            String category = typeString.substring(0, typeString.lastIndexOf(":"));
            FileObject folder = getOrCreateCategory(category);
            FileObject typeFile = FileUtil.createData(folder, safeFileName(typeString) + ".type");
            typeFile.setAttribute(TypeDataObject.TYPE_ATTR_KEY, typeString);
            knownTypes.add(type);
        } catch (IOException ex) {
            Exceptions.printStackTrace(ex);
        }
    }

    private FileObject getOrCreateCategory(String category) throws IOException {
        FileObject categoryFolder = FileUtil.createFolder(typeFileSystem.getRoot(),
                FOLDER + safeFileName(category));
        categoryFolder.setAttribute("displayName", category);
        return categoryFolder;
    }

    private void refreshCategoryOrder() throws IOException {
        FileObject root = typeFileSystem.findResource(FOLDER);
        FileObject[] categories = root.getChildren();
        Arrays.sort(categories, Comparator.comparing(FileObject::getName,
                (n1, n2) -> {
                    if (n1.startsWith("core")) {
                        return n2.startsWith("core") ? n1.compareTo(n2) : -1;
                    } else {
                        return n2.startsWith("core") ? 1 : n1.compareTo(n2);
                    }
                }));
        FileUtil.setOrder(Arrays.asList(categories));
    }

    private String safeFileName(String fileName) {
        return fileName.replace(":", "_");
    }

    static PaletteFiles getDefault() {
        return INSTANCE;
    }

}
