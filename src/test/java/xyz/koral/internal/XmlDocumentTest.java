package xyz.koral.internal;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.w3c.dom.Element;

import xyz.koral.internal.XmlDocument;

public class XmlDocumentTest  extends TestCase
{
    public XmlDocumentTest(String testName)
    {
        super(testName);
    }

    public static Test suite()
    {
        return new TestSuite(XmlDocumentTest.class);
    }
    
    final String docHeader = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
			"<koral id=\"test.sub\" version=\"0.2\" xmlns=\"http://koral.io/schema\">\n";
    final String docEnd = "\n</koral>";
    
    public void test0() throws Exception
    {
    	String doc = "\t<array id=\"timestamp\" type=\"numeric\" count=\"2\">123|1234</array>\n"
    			+ "\t<array id=\"stuff\" type=\"string\" count=\"3\">1121212s|1|234</array>";
    	InputStream is = new ByteArrayInputStream((docHeader + doc + docEnd).getBytes(XmlDocument.cs));
    	XmlDocument.skipChildren(is, "array");
    }
    
    public void test1() throws Exception
    {
    	String start = "\t<array id=\"timestamp\" type=\"numeric\" count=\"2\">";
    	String text	= "123|1234";
    	
    	
    	String end = "</array>";
    	long offset = size(docHeader + start);
    	long bytes = size(text);
    	
    	InputStream is = new ByteArrayInputStream((docHeader + start + text + end + docEnd).getBytes(XmlDocument.cs));
    	XmlDocument doc = XmlDocument.skipChildren(is, "array");
    	
    	Element e = doc.xpath("//array").get(0);
    	assertEquals(offset, (long) new Long(e.getAttribute(XmlDocument.sourceOffsetAtt)));
    	assertEquals(bytes, (long) new Long(e.getAttribute(XmlDocument.noOfBytesAtt)));
    	ByteArrayOutputStream bos = new ByteArrayOutputStream();
    	doc.save(bos);
    	System.out.println(new String(bos.toByteArray(), XmlDocument.cs));
    }
    
    static int size(String t)
    {
    	return t.getBytes(XmlDocument.cs).length;
    }
}
