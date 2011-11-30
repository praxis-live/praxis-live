/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package net.neilcsmith.praxis.live.pxr;

import org.netbeans.spi.project.LookupProvider;
import org.netbeans.spi.project.ui.PrivilegedTemplates;
import org.openide.util.Lookup;
import org.openide.util.lookup.Lookups;

/**
 *
 * @author Neil C Smith (http://neilcsmith.net)
 */
@LookupProvider.Registration(projectType="net-neilcsmith-praxis-live-project")
public class PXRLookupProvider implements LookupProvider {

    private final static PrivilegedTemplates templates = new PrivTemplatesImpl();

    @Override
    public Lookup createAdditionalLookup(Lookup baseContext) {
        return Lookups.singleton(templates);
    }

    private static class PrivTemplatesImpl implements PrivilegedTemplates {

        @Override
        public String[] getPrivilegedTemplates() {
            return new String[]{
                "Templates/Praxis/AudioTemplate.pxr",
                "Templates/Praxis/VideoTemplate.pxr"
            };
        }
    }
}
