/************************************************************************
 *
 *  W2XRegistration.java
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License version 2.1, as published by the Free Software Foundation.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this library; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston,
 *  MA  02111-1307  USA
 *
 *  Copyright: 2002-2022 by Henrik Just
 *
 *  All Rights Reserved.
 * 
 *  Version 1.7 (2022-06-17) 
 *
 */ 
 
package org.openoffice.da.comp.writer2xhtml;

import com.sun.star.lang.XMultiServiceFactory;
import com.sun.star.lang.XSingleServiceFactory;
import com.sun.star.registry.XRegistryKey;

import com.sun.star.comp.loader.FactoryHelper;

/** This class provides a static method to instantiate our uno components
 * on demand (__getServiceFactory()), and a static method to give
 * information about the components (__writeRegistryServiceInfo()).
 * Furthermore, it saves the XMultiServiceFactory provided to the
 * __getServiceFactory method for future reference by the componentes.
 */
public class W2XRegistration {
    
    public static XMultiServiceFactory xMultiServiceFactory;

    /**
     * Returns a factory for creating the service.
     * This method is called by the <code>JavaLoader</code>
     *
     * @return  returns a <code>XSingleServiceFactory</code> for creating the
     *          component
     *
     * @param   implName     the name of the implementation for which a
     *                       service is desired
     * @param   multiFactory the service manager to be used if needed
     * @param   regKey       the registryKey
     *
     * @see                  com.sun.star.comp.loader.JavaLoader
     */
    public static XSingleServiceFactory __getServiceFactory(String implName,
        XMultiServiceFactory multiFactory, XRegistryKey regKey) {
        xMultiServiceFactory = multiFactory;
        XSingleServiceFactory xSingleServiceFactory = null;
        if (implName.equals(Writer2xhtml.__implementationName) ) {
            xSingleServiceFactory = FactoryHelper.getServiceFactory(Writer2xhtml.class,
            Writer2xhtml.__serviceName,
            multiFactory,						    
            regKey);
        }
        else if (implName.equals(W2XExportFilter.class.getName()) ) {
            xSingleServiceFactory = FactoryHelper.getServiceFactory(W2XExportFilter.class,
            W2XExportFilter.__serviceName,
            multiFactory,						    
            regKey);
        }
        else if (implName.equals(XhtmlOptionsDialog.__implementationName)) {
            xSingleServiceFactory = FactoryHelper.getServiceFactory(XhtmlOptionsDialog.class,
            XhtmlOptionsDialog.__serviceName,
            multiFactory,						    
            regKey);
        }
        else if (implName.equals(XhtmlOptionsDialogMath.__implementationName)) {
            xSingleServiceFactory = FactoryHelper.getServiceFactory(XhtmlOptionsDialogMath.class,
            XhtmlOptionsDialogMath.__serviceName,
            multiFactory,						    
            regKey);
        }
        else if (implName.equals(HTML5OptionsDialog.__implementationName)) {
            xSingleServiceFactory = FactoryHelper.getServiceFactory(HTML5OptionsDialog.class,
            HTML5OptionsDialog.__serviceName,
            multiFactory,						    
            regKey);
        }
        else if (implName.equals(XhtmlOptionsDialogCalc.__implementationName)) {
            xSingleServiceFactory = FactoryHelper.getServiceFactory(XhtmlOptionsDialogCalc.class,
            XhtmlOptionsDialogCalc.__serviceName,
            multiFactory,						    
            regKey);
        }
        else if (implName.equals(HTML5OptionsDialogCalc.__implementationName)) {
            xSingleServiceFactory = FactoryHelper.getServiceFactory(HTML5OptionsDialogCalc.class,
            HTML5OptionsDialogCalc.__serviceName,
            multiFactory,						    
            regKey);
        }
        else if (implName.equals(EpubOptionsDialog.__implementationName)) {
            xSingleServiceFactory = FactoryHelper.getServiceFactory(EpubOptionsDialog.class,
            EpubOptionsDialog.__serviceName,
            multiFactory,						    
            regKey);
        }
        else if (implName.equals(Epub3OptionsDialog.__implementationName)) {
            xSingleServiceFactory = FactoryHelper.getServiceFactory(Epub3OptionsDialog.class,
            Epub3OptionsDialog.__serviceName,
            multiFactory,						    
            regKey);
        }
        else if (implName.equals(EpubMetadataDialog.__implementationName)) {
            xSingleServiceFactory = FactoryHelper.getServiceFactory(EpubMetadataDialog.class,
            EpubMetadataDialog.__serviceName,
            multiFactory,						    
            regKey);
        }
        else if (implName.equals(ConfigurationDialog.__implementationName)) {
            xSingleServiceFactory = FactoryHelper.getServiceFactory(ConfigurationDialog.class,
            ConfigurationDialog.__serviceName,
            multiFactory,						    
            regKey);
        }
        else if (implName.equals(ToolbarSettingsDialog.__implementationName)) {
            xSingleServiceFactory = FactoryHelper.getServiceFactory(ToolbarSettingsDialog.class,
            ToolbarSettingsDialog.__serviceName,
            multiFactory,						    
            regKey);
        }
        
        return xSingleServiceFactory;
    }
    
    /**
     * Writes the service information into the given registry key.
     * This method is called by the <code>JavaLoader</code>
     * <p>
     * @return  returns true if the operation succeeded
     * @param   regKey       the registryKey
     * @see                  com.sun.star.comp.loader.JavaLoader
     */
    public static boolean __writeRegistryServiceInfo(XRegistryKey regKey) {
        return
            FactoryHelper.writeRegistryServiceInfo(Writer2xhtml.__implementationName, 
                Writer2xhtml.__serviceName, regKey) &
            FactoryHelper.writeRegistryServiceInfo(W2XExportFilter.__implementationName,
                W2XExportFilter.__serviceName, regKey) &
            FactoryHelper.writeRegistryServiceInfo(XhtmlOptionsDialog.__implementationName,
                XhtmlOptionsDialog.__serviceName, regKey) &
            FactoryHelper.writeRegistryServiceInfo(XhtmlOptionsDialogMath.__implementationName,
                XhtmlOptionsDialogMath.__serviceName, regKey) &
            FactoryHelper.writeRegistryServiceInfo(HTML5OptionsDialog.__implementationName,
                    HTML5OptionsDialog.__serviceName, regKey) &
            FactoryHelper.writeRegistryServiceInfo(XhtmlOptionsDialogCalc.__implementationName,
                XhtmlOptionsDialogCalc.__serviceName, regKey) & 
            FactoryHelper.writeRegistryServiceInfo(HTML5OptionsDialogCalc.__implementationName,
            		HTML5OptionsDialogCalc.__serviceName, regKey) & 
            FactoryHelper.writeRegistryServiceInfo(EpubOptionsDialog.__implementationName,
                EpubOptionsDialog.__serviceName, regKey) &
            FactoryHelper.writeRegistryServiceInfo(Epub3OptionsDialog.__implementationName,
                Epub3OptionsDialog.__serviceName, regKey) &
            FactoryHelper.writeRegistryServiceInfo(EpubMetadataDialog.__implementationName,
                EpubMetadataDialog.__serviceName, regKey) &
            FactoryHelper.writeRegistryServiceInfo(ConfigurationDialog.__implementationName,
                ConfigurationDialog.__serviceName, regKey) &
            FactoryHelper.writeRegistryServiceInfo(ToolbarSettingsDialog.__implementationName,
                ToolbarSettingsDialog.__serviceName, regKey);
    }
}

