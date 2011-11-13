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

package org.ota.ws;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.jibx.runtime.BindingDirectory;
import org.jibx.runtime.IBindingFactory;
import org.jibx.runtime.IMarshallable;
import org.jibx.runtime.IMarshallingContext;
import org.jibx.runtime.IUnmarshallingContext;
import org.jibx.runtime.JiBXException;
import org.jibx.ws.client.Client;
import org.jibx.ws.io.MessageOptions;
import org.jibx.ws.protocol.Protocol;
import org.jibx.ws.protocol.ProtocolDirectory;
import org.jibx.ws.soap.client.SoapClient;

/**
 * Simple test client for OTA Air web service. This reads documents specified as command line parameters, sending each
 * document to the service and printing the document (if any) received in response.
 */
public class AirServiceClient
{
    /** Name of properties file read from classpath. */
    private static final String PROPERTIES_FILE_NAME = "air-client.properties";
    
    /** Property key for binding name. */
    private static final String BINDING_NAME_KEY = "org.ota.ws.AirServiceClient.binding";
    
    /** Property key for binding package. */
    private static final String PACKAGE_NAME_KEY = "org.ota.ws.AirServiceClient.package";
    
    /** Property key for protocol used with server. */
    private static final String PROTOCOL_KEY = "org.ota.ws.AirServiceClient.protocol";
    
    /** Property key for service endpoint address. */
    private static final String ENDPOINT_KEY = "org.ota.ws.AirServiceClient.endpoint";
    
    /** Property key for request media type (sent to server). */
    private static final String REQUEST_TYPE_KEY = "org.ota.ws.AirServiceClient.request";
    
    /** Property key for response media type(s) (accepted from server). */
    private static final String RESPONSE_TYPE_KEY = "org.ota.ws.AirServiceClient.response";
    
    /**
     * Get a required property value.
     * 
     * @param key
     * @param props
     * @return property value (non-<code>null</code>)
     */
    private static String getRequiredProperty(String key, Properties props) {
        String value = props.getProperty(key);
        if (value == null) {
            System.err.println("Missing required '" + key + "' property value");
            System.exit(1);
        }
        return value;
    }
    
    /**
     * Split comma-delimited string into array of strings.
     * 
     * @param text
     * @return array
     */
    private static String[] splitString(String text) {
        List<String> strings = new ArrayList<String>();
        int base = 0;
        int split;
        while ((split = text.indexOf(',', base)) >= 0) {
            strings.add(text.substring(base, split));
            base = split + 1;
        }
        strings.add(text.substring(base));
        return strings.toArray(new String[strings.size()]);
    }
    
    /**
     * Test client. This uses an air-client.properties file for configuration information, including the binding name
     * and package and service endpoint. It takes command line parameters giving the paths to request files which are
     * sent to the service, and prints the response received for each request.
     * 
     * @param args
     */
    public static void main(String[] args) throws Exception {
        
        // get the properties
        Properties props = new Properties(System.getProperties());
        InputStream is = AirServiceClient.class.getClassLoader().getResourceAsStream(PROPERTIES_FILE_NAME);
        if (is != null) {
            props.load(is);
        }
        
        // look up the binding factory
        String bname = getRequiredProperty(BINDING_NAME_KEY, props);
        String pname = getRequiredProperty(PACKAGE_NAME_KEY, props);
        IBindingFactory factory = null;
        try {
            factory = BindingDirectory.getFactory(bname, pname);
        } catch (JiBXException e) {
            System.err.println("Binding '" + bname + "' not found in package '" + pname + '\'');
            System.exit(2);
        }
        
        // create the appropriate type of client
        Protocol protocol = ProtocolDirectory.getProtocol(getRequiredProperty(PROTOCOL_KEY, props));
        MessageOptions options = new MessageOptions();
        options.setOutMediaTypeCode(getRequiredProperty(REQUEST_TYPE_KEY, props));
        options.setInMediaTypeCodes(splitString(getRequiredProperty(RESPONSE_TYPE_KEY, props)));
        Client client = protocol.createClient(getRequiredProperty(ENDPOINT_KEY, props), factory, options);
        // to just create a SOAP client using only XML media type, replace the above with the following
//        Client client = new SoapClient(getRequiredProperty(ENDPOINT_KEY, props), factory);
        
        // process each command line parameter as document path
        for (int i = 0; i < args.length; i++) {
            String path = args[i];
            try {
                
                // read input document and send to server
                IUnmarshallingContext uctx = factory.createUnmarshallingContext();
                Object req = uctx.unmarshalDocument(new FileInputStream(path), null);
                Object rsp = client.call(req);
                
                // print response information
                System.out.print("Sent file '" + path + '\'');
                if (rsp == null) {
                    System.out.println(" with response 'null'");
                } else {
                    
                    // set up to marshal out copy of response document
                    IMarshallingContext mctx = factory.createMarshallingContext();
                    System.out.println(" with response:");
                    mctx.setIndent(2);
                    
                    // print document without closing the output stream (using basic methods, not marshalDocument)
                    mctx.setOutput(System.out, "UTF-8");
                    ((IMarshallable)rsp).marshal(mctx);
                    mctx.getXmlWriter().flush();
                    System.out.println();
                    System.out.println();
                    
                }
            } catch (FileNotFoundException e) {
                System.err.println("File '" + path + "' not found");
                e.printStackTrace();
            } catch (JiBXException e) {
                System.err.println("Error reading file '" + path + "'");
                e.printStackTrace();
            } catch (IOException e) {
                System.err.println("Error writing output");
                e.printStackTrace();
            }
        }
    }
}