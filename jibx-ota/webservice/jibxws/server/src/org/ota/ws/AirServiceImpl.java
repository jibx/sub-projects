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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.jibx.runtime.BindingDirectory;
import org.jibx.runtime.IBindingFactory;
import org.jibx.runtime.IMarshallable;
import org.jibx.runtime.IMarshallingContext;
import org.jibx.runtime.IUnmarshallingContext;
import org.jibx.runtime.JiBXException;
import org.ota.air.AvailRQ;
import org.ota.air.AvailRS;
import org.ota.air.BookModifyRQ;
import org.ota.air.BookRQ;
import org.ota.air.BookRS;
import org.ota.air.CheckInRQ;
import org.ota.air.CheckInRS;
import org.ota.air.DemandTicketRQ;
import org.ota.air.DemandTicketRS;
import org.ota.air.DetailsRQ;
import org.ota.air.DetailsRS;
import org.ota.air.FareDisplayRQ;
import org.ota.air.FareDisplayRS;
import org.ota.air.FlifoRQ;
import org.ota.air.FlifoRS;
import org.ota.air.LowFareSearchRQ;
import org.ota.air.LowFareSearchRS;
import org.ota.air.PriceRQ;
import org.ota.air.PriceRS;
import org.ota.air.RulesRQ;
import org.ota.air.RulesRS;
import org.ota.air.ScheduleRQ;
import org.ota.air.ScheduleRS;
import org.ota.air.SeatMapRQ;
import org.ota.air.SeatMapRS;

/**
 * Simple test implementation of OTA Air web service. This reads documents matching a path and file name pattern
 * (specified by a property, either set directly as a JVM system property or from a 'air-service.properties' properties
 * file on the classpath) at startup, then responds to each method call with a randomly-selected document of the
 * appropriate type.
 */
public class AirServiceImpl
{
    /** Logger for class. */
    private static final Logger s_logger = Logger.getLogger(AirServiceImpl.class.getName());
    
    /** Name of properties file read from classpath. */
    private static final String PROPERTIES_FILE_NAME = "air-service.properties";
    
    /** Property key for binding name. */
    private static final String BINDING_NAME_KEY = "org.ota.ws.AirServiceImpl.binding";
    
    /** Property key for binding package. */
    private static final String PACKAGE_NAME_KEY = "org.ota.ws.AirServiceImpl.package";
    
    /** Property key for path to response documents. */
    private static final String DOCUMENTS_PATH_KEY = "org.ota.ws.AirServiceImpl.path";
    
    /** Property key for response document file name pattern. */
    private static final String NAME_PATTERN_KEY = "org.ota.ws.AirServiceImpl.pattern";
    
    /** Binding factory for OTA documents. */
    private static final IBindingFactory m_factory;
    
    /** Documents to be returned. */
    private static final Map<String,DocumentList> s_classDocuments = new HashMap<String,DocumentList>();
    static {
        
        // get the properties
        Properties props = new Properties(System.getProperties());
        InputStream is = AirServiceImpl.class.getClassLoader().getResourceAsStream(PROPERTIES_FILE_NAME);
        if (is != null) {
            try {
                props.load(is);
            } catch (IOException e) {
                s_logger.error("Error loading properties from " + PROPERTIES_FILE_NAME, e);
                throw new ExceptionInInitializerError(e);
            }
        }
        
        // look up the binding factory
        String bname = (String)props.get(BINDING_NAME_KEY);
        String pname = (String)props.get(PACKAGE_NAME_KEY);
        if (bname == null || pname == null) {
            throw new ExceptionInInitializerError("Binding name and/or package name not set in properties");
        }
        try {
            m_factory = BindingDirectory.getFactory(bname, pname, AirServiceImpl.class.getClassLoader());
        } catch (JiBXException e) {
            String msg = "Binding '" + bname + "' not found in package '" + pname + '\'';
            s_logger.error(msg);
            throw new ExceptionInInitializerError(msg);
        }
        
        // process path for documents to be loaded
        //  simplified version of <code>org.jibx.utils.ResourceMatcher.matchPaths</code>
        String path = (String)props.get(DOCUMENTS_PATH_KEY);
        String pattern = (String)props.get(NAME_PATTERN_KEY);
        if (path == null || pattern == null) {
            throw new ExceptionInInitializerError("Document path or name pattern not set in properties");
        } else {
            PatternMatcher filter = new PatternMatcher(pattern);
            s_logger.debug("Matching file names to pattern '" + path + '\'');
            File dir = new File(path);
            String[] matches = dir.list(filter);
            if (matches == null || matches.length == 0) {
                String msg = "No files found matching command line pattern '" + pattern +
                    "' in directory " + dir.getAbsolutePath();
                s_logger.error(msg);
                throw new ExceptionInInitializerError(msg);
            } else {
                
                // read all documents matching path pattern
                for (int i = 0; i < matches.length; i++) {
                    File file = new File(dir, matches[i]);
                    try {
                        IUnmarshallingContext uctx = m_factory.createUnmarshallingContext();
                        Object obj = uctx.unmarshalDocument(new FileInputStream(file), null);
                        String cname = obj.getClass().getName();
                        DocumentList list = s_classDocuments.get(cname);
                        if (list == null) {
                            list = new DocumentList();
                            s_classDocuments.put(cname, list);
                        }
                        list.add(obj);
                        s_logger.info("Unmarshalled '" + file.getPath() + "' to instance of class " + cname);
                    } catch (FileNotFoundException e) {
                        s_logger.error("Error reading file '" + file.getPath(), e);
                    } catch (JiBXException e) {
                        s_logger.error("Error reading file '" + file.getPath(), e);
                    }
                }
                
            }
        }
    }
    
    /**
     * Get the next document to be returned of a particular type.
     * 
     * @param type
     * @return unmarshalled document object, or <code>null</code> if no document available
     */
    private static Object getDocument(Class type) {
        DocumentList list = s_classDocuments.get(type.getName());
        if (list == null) {
            return null;
        } else {
            return list.next();
        }
    }
    
    //
    // Service methods - these just provide a minimal implementation of the interface
    
    public AvailRS avail(AvailRQ req) {
        return (AvailRS)getDocument(AvailRS.class);
    }
    
    public BookRS book(BookRQ req) {
        return (BookRS)getDocument(BookRS.class);
    }
    
    public BookRS bookModify(BookModifyRQ req) {
        return (BookRS)getDocument(BookRS.class);
    }
    
    public CheckInRS checkin(CheckInRQ req) {
        return (CheckInRS)getDocument(CheckInRS.class);
    }
    
    public DemandTicketRS demandTicket(DemandTicketRQ req) {
        return (DemandTicketRS)getDocument(DemandTicketRS.class);
    }
    
    public DetailsRS details(DetailsRQ req) {
        return (DetailsRS)getDocument(DetailsRS.class);
    }
    
    public FareDisplayRS fareDisplay(FareDisplayRQ req) {
        return (FareDisplayRS)getDocument(FareDisplayRS.class);
    }
    
    public FlifoRS flifo(FlifoRQ req) {
        return (FlifoRS)getDocument(FlifoRS.class);
    }
    
    public LowFareSearchRS lowFareSearch(LowFareSearchRQ req) {
        return (LowFareSearchRS)getDocument(LowFareSearchRS.class);
    }
    
    public PriceRS price(PriceRQ req) {
        return (PriceRS)getDocument(PriceRS.class);
    }
    
    public RulesRS rules(RulesRQ req) {
        return (RulesRS)getDocument(RulesRS.class);
    }
    
    public ScheduleRS schedule(ScheduleRQ req) {
        return (ScheduleRS)getDocument(ScheduleRS.class);
    }
    
    public SeatMapRS seatMap(SeatMapRQ req) {
        return (SeatMapRS)getDocument(SeatMapRS.class);
    }

    /**
     * File name pattern matcher. This is just a copy of <code>org.jibx.utils.ResourceMatcher.PatternMatcher</code> with
     * the matching code imported.
     */
    private static class PatternMatcher implements FilenameFilter
    {
        /** Current match pattern. */
        private String m_pattern;
        
        /**
         * Constructor.
         * 
         * @param pattern
         */
        public PatternMatcher(String pattern) {
            m_pattern = pattern;
        }

        /**
         * Checks if a name matches a pattern. This method accepts one or more '*' wildcard characters in the pattern,
         * calling itself recursively in order to handle multiple wildcards.
         * 
         * @param name
         * @param pattern match pattern
         * @return <code>true</code> if pattern matched, <code>false</code> if not
         */
        private static boolean isPatternMatch(String name, String pattern) {
            
            // check special match cases first
            if (pattern.length() == 0) {
                return name.length() == 0;
            } else if (pattern.charAt(0) == '*') {
                
                // check if the wildcard is all that's left of pattern
                if (pattern.length() == 1) {
                    return true;
                } else {
                    
                    // check if another wildcard follows next segment of text
                    pattern = pattern.substring(1);
                    int split = pattern.indexOf('*');
                    if (split > 0) {
                        
                        // recurse on each match to text segment
                        String piece = pattern.substring(0, split);
                        pattern = pattern.substring(split);
                        int offset = -1;
                        while ((offset = name.indexOf(piece, ++offset)) > 0) {
                            int end = offset + piece.length();
                            if (isPatternMatch(name.substring(end), pattern)) {
                                return true;
                            }
                        }
                        
                    } else {
                        
                        // no more wildcards, need exact match to end of name
                        return name.endsWith(pattern);
                        
                    }
                }
            } else {
                
                // check for leading text before first wildcard
                int split = pattern.indexOf('*');
                if (split > 0) {
                    
                    // match leading text to start of name
                    String piece = pattern.substring(0, split);
                    if (name.startsWith(piece)) {
                        return isPatternMatch(name.substring(split), pattern.substring(split));
                    } else {
                        return false;
                    }
                    
                } else {
                    
                    // no wildcards, need exact match
                    return name.equals(pattern);
                    
                }
            }
            return false;
        }
        
        /**
         * Check for file name match.
         * 
         * @param dir
         * @param name
         * @return match flag
         */
        public boolean accept(File dir, String name) {
            boolean match = isPatternMatch(name, m_pattern);
            if (match) {
                s_logger.debug(" matched file name '" + name + '\'');
            }
            return match;
        }
    }
    
    /**
     * Document list with circular iterator.
     */
    private static class DocumentList extends ArrayList
    {
        private int m_index;
        
        /**
         * Circular iterator over documents in list. This is synchronized to allow multithreaded use.
         * 
         * @return document
         */
        public synchronized Object next() {
            if (m_index >= size()) {
                m_index = 0;
            }
            return get(m_index++);
        }
    }
    
    /**
     * Standalone test method. This just tests the service implementation code using direct calls to the service
     * methods.
     * 
     * @param args
     */
    public static void main(String[] args) {
        AirServiceImpl inst = new AirServiceImpl();
        for (int i = 0; i < args.length; i++) {
            String path = args[i];
            try {
                
                // read input document and send to server
                IUnmarshallingContext uctx = m_factory.createUnmarshallingContext();
                Object req = uctx.unmarshalDocument(new FileInputStream(path), null);
                Object rsp = null;
                if (req instanceof AvailRQ) {
                    rsp = inst.avail((AvailRQ)req);
                } else if (req instanceof BookRQ) {
                    rsp = inst.book((BookRQ)req);
                } else if (req instanceof BookModifyRQ) {
                    rsp = inst.bookModify((BookModifyRQ)req);
                } else if (req instanceof CheckInRQ) {
                    rsp = inst.checkin((CheckInRQ)req);
                } else if (req instanceof DemandTicketRQ) {
                    rsp = inst.demandTicket((DemandTicketRQ)req);
                } else if (req instanceof DetailsRQ) {
                    rsp = inst.details((DetailsRQ)req);
                } else if (req instanceof FareDisplayRQ) {
                    rsp = inst.fareDisplay((FareDisplayRQ)req);
                } else if (req instanceof FlifoRQ) {
                    rsp = inst.flifo((FlifoRQ)req);
                } else if (req instanceof LowFareSearchRQ) {
                    rsp = inst.lowFareSearch((LowFareSearchRQ)req);
                } else if (req instanceof PriceRQ) {
                    rsp = inst.price((PriceRQ)req);
                } else if (req instanceof RulesRQ) {
                    rsp = inst.rules((RulesRQ)req);
                } else if (req instanceof ScheduleRQ) {
                    rsp = inst.schedule((ScheduleRQ)req);
                } else if (req instanceof SeatMapRQ) {
                    rsp = inst.seatMap((SeatMapRQ)req);
                }
                
                // print response information
                System.out.print("Sent file '" + path + '\'');
                if (rsp == null) {
                    System.out.println(" with response 'null'");
                } else {
                    
                    // set up to marshal out copy of response document
                    IMarshallingContext mctx = m_factory.createMarshallingContext();
                    System.out.println(" with response:");
                    mctx.setIndent(2);
                    
                    // print document without closing the output stream (using basic methods, not marshalDocument)
                    mctx.setOutput(System.out, Charset.defaultCharset().displayName());
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