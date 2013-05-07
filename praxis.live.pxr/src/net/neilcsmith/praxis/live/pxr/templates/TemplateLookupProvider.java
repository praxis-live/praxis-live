/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.neilcsmith.praxis.live.pxr.templates;

import org.netbeans.spi.project.LookupProvider;
import org.openide.util.Lookup;
import org.openide.util.lookup.Lookups;

/**
 *
 * @author Neil C Smith
 */
@LookupProvider.Registration(projectType="net-neilcsmith-praxis-live-project")
public class TemplateLookupProvider implements LookupProvider {

    @Override
    public Lookup createAdditionalLookup(Lookup baseContext) {
        return Lookups.singleton(TemplateFiles.getDefault().getPrivilegedTemplates());
    }
    
    
    
}
