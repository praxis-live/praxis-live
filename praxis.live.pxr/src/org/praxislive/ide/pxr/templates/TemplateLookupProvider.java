/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.praxislive.ide.pxr.templates;

import org.netbeans.spi.project.LookupProvider;
import org.openide.util.Lookup;
import org.openide.util.lookup.Lookups;

/**
 *
 * @author Neil C Smith
 */
@LookupProvider.Registration(projectType="org-praxislive-ide-project")
public class TemplateLookupProvider implements LookupProvider {

    @Override
    public Lookup createAdditionalLookup(Lookup baseContext) {
        return Lookups.singleton(TemplateFiles.getDefault().getPrivilegedTemplates());
    }
    
    
    
}
