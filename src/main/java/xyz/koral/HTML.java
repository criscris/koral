package xyz.koral;

import java.io.StringWriter;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class HTML 
{
	Document doc;
	String title;
	
	Element head;
	Element body;
	
	Element current;
	Element lastAppended = null;

	public HTML(String title)
	{
		DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
		try 
		{
			DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
			doc = docBuilder.newDocument();
			
			Element root = doc.createElement("html");
			doc.appendChild(root);
			
			head = doc.createElement("head");
			root.appendChild(head);
			
			current = head;
			add("meta", null, "charset", "UTF-8");
			if (title != null) add("title", title);
			
			
			body = doc.createElement("body");
			root.appendChild(body);
			
			current = body;
		} 
		catch (ParserConfigurationException ex) 
		{
			throw new KoralError(ex);
		}
	}
	
	public HTML title(String title)
	{
		this.title = title;
		return this;
	}
	
	public HTML header()
	{
		current = head;
		return this;
	}
	
	public HTML body()
	{
		current = body;
		return this;
	}
	
	public HTML child()
	{
		if (lastAppended == null) throw new KoralError("Nothing appended.");
		current = lastAppended;
		return this;
	}
	
	public HTML parent()
	{
		current = (Element) current.getParentNode();
		return this;
	}
	
	
	public HTML add(String tagName, String content, String... attributekeyValues)
	{
		lastAppended = create(tagName, content, attributekeyValues);
		current.appendChild(lastAppended);
		return this;
	}
	
	public HTML add(String content)
	{
		current.appendChild(doc.createTextNode(content));
		return this;
	}
	
	private Element create(String tagName, String content, String... attributekeyValues)
	{
		Element elem = doc.createElement(tagName);
		
		if (content != null) elem.setTextContent(content);
		if (attributekeyValues != null && attributekeyValues.length > 0)
		{
			for (int i=0; i<attributekeyValues.length/2; i++)
			{
				elem.setAttribute(attributekeyValues[i*2], attributekeyValues[i*2+1]);
			}
		}
		return elem;
	}
	
	public String create()
	{
		try 
		{
			StringWriter sw = new StringWriter();
			TransformerFactory tf = TransformerFactory.newInstance();
			Transformer transformer = tf.newTransformer();
	        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
	        transformer.setOutputProperty(OutputKeys.METHOD, "xml");
	        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
	        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
	        transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
	        transformer.transform(new DOMSource(doc), new StreamResult(sw));
	       
	        return "<!DOCTYPE html>\n" + sw.toString();
		} 
		catch (TransformerException ex)
		{
			throw new KoralError(ex);
		}
	}
	
	public static HTML koral(String title, String koralBaseURI)
	{
		if (koralBaseURI.length() > 0 && !koralBaseURI.endsWith("/")) koralBaseURI += "/";
		return new HTML(title).header().add("script", " ", "src", koralBaseURI + "koral.js").body();
	}
}
