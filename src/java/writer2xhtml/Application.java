/************************************************************************
 *
 *  Application.java
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
 *  Version 1.7 (2022-06-14) 
 *
 */
 
package writer2xhtml;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
//import java.io.FileOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Set;
import java.util.Vector;

import writer2xhtml.api.Converter;
import writer2xhtml.api.ConverterFactory;
import writer2xhtml.api.ConverterResult;
import writer2xhtml.api.MIMETypes;
import writer2xhtml.util.Misc;

/**
 * <p>Command line utility to convert an OpenOffice.org Writer XML file into XHTML/EPUB</p>
 * <p>The utility is invoked with the following command line:</p>
 * <pre>java -jar writer2xhtml.jar [options] source [target]</pre>
 * <p>Where the available options are
 * <ul>
 * <li><code>-html5</code>, <code>-xhtml</code>,
       <code>-xhtml+mathml</code>, <code>-epub</code>, <code>-epub3</code>
 * <li><code>-config[=]filename</code>
 * <li><code>-template[=]filename</code>
 * <li><code>-stylesheet[=]filename</code>
 * <li><code>-resource[=]filename[::media type]</code>
 * <li><code>-option[=]value</code>
 * </ul>
 * <p>where <code>option</code> can be any simple option known to Writer2xhtml
 * (see documentation for the configuration file).</p>
 */
public final class Application {
	
    /* Based on command-line parameters. */
    private String sTargetMIME = MIMETypes.HTML5;
    private Vector<String> configFileNames = new Vector<String>();
    private String sTemplateFileName = null;
    private String sStyleSheetFileName = null;
    private Set<String> resources = new HashSet<String>(); 
    private Hashtable<String,String> options = new Hashtable<String,String>();
    private String sSource = null;
    private String sTarget = null;

    /**
     *  Main method
     *
     *  @param  args  The argument passed on the command line.
     */
    public static final void main (String[] args){
        try {
        	//long time = System.currentTimeMillis();
            Application app = new Application();
            app.parseCommandLine(args);
            app.doConversion();
            //System.out.println("Total conversion time was "+(System.currentTimeMillis()-time)+" miliseconds");
        } catch (IllegalArgumentException ex) {
            String msg = ex.getMessage();
            showUsage(msg);
        }
    }
	
    // Convert file
    private void doConversion() {
        // Step 1: Say hello...
        System.out.println();
        System.out.println("This is Writer2xhtml, Version " + ConverterFactory.getVersion() + 
                           " (" + ConverterFactory.getDate() + ")");
        System.out.println();
        System.out.println("Starting conversion...");
		
        // Step 2: Examine source
        File source = new File(sSource);
        if (!source.exists()) {
            System.out.println("I'm sorry, I can't find "+sSource);
            System.exit(1);
        }
        if (!source.canRead()) {
            System.out.println("I'm sorry, I can't read "+sSource);
            System.exit(1);
        }
        if (!source.isFile()) {
            System.out.println("I'm sorry, "+sSource+" is not a file");
            System.exit(1);
        }

        // Step 3: Examine target
        File target;
        if (sTarget==null) {
            target = new File(source.getParent(),Misc.removeExtension(source.getName()));
        }
        else {
            target = new File(sTarget);
            if (sTarget.endsWith(File.separator)) {
                target = new File(target,Misc.removeExtension(source.getName()));
            }
        }
		
        // Step 4: Create converter
        Converter converter = ConverterFactory.createConverter(sTargetMIME);
        if (converter==null) {
            System.out.println("Failed to create converter for "+sTargetMIME);
            System.exit(1);
        }
		
        // Step 5a: Read template
        if (sTemplateFileName!=null) {
            try {
                System.out.println("Reading template "+sTemplateFileName);
                byte [] templateBytes = Misc.inputStreamToByteArray(new FileInputStream(sTemplateFileName));
                converter.readTemplate(new ByteArrayInputStream(templateBytes));
            }
            catch (FileNotFoundException e) {
                System.out.println("--> This file does not exist!");
                System.out.println("    "+e.getMessage());
            }
            catch (IOException e) {
                System.out.println("--> Failed to read the template file!");
                System.out.println("    "+e.getMessage());
            }
        }
		
        // Step 5b: Read style sheet
        if (sStyleSheetFileName!=null) {
            try {
                System.out.println("Reading style sheet "+sStyleSheetFileName);
                byte [] styleSheetBytes = Misc.inputStreamToByteArray(new FileInputStream(sStyleSheetFileName));
                converter.readStyleSheet(new ByteArrayInputStream(styleSheetBytes));
            }
            catch (FileNotFoundException e) {
                System.out.println("--> This file does not exist!");
                System.out.println("    "+e.getMessage());
            }
            catch (IOException e) {
                System.out.println("--> Failed to read the style sheet file!");
                System.out.println("    "+e.getMessage());
            }
        }
        
        // Step 5c: Read style resources
        for (String sResource : resources) {
        	String sMediaType;
        	String sFileName;
        	int nSeparator = sResource.indexOf("::");
        	if (nSeparator>-1) {
        		sFileName = sResource.substring(0,nSeparator);
        		sMediaType = sResource.substring(nSeparator+2);
        	}
        	else {
        		sFileName = sResource;
        		sMediaType = null;
        	}
        	System.out.println("Reading resource file "+sFileName);
        	try {
        		byte [] resourceBytes = Misc.inputStreamToByteArray(new FileInputStream(sFileName));
        		converter.readResource(new ByteArrayInputStream(resourceBytes),sFileName,sMediaType);
        	} catch (IOException e) {
                System.out.println("--> Failed to read the resource file!");
                System.out.println("    "+e.getMessage());
        	}        		
        	
        }
		
        // Step 6: Read config
        for (int i=0; i<configFileNames.size(); i++) {
            String sConfigFileName = (String) configFileNames.get(i);
            if (sConfigFileName.startsWith("*")) {
                sConfigFileName = sConfigFileName.substring(1);
                System.out.println("Reading default configuration "+sConfigFileName);
                try {
                    converter.getConfig().readDefaultConfig(sConfigFileName);
                }
                catch (IllegalArgumentException e) {
                    System.err.println("--> This configuration is unknown!");
                    System.out.println("    "+e.getMessage());
                }
            }
            else {
                System.out.println("Reading configuration file "+sConfigFileName);
                try {
                    byte[] configBytes = Misc.inputStreamToByteArray(new FileInputStream(sConfigFileName));
                    converter.getConfig().read(new ByteArrayInputStream(configBytes));
                }
                catch (IOException e) {
                    System.err.println("--> Failed to read the configuration!");
                    System.out.println("    "+e.getMessage());
                }
            }
        }
		
        // Step 7: Set options from command line
        Enumeration<String> keys = options.keys();
        while (keys.hasMoreElements()) {
            String sKey = keys.nextElement();
            String sValue = (String) options.get(sKey);
            converter.getConfig().setOption(sKey,sValue);
        }
	 	
        // Step 8: Perform conversion
        System.out.println("Converting "+source.getPath());
        ConverterResult dataOut = null;

        try {
            dataOut = converter.convert(source,target.getName());
        }
        catch (FileNotFoundException e) {
            System.out.println("--> The file "+source.getPath()+" does not exist!");
            System.out.println("    "+e.getMessage());
            System.exit(1);
        }
        catch (IOException e) {
            System.out.println("--> Failed to convert the file "+source.getPath()+"!");
            System.out.println("    "+e.getMessage());
            System.out.println("    Please make sure the file is in OpenDocument format");
            System.exit(1);
        }

        // TODO: Should do some further checking on the feasability of writing
        // the directory and the files.
        File targetDir = target.getParentFile();
        if (targetDir!=null && !targetDir.exists()) { targetDir.mkdirs(); }
        try {
            dataOut.write(targetDir);
        }
        catch (IOException e) {
            System.out.println("--> Error writing out file!");
            System.out.println("    "+e.getMessage());
            System.exit(1);
        }
		
        // Step 9: Say goodbye!
        System.out.println("Done!");
    }


    /**
     *  Display usage.
     */
    private static void showUsage(String msg) {
        System.out.println();
        System.out.println("This is Writer2xhtml, Version " + ConverterFactory.getVersion() 
                           + " (" + ConverterFactory.getDate() + ")");
        System.out.println();
        if (msg != null) System.out.println(msg);
        System.out.println();
        System.out.println("Usage:");
        System.out.println("   java -jar <path>/writer2xhtml.jar <options> <source file/directory> [<target file/directory>]");
        System.out.println("where the available options are:");
        System.out.println("   -xhtml");
        System.out.println("   -xhtml11");
        System.out.println("   -xhtml+mathml");
        System.out.println("   -html5");
        System.out.println("   -epub");
        System.out.println("   -epub3");
        System.out.println("   -template[=]<template file>");
        System.out.println("   -stylesheet[=]<style sheet file>");
        System.out.println("   -resource[=]<resource file>[::<media type>]");
        System.out.println("   -cleanxhtml");
        System.out.println("   -config[=]<configuration file>");
        System.out.println("   -<configuration option>[=]<value>");
        System.out.println("See the documentation for the available configuration options");
    }

    /**
     *  Parse command-line arguments.
     *
     *  @param  args  Array of command line arguments.
     *
     *  @throws  IllegalArgumentException  If an argument is invalid.
     */
    private void parseCommandLine(String sArgs[])
        throws IllegalArgumentException {

        int i = 0;
		
        while (i<sArgs.length) {
            String sArg = getArg(i++,sArgs);
            if (sArg.startsWith("-")) { // found an option
                if ("-html5".equals(sArg)) { sTargetMIME = MIMETypes.HTML5; }
                else if ("-xhtml".equals(sArg)) { sTargetMIME = MIMETypes.XHTML; }
                else if ("-xhtml11".equals(sArg)) { sTargetMIME = MIMETypes.XHTML11; }
                else if ("-xhtml+mathml".equals(sArg)) { sTargetMIME = MIMETypes.XHTML_MATHML; }
                else if ("-epub".equals(sArg)) { sTargetMIME = MIMETypes.EPUB; }
                else if ("-epub3".equals(sArg)) { sTargetMIME = MIMETypes.EPUB3; }
                else if ("-cleanxhtml".equals(sArg)) { configFileNames.add("*cleanxhtml.xml"); }
                else { // option with argument
                    int j=sArg.indexOf("=");
                    String sArg2;
                    if (j>-1) { // argument is separated by =
                        sArg2 = sArg.substring(j+1);
                        sArg = sArg.substring(0,j);
                    }
                    else { // argument is separated by space
                        sArg2 = getArg(i++,sArgs);
                    }
                    if ("-config".equals(sArg)) { configFileNames.add(sArg2); }
                    else if ("-template".equals(sArg)) { sTemplateFileName = sArg2; }
                    else if ("-stylesheet".equals(sArg)) { sStyleSheetFileName = sArg2; }
                    else if ("-resource".equals(sArg)) { resources.add(sArg2); }
                    else { // configuration option
                        options.put(sArg.substring(1),sArg2);
                    }
                }
            }
            else { // not an option, so this must be the source
                sSource = sArg;
                // Possibly followed by the target
                if (i<sArgs.length) {
                    String sArgument = getArg(i++,sArgs); 
                    if (sArgument.length()>0) { sTarget = sArgument; }
                }
                // Skip any trailing empty arguments and signal an error if there's more
                while (i<sArgs.length) {
                    String sArgument = getArg(i++,sArgs);
                    if (sArgument.length()>0) {
                        throw new IllegalArgumentException("I didn't expect "+sArgument+"?");
                    }
                }
            }
        }
        if (sSource==null) {
            throw new IllegalArgumentException("Please specify a source document/directory!");
        }
        // Parsing of command line ended successfully!
    }


    /**
     *  Extract the next argument from the array, while checking to see
     *  that the array size is not exceeded.  Throw a friendly error
     *  message in case the arg is missing.
     *
     *  @param  i     Argument index.
     *  @param  args  Array of command line arguments.
     *
     *  @return  The argument with the specified index.
     *
     *  @throws  IllegalArgumentException  If an argument is invalid.
     */
    private String getArg(int i, String args[])
        throws IllegalArgumentException {

        if (i < args.length) {
            return args[i];
        }
        else throw new
            IllegalArgumentException("I'm sorry, the commandline ended abnormally");
    }
	

}
