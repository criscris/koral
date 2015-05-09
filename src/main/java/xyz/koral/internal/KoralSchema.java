package xyz.koral.internal;

import java.io.IOException;
import java.io.InputStream;

import javax.xml.XMLConstants;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import org.xml.sax.SAXException;

import xyz.koral.Koral;

public class KoralSchema 
{
	private Schema schema;
	
	public static KoralSchema instance;
	
	public static synchronized KoralSchema getInstance()
	{
		if (instance == null)
		{
			instance = new KoralSchema();
		}
		return instance;
	}
	
	KoralSchema() 
	{
		SchemaFactory sFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
	    try 
	    {
			schema = sFactory.newSchema(new StreamSource(Koral.class.getResourceAsStream("koral.xsd")));
		} 
	    catch (SAXException ex) 
	    {
	    	throw new KoralError(ex);
		}
	}
	
	public void validate(InputStream is) throws SAXException, IOException
	{
        Validator validator = schema.newValidator();
        Source source = new StreamSource(is);
        validator.validate(source);
	}
}
