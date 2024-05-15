/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2020 Neil C Smith.
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
package org.praxislive.ide.pxr;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.netbeans.api.project.FileOwnerQuery;
import org.netbeans.spi.project.ProjectServiceProvider;
import org.openide.filesystems.FileObject;
import org.openide.util.Lookup;
import org.praxislive.ide.model.RootProxy;
import org.praxislive.ide.project.api.PraxisProject;
import org.praxislive.ide.project.spi.RootRegistry;

/**
 *
 */
@ProjectServiceProvider(projectType = PraxisProject.TYPE,
        service = {RootRegistry.class, PXRRootRegistry.class})
public class PXRRootRegistry implements RootRegistry {

    private final PraxisProject project;
    private final Set<PXRRootProxy> roots;
    private final PropertyChangeSupport pcs;

    public PXRRootRegistry(Lookup lookup) {
        this.project = Objects.requireNonNull(lookup.lookup(PraxisProject.class));
        this.roots = new LinkedHashSet<>();
        pcs = new PropertyChangeSupport(this);
    }

    @Override
    public Optional<RootProxy> find(String id) {
        return roots.stream()
                .filter(r -> r.getAddress().rootID().equals(id))
                .map(RootProxy.class::cast)
                .findFirst();
    }

    @Override
    public List<RootProxy> findAll() {
        return roots.stream().collect(Collectors.toList());
    }

    @Override
    public void addPropertyChangeListener(PropertyChangeListener listener) {
        pcs.addPropertyChangeListener(listener);
    }

    @Override
    public void removePropertyChangeListener(PropertyChangeListener listener) {
        pcs.removePropertyChangeListener(listener);
    }

    void register(PXRRootProxy root) {
        if (root == null) {
            throw new NullPointerException();
        }
        if (roots.contains(root)) {
            throw new IllegalArgumentException();
        }
        roots.add(root);
        fireRootsChange();
    }

    void remove(PXRRootProxy root) {
        roots.remove(root);
        fireRootsChange();
    }

    private void fireRootsChange() {
        pcs.firePropertyChange("roots", null, null);
    }

    PXRRootProxy[] getRoots() {
        return roots.toArray(new PXRRootProxy[roots.size()]);
    }

    PXRRootProxy getRootByID(String id) {
        return roots.stream()
                .filter(r -> r.getAddress().rootID().equals(id))
                .findFirst()
                .orElse(null);

    }

    PXRRootProxy getRootByFile(FileObject file) {
        return roots.stream()
                .filter(r -> r.getSourceFile().equals(file))
                .findFirst()
                .orElse(null);
    }

    static PXRRootProxy findRootForFile(FileObject file) {
        var reg = registryForFile(file);
        if (reg != null) {
            return reg.getRootByFile(file);
        } else {
            return null;
        }
    }

    static PXRRootRegistry registryForFile(FileObject file) {
        var project = FileOwnerQuery.getOwner(file);
        if (project == null) {
            return null;
        }
        return project.getLookup().lookup(PXRRootRegistry.class);
    }

}
