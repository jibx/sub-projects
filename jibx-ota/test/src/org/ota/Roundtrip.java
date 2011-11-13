/*
 * Copyright (c) 2010, Dennis M. Sosnoski. All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the
 * following conditions are met:
 * 
 * Redistributions of source code must retain the above copyright notice, this list of conditions and the following
 * disclaimer. Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the
 * following disclaimer in the documentation and/or other materials provided with the distribution. Neither the name of
 * JiBX nor the names of its contributors may be used to endorse or promote products derived from this software without
 * specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES,
 * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.ota;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;

import org.jibx.extras.DocumentComparator;
import org.jibx.runtime.BindingDirectory;
import org.jibx.runtime.IBindingFactory;
import org.jibx.runtime.IMarshallable;
import org.jibx.runtime.IMarshallingContext;
import org.jibx.runtime.IUnmarshallingContext;
import org.jibx.runtime.JiBXException;
import org.jibx.runtime.impl.UnmarshallingContext;

/**
 * Test class to verify the accuracy of the data model generated from the schemas. Each instance document matching the
 * supplied pattern is first unmarshalled to an object representation then marshalled back out to a new document, which
 * is then compared to the original document.
 */
public class Roundtrip
{
    /**
     * Roundtrip all document files in a directory. Only those files with names ending in ".xml" are roundtripped.
     * 
     * @param root directory
     * @param bfact binding factory
     * @param counts result counts
     */
    private static void processDirectory(File root, IBindingFactory bfact, Counts counts) {
        
        // process all the files in this directory
        File[] files = root.listFiles();
        for (int i = 0; i < files.length; i++) {
            File file = files[i];
            if (file.isDirectory()) {
                processDirectory(file, bfact, counts);
            } else if (file.getName().toLowerCase().endsWith(".xml")) {
                try {
                    
                    // unmarshal document into object
                    IUnmarshallingContext uctx = bfact.createUnmarshallingContext();
                    Object obj = uctx.unmarshalDocument(new FileInputStream(file), null);
                    String encoding = ((UnmarshallingContext)uctx).getInputEncoding();
                    
                    // marshal object back out to document in memory
                    IMarshallingContext mctx = bfact.createMarshallingContext();
                    mctx.setIndent(2);
                    ByteArrayOutputStream bos = new ByteArrayOutputStream();
                    mctx.setOutput(bos, "UTF-8");
                    ((IMarshallable)obj).marshal(mctx);
                    mctx.endDocument();
                    
                    // compare with original input document
                    InputStreamReader brdr = new InputStreamReader
                        (new ByteArrayInputStream(bos.toByteArray()), "UTF-8");
                    InputStreamReader frdr = new InputStreamReader(new FileInputStream(file), encoding);
                    DocumentComparator comp = new DocumentComparator(System.err, true);
                    if (comp.compare(frdr, brdr)) {
                        System.out.println("Successfully round-tripped file '" + file.getPath() + '\'');
                        counts.m_success++;
                    } else {
                        
                        // save output for comparison failure
                        counts.m_fail++;
                        try {
                            FileOutputStream fos = new FileOutputStream(file.getName());
                            fos.write(bos.toByteArray());
                            fos.close();
                        } catch (IOException e) {
                            System.err.println("Error writing to file '" + file.getName() + "': " + e.getMessage());
                        }
                    }
                    
                } catch (Exception e) {
                    counts.m_fail++;
                    System.err.println("Error on file '" + file.getPath() + '\'');
                    e.printStackTrace();
                }
            }
        }
    }
    
    /**
     * Scans a directory structure of sample documents, unmarshalling each sample document and then marshalling it back
     * out and comparing the result with the original input document. The first command line argument is the package for
     * the binding, the second argument is the sample documents directory.
     * 
     * @param args 
     */
    public static void main(String[] args) {
        if (args.length != 3) {
            System.out.println("Usage: java -cp ... " + "org.ota.Roundtrip binding package path");
            System.exit(1);
        }
        
        // look up the binding factory
        IBindingFactory bfact = null;
        try {
            bfact = BindingDirectory.getFactory(args[0], args[1]);
        } catch (JiBXException e) {
            System.err.println("Binding '" + args[0] + "' not found in package '" + args[1] + '\'');
            System.exit(2);
        }
        
        // setup the root document directory path
        File root = new File(args[2]);
        if (!root.isDirectory()) {
            System.err.println("Root directory '" + args[2] + "' not found");
            System.exit(3);
        }
        
        // process all the files
        Counts counts = new Counts();
        processDirectory(root, bfact, counts);
        System.out.println("Success count: " + counts.m_success);
        System.out.println("Failure count: " + counts.m_fail);
        if (counts.m_fail > 0) {
            System.exit(4);
        }
    }
    
    private static class Counts
    {
        public int m_success;
        public int m_fail;
    }
}