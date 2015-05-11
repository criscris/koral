package xyz.koral.internal;


import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.net.InetSocketAddress;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.logging.StreamHandler;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
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
import org.xml.sax.SAXException;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

public class KoralServer 
{
	private final static Logger L = Logger.getLogger(KoralServer.class.getName()); 
	public static final String PORTINFO = "serverstartedonport=";
	
	public static void main(String[] args) throws Exception
	{
		List<File> pathes = new ArrayList<>();
		int port = 0; // assign a random free port
		for (int i=0; i<args.length; i++)
		{
			if (args[i].startsWith("port="))
			{
				port = new Integer(args[i].substring(5));
			}
			
			File path = new File(args[i]);
			if (path.exists())
			{
				pathes.add(path);
			}
			else
			{
				System.out.println("Path does not exist: " + path.getAbsolutePath());
			}
		}
		
		KoralServer s = new KoralServer(port);
		s.acceptConnections();
		System.out.println(PORTINFO + s.server.getAddress().getPort());
		L.info("rock'n'roll");
	}
	
	HttpServer server;
	//XhtmlTemplate siteData;
	
	public KoralServer(int port) throws Exception
	{
		//siteData = new XhtmlTemplate(KoralServer.class.getResourceAsStream("koralpage.xml"));
		
		server = HttpServer.create(new InetSocketAddress(port), 0);
	    server.createContext("/", new HttpHandler() 
	    {
	    	class Data
	    	{
	    		byte[] data;
	    		String name;
	    		
	    		public Data(String name) throws IOException
	    		{
	    			this.name = name;
	    			data = BinaryFiles.getData(KoralServer.class.getResourceAsStream("webclient/" + name));
	    		}
	    		
	    		public String getContentType()
	    		{
	    			int i1 = name.lastIndexOf(".");
	    			if (i1 == -1) return "";
	    			String extension = name.substring(i1 + 1).toLowerCase();
	    			
	    			switch (extension)
	    			{
	    			case "js": return "application/javascript;charset=utf-8";
	    			case "css": return "text/css;charset=utf-8";
	    			case "png": return "image/png";
	    			case "svg": return "image/svg+xml";
	    			case "xml": return "text/xml;charset=utf-8";
	    			case "xhtml": 
	    			case "html": return "text/html;charset=utf-8";
	    			}
	    			return "";
	    		}
	    	}
	    	Map<String, Data> fileMap = new HashMap<>();
	    	
			public void handle(HttpExchange t) throws IOException 
			{
				String uri = t.getRequestURI().getPath();
				int i1 = uri.lastIndexOf("/");
				if (i1 == -1) return;
				
				String name = uri.substring(i1 + 1);
				Data d = fileMap.get(name);
//				if (d == null)
//				{
					d = new Data(name);
					fileMap.put(name, d);
//				}
				t.getResponseHeaders().add("Content-Type", d.getContentType());
				t.sendResponseHeaders(200, d.data.length);
				
				
				OutputStream os = t.getResponseBody();
				os.write(d.data);
				os.close();
			}
		});
	    
	    server.createContext("/log", new XmlLogHandler(L));
	    
	    server.createContext("/shutdown", new HttpHandler() 
	    {
			public void handle(HttpExchange t) throws IOException 
			{
				server.stop(1);
				System.out.println("Shut down koral server.");
			}
	    });
	    server.createContext("/ping", new HttpHandler() 
	    {
			public void handle(HttpExchange t) throws IOException 
			{
				HttpUtil.writeStringAsResponse("koral\n", t);
			}
	    });
	    
	    KoralServices koral2Services = new KoralServices();
	    for (Entry<String, HttpHandler> entry : koral2Services.createHttpHandlers())
	    {
	    	server.createContext(entry.getKey(), entry.getValue());
	    }
	    
	    server.setExecutor(null); 
	}
	
	public void acceptConnections()
	{
		server.start();
	}
}


class XhtmlTemplate
{
	Document doc;
	
	public XhtmlTemplate(InputStream is) throws ParserConfigurationException, SAXException, IOException
	{
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		DocumentBuilder db = dbf.newDocumentBuilder(); 
		doc = db.parse(is);
	}
	
	Document getCopy()
	{
		return (Document) doc.cloneNode(true);
	}
}

class HttpUtil
{
	public static Map<String, String> parameterMap(HttpExchange t)
	{
	    Map<String, String> result = new HashMap<String, String>();
	    String query = t.getRequestURI().getQuery();
	    if (query != null)
	    {
    	    for (String param : query.split("&")) 
    	    {
    	    	int i1 = param.indexOf("=");
    	    	if (i1 != -1)
    	    	{
    	    		result.put(param.substring(0, i1).toLowerCase(), param.substring(i1 + 1));
    	    	}
    	        else
    	        {
    	            result.put(param.toLowerCase(), "");
    	        }
    	    }
	    }
	    return result;
	}
	
	public static void writeStringAsResponse(String answer, HttpExchange t) throws IOException
	{
		byte[] data = answer.getBytes("UTF-8");
		t.sendResponseHeaders(200, data.length);			
		OutputStream os = t.getResponseBody();
		os.write(data);
		os.close();
	}
}

class KoralPage
{
	Document doc;
	
	public KoralPage(Document doc)
	{
		this.doc = doc;
	}
	
	public void setJsonData(String json, String varName) throws XPathExpressionException
	{
		XPath xpath = XPathFactory.newInstance().newXPath();
		XPathExpression expr = xpath.compile("/html/head/script[@id='parseDocumentData']");
		Element script2 = (Element) expr.evaluate(doc, XPathConstants.NODE);
		
		Element script = doc.createElement("script");
		script.setAttribute("type", "application/json");
		script.setAttribute("id", varName);
		script.appendChild(doc.createTextNode(json));		
		script2.appendChild(doc.createTextNode("var " + varName + " = JSON.parse(document.getElementById('" + varName + "').innerHTML);"));
		
		script2.getParentNode().insertBefore(script, script2);
	}
	
	public void writeTo(OutputStream out) throws IOException, TransformerException 
	{
	    TransformerFactory tf = TransformerFactory.newInstance();
	    Transformer transformer = tf.newTransformer();
	    transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
	    transformer.setOutputProperty(OutputKeys.METHOD, "xml");
	    transformer.setOutputProperty(OutputKeys.INDENT, "yes");
	    transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
	    transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");

	    transformer.transform(new DOMSource(doc), new StreamResult(new OutputStreamWriter(out, "UTF-8")));
	}
}

class XmlLogHandler implements HttpHandler
{
	ByteArrayOutputStream bos;
	StreamHandler handler;
	Logger logger;
	Formatter formatter;
	
	public XmlLogHandler(Logger logger)
	{
		this.logger = logger;
		logger.setLevel(Level.INFO);
		formatter = new XmlLogFormatter();
		resetHandler();
		
		logger.setUseParentHandlers(false); // no console logging
	}
	
	void resetHandler()
	{
		bos = new ByteArrayOutputStream();
		handler = new StreamHandler(bos, formatter);
		logger.addHandler(handler);
	}

	public void handle(HttpExchange t) throws IOException
	{
		handler.flush();
		handler.close();
		logger.removeHandler(handler);
		byte[] data = bos.toByteArray();
		resetHandler();
		t.sendResponseHeaders(200, 0);	
		BinaryFiles.copy(new ByteArrayInputStream(data), t.getResponseBody(), true, true);
	}
}


class XmlLogFormatter extends Formatter
{
	Charset cs = Charset.forName("UTF-8");
	SimpleDateFormat sd = new SimpleDateFormat("yyyyMMdd_HH:mm:ss.SSS");
	
	public String format(LogRecord record) 
	{
		StringBuilder sb = new StringBuilder("\t<log ");
		sb.append("date=\"");
		sb.append(sd.format(new Date(record.getMillis())));
		sb.append("\" level=\"");
		sb.append(record.getLevel().toString());
		sb.append("\">");
		sb.append(new XmlEscaper().escapeContent(formatMessage(record)));
		
		Throwable t = record.getThrown();
		if (t != null)
		{
			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			PrintStream pos = new PrintStream(bos);
			t.printStackTrace(pos);
			pos.close();
			sb.append("\n");
			sb.append(new String(bos.toByteArray(), cs));
		}
		sb.append("</log>\n");
		
		return sb.toString();
	}

	public String getHead(Handler h) 
	{
		StringBuilder sb = new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
		sb.append("<logs>\n");
		return sb.toString();
	}

	public String getTail(Handler h) 
	{
		return "</logs>\n";
	}
}
