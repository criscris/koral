package xyz.koral.internal;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class XmlDocument 
{
	public static final Charset cs = Charset.forName("UTF-8");
	public static final String sourceOffsetAtt = "sourceOffset";
	public static final String noOfBytesAtt = "noOfBytes";
	
	Document doc;
	XPathFactory xFactory = XPathFactory.newInstance();
	
	public static boolean checkUTF8Encoding(InputStream is) 
	{
		try
		{
			XMLInputFactory inputFactory = XMLInputFactory.newInstance();
			XMLStreamReader reader = inputFactory.createXMLStreamReader(is);
			String encoding = reader.getEncoding();
			reader.close();
			return cs.name().equalsIgnoreCase(encoding);
		}
		catch (XMLStreamException ex)
		{
			throw new KoralError(ex);
		}
	}
	
	public XmlDocument(InputStream is)
	{
		try
		{
			DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
			doc = dBuilder.parse(is);
		}
		catch (IOException | SAXException | ParserConfigurationException ex)
		{
			throw new KoralError(ex);
		}
	}
	
	public static XmlDocument skipChildren(InputStream is, String nodeName)
	{
		DenseCharVector strippedDoc = new DenseCharVector();
		
		abstract class CharProcessor
		{
			CharProcessor nextState;
			
			CharRingBuffer ringBuffer;
			char[] matchSequence;
			
			public CharProcessor(String matchSequence)
			{
				this.matchSequence = matchSequence.toCharArray();
				ringBuffer = new CharRingBuffer(matchSequence.length());
			}
			
			public CharProcessor match(char c, long offset)
			{
				ringBuffer.add(c);
				return process(c, ringBuffer.startsWith(matchSequence), offset);
			}
			
			abstract CharProcessor process(char c, boolean sequenceMatched, long offset);
		}
		 
		class TagFinder extends CharProcessor
		{
			public TagFinder() 
			{
				super("<" + nodeName);
			}

			CharProcessor process(char c, boolean sequenceMatched, long offset) 
			{
				strippedDoc.add(c);
				if (sequenceMatched)
				{
					return nextState;
				}
				
				return this;
			}
		}
		
		class StartOfChildrenFinder extends CharProcessor
		{
			public StartOfChildrenFinder() 
			{
				super(">");
			}

			CharProcessor process(char c, boolean sequenceMatched, long offset) 
			{
				if (sequenceMatched)
				{
					return nextState;
				}
				strippedDoc.add(c);
				
				return this;
			}
		}
		
		class EndOfChildrenFinder extends CharProcessor
		{
			long fileOffset = 0;
			boolean first = true;
			
			public EndOfChildrenFinder() 
			{
				super("</" + nodeName + ">");
			}

			CharProcessor process(char c, boolean sequenceMatched, long offset) 
			{
				if (first) 
				{
					fileOffset = offset;
					first = false;
				}
				
				if (sequenceMatched)
				{
					long noOfBytes = offset - matchSequence.length + 1 - fileOffset;
					strippedDoc.add(" " + sourceOffsetAtt + "=\"" + fileOffset + "\" " + noOfBytesAtt + "=\"" + noOfBytes + "\"/>");
					first = true;
					return nextState;
				}
				
				return this;
			}
		}
		
		TagFinder tag = new TagFinder();
		StartOfChildrenFinder start = new StartOfChildrenFinder();
		EndOfChildrenFinder end = new EndOfChildrenFinder();
		tag.nextState = start;
		start.nextState = end;
		end.nextState = tag;
		
		CharProcessor current = tag;
		UTF8ByteCountReader reader = new UTF8ByteCountReader(is, true);
		int readChars = 0;
		
		
		try
		{
			while ((readChars = reader.read()) > 0)
			{
				for (int i=0; i<readChars; i++)
				{	
					char c = reader.buf[i];
					long offset = reader.offsets[i];
					current = current.match(c, offset);
				}
			}
			reader.close();
		}
		catch (IOException ex)
		{
			throw new KoralError(ex);
		}

		
		return new XmlDocument(new ByteArrayInputStream(strippedDoc.toString().getBytes(cs)));
	}
	
	public Element root()
	{
		return doc.getDocumentElement();
	}
	
	public static List<Element> children(Element parent, String... allowedChildTagNames)
	{
		List<Element> children = new ArrayList<Element>();
		NodeList nodes = parent.getChildNodes();
		for (int i=0; i<nodes.getLength();i++)
		{
			Node node = nodes.item(i);
			if (node instanceof Element)
			{
				Element e = (Element) node;
				boolean hasName = false;
				for (String c : allowedChildTagNames)
				{
					if (e.getTagName().equals(c))
					{
						hasName = true;
						break;
					}
				}
				if (hasName) children.add(e);
			}
		}
		return children;
	}
	
	public List<Element> xpath(String xpath)
	{
		return xpath(root(), xpath);
	}
	
	public List<Element> xpath(Element parent, String xpath)
	{
		try
		{
			XPath p = xFactory.newXPath();
			XPathExpression expr = p.compile(xpath);
			NodeList nodes = (NodeList) expr.evaluate(parent, XPathConstants.NODESET);

			List<Element> elements = new ArrayList<Element>();
			for (int i=0; i<nodes.getLength();i++)
			{
				Node node = nodes.item(i);
				if (node instanceof Element) elements.add((Element) node);
			}
			return elements;
		}
		catch (XPathExpressionException ex)
		{
			throw new KoralError(ex);
		}
	}
	
	public void save(OutputStream os)
	{
		try
		{
			Transformer transformer = TransformerFactory.newInstance().newTransformer();
			transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
			transformer.setOutputProperty(OutputKeys.METHOD, "xml");
			transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
			transformer.setOutputProperty(OutputKeys.INDENT, "yes");
			transformer.setOutputProperty(OutputKeys.ENCODING, cs.name());
			StreamResult result = new StreamResult(os);
			DOMSource source = new DOMSource(doc);
			transformer.transform(source, result);
		}
		catch (TransformerException ex)
		{
			throw new KoralError(ex);
		}
	}
}
