// Adapted from the spring oxm test - Thanks!

package org.jibx.samples;

import java.io.ByteArrayOutputStream;
import java.io.Reader;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.jibx.runtime.BindingDirectory;
import org.jibx.runtime.IBindingFactory;
import org.jibx.runtime.IMarshallingContext;
import org.jibx.runtime.IUnmarshallingContext;
import org.jibx.runtime.JiBXException;
import org.joda.time.DateTime;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * Unit test for simple App.
 */
public class AppTest 
    extends TestCase
{
    public final static String STRING_ENCODING = "UTF8";
    public final static String URL_ENCODING = "UTF-8";

    /**
     * Create the test case
     *
     * @param testName name of the test case
     */
    public AppTest( String testName )
    {
        super( testName );
    }

    /**
     * @return the suite of tests being tested
     */
    public static Test suite()
    {
        return new TestSuite( AppTest.class );
    }

    /**
     * Test it!
     */
    public void testApp()
    {
        assertTrue( true );

        String system = "ping";
    	Object message = createMessage();
    	String xml = marshalObject(message, system);
	    System.out.println(xml);
	    
	    message = unmarshalMessage(xml, system);
	    this.checkMessage(message);
    }
    /**
     * Create a default ping message.
     * @return
     */
    public Object createMessage()
    {
    	Flights ping = new Flights();
    	FlightType type = new FlightType();
    	type.setNumber(FLIGHT_NO);
    	ping.addFlight(type);
        return ping;
    }
    public static final int FLIGHT_NO = 5;
    /**
     * Print the payload of this ping message.
     * @param message
     */
    public void checkMessage(Object message)
    {
    	Flights ping = (Flights)message;
    	FlightType flight = ping.getFlight(0);
        assertTrue( flight.getNumber() == FLIGHT_NO );
    }
    public static final String DATE_FORMAT = "yyyy-MM-dd";
    public static final SimpleDateFormat gDateFormat = new SimpleDateFormat(DATE_FORMAT);
    /**
     * Convert this date to the standard string date format.
     * which is yyyy-MM-dd.
     * @param date
     * @return
     */
    public String dateToStringDateFormat(Date date)
    {
        if (date == null)
            return null;
        return gDateFormat.format(date);
    }
    /**
     * Marshal this message to xml .
     * @param message
     * @param system
     * @return
     */
    public String marshalObject(Object message, String system)
    {
        String packageName = "org.jibx.samples";
        String bindingName = "binding";

		try {
			IBindingFactory jc = BindingDirectory.getFactory(bindingName, packageName);
			IMarshallingContext marshaller = jc.createMarshallingContext();
	        ByteArrayOutputStream out = new ByteArrayOutputStream();
			marshaller.marshalDocument(message, URL_ENCODING, null, out);
			String xml = out.toString(STRING_ENCODING);
			return xml;
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		} catch (JiBXException e) {
			e.printStackTrace();
		}
		return null;
    }
    /**
     * Unmarshal this xml Message to an object.
     * @param xml
     * @param system
     * @return
     */
    public Object unmarshalMessage(String xml, String system)
    {
        String packageName = "org.jibx.samples";
        String bindingName = "binding";
        
		try {
			IBindingFactory jc = BindingDirectory.getFactory(bindingName, packageName);
			IUnmarshallingContext unmarshaller = jc.createUnmarshallingContext();
	        Reader inStream = new StringReader(xml);
			Object message = unmarshaller.unmarshalDocument( inStream, bindingName);
			return message;
		} catch (JiBXException e) {
			e.printStackTrace();
		}
    	return null;
    }
}
