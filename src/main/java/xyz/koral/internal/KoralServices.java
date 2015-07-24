package xyz.koral.internal;


import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

import xyz.koral.Array;
import xyz.koral.Entry;
import xyz.koral.Koral;
import xyz.koral.KoralIO;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

public class KoralServices 
{
	private final static Logger L = Logger.getLogger(KoralServices.class.getName()); 
	
	Koral k = new Koral();
	
	static final String fileUri = "/file";
	class FileLoader implements HttpHandler
	{
		public void handle(HttpExchange t) throws IOException 
		{
			try 
			{
				String uri = t.getRequestURI().getPath().substring(fileUri.length());
				File file = new File(uri);
				Koral kk = KoralIO.load(file);
				k.add(kk);
				
				File indexFile = new Index(file).getIndexFile();
				XmlDocument xml = new XmlDocument(new FileInputStream(indexFile));
				
				t.getResponseHeaders().add("Content-Type", "text/xml;charset=utf-8");
				t.sendResponseHeaders(200, 0);
				OutputStream os = t.getResponseBody();
				xml.save(os);
				os.close();
			} 
			catch (Exception e) 
			{
				L.log(Level.WARNING, e.getMessage(), e);
				t.sendResponseHeaders(400, 0);
				t.getResponseBody().close();
			}
		}	
	}
	
	
	static final String arrayUri = "/array";
	class ArrayContentStreamer implements HttpHandler
	{
		public void handle(HttpExchange t) throws IOException 
		{
			try 
			{
				String uri = t.getRequestURI().getPath().substring(arrayUri.length() + 1);
				Array a = k.asArray(uri);
				if (a == null) throw new KoralError("Array id not found.");
				
				t.getResponseHeaders().add("Content-Type", "text/plain;charset=utf-8");
				t.sendResponseHeaders(200, 0);
				
				BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(t.getResponseBody(), XmlDocument.cs));
				a.forEach(new Consumer<Entry>() 
				{
					long index = 0;
					XmlEscaper escaper = new XmlEscaper();
					
					public void accept(Entry t) 
					{
						try
						{
							if (t.pitchIndex() != 0) return;
							long eIndex = t.index();
							while (index != eIndex)
							{
								writer.write("|");
								index++;
							}
							writer.write(escaper.escapeContent(t.getS()));
						}
						catch (IOException ex)
						{
							throw new KoralError(ex);
						}
					}
				});
				writer.write("\n"); // R needs a last line return
				writer.close();
			} 
			catch (Exception e) 
			{
				L.log(Level.WARNING, e.getMessage(), e);
				t.sendResponseHeaders(400, 0);
				t.getResponseBody().close();
			}
		}
	}
	
	public List<java.util.Map.Entry<String, HttpHandler>> createHttpHandlers()
	{
		Map<String, HttpHandler> handlers = new HashMap<>();
		handlers.put(fileUri, new FileLoader());
		handlers.put(arrayUri, new ArrayContentStreamer());
		return new ArrayList<>(handlers.entrySet());
	}
}
