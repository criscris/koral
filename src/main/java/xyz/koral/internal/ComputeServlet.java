package xyz.koral.internal;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Enumeration;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import xyz.koral.Arg;
import xyz.koral.Compute;
import xyz.koral.DataSource;

public class ComputeServlet extends HttpServlet 
{
	private static final long serialVersionUID = 4577838218892779721L;
	
	File sourceFile;
	long configLastModified;
	Map<String, DataSource> config;
	
	public ComputeServlet(File sourceFile)
	{
		this.sourceFile = sourceFile;
	}
	
	public synchronized Map<String, DataSource> getSourceConfig()
	{
		if (config == null || sourceFile.lastModified() > configLastModified)
		{
			configLastModified = sourceFile.lastModified();
			config = Compute.load(sourceFile);
		}
		return config;
	}
	
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException 
	{
		long time = System.currentTimeMillis();
		
		String path = req.getRequestURI().substring(req.getContextPath().length());		
		if (path.contains("..")) return;
		if (path.startsWith("/")) path = path.substring(1);
		
		DataSource ds = getSourceConfig().get(path);
		if (ds == null) return;
		ds = new DataSource(ds);
		
		Enumeration<String> params = req.getParameterNames();
		while (params.hasMoreElements())
		{
			String name = params.nextElement();
			String value = req.getParameter(name);
			try 
			{
				value = URLDecoder.decode(value, "UTF-8");
			} 
			catch (UnsupportedEncodingException ex) 
			{
				
			}
			
			//Arg arg = IO.readJSON(new ByteArrayInputStream(value.getBytes("UTF-8")), Arg.class);
			Arg arg = new Arg("param", value);
			//if (arg != null)
			{
				ds.addArg(name, arg);
			}
			
		}
		
		resp.setContentType(KoralServlet.getContentType(path));
		Compute.compute(sourceFile.getParentFile(), path, ds, resp.getOutputStream());
		System.out.println("compute " + path + " in " + (System.currentTimeMillis() - time) + " ms.");
	}

}
