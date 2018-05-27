package xyz.koral.compute.java;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import xyz.koral.compute.config.DataFormat;
import xyz.koral.compute.config.DataSource;
import xyz.koral.server.HttpCode;
import xyz.koral.server.KoralServlet.Action;
import xyz.koral.server.KoralServlet.Verb;
import xyz.koral.server.ServerUtil;

public class ComputeServlet extends HttpServlet 
{
	Cache cache = new Cache();
	
	private static final long serialVersionUID = -7354219903110816859L;
	
	void doRequest(Verb verb, HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException 
	{
		String uri;
		try 
		{
			uri = URLDecoder.decode(req.getRequestURI().substring(req.getContextPath().length()), "UTF-8");
			if (uri.startsWith("/")) uri = uri.substring(1);
		} 
		catch (UnsupportedEncodingException ex) 
		{
			resp.sendError(HttpCode.Internal_server_error_500);
			return;
		}

		String a = req.getParameter("action");
		Action action = Action.empty;
		if (a != null)
		{
	        try 
	        {
	        	action = Action.valueOf(a);
	        } 
	        catch(IllegalArgumentException ex) 
	        {
	        	resp.sendError(HttpCode.Bad_request_400);
	        	return;
	        }
		}
				
		if (verb == Verb.GET)
		{
			if (action == Action.shutDown)
			{
				shutDown();
				return;
			}
			if (action == Action.empty)
			{
				if (uri == null || uri.length() == 0)
				{
		        	resp.sendError(HttpCode.Bad_request_400);
		        	return;
				}
				if (!cache.exists(uri))
				{
					resp.sendError(HttpCode.Not_found_404);
					return;
				}
				
				Object data = cache.value(uri);
				DataFormat f = cache.format(uri);
				resp.setContentType(ServerUtil.getContentType(f));
				Conversion.output(data, resp.getOutputStream(), f);
			}
		}
		else if (verb == Verb.POST && action == Action.compute && uri != null && uri.length() > 0)
		{
			DataSource ds = ServerUtil.body(req, DataSource.class);
			OutputStream os = null;
			if (!ds.nostore)
			{
				os = resp.getOutputStream();
				resp.setContentType(ServerUtil.getContentType(ds.type));
			}
			JavaFunction.compute(uri, ds, cache, os);
			return;
		}
		
		resp.sendError(HttpCode.Bad_request_400);
	}
	
	void shutDown()
	{
		new Thread(() -> 
		{
			try 
			{
				Thread.sleep(200);
			} 
			catch (InterruptedException e) 
			{

			}
			Runtime.getRuntime().exit(0);
		}).start();
	}

	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException 
	{
		doRequest(Verb.GET, req, resp);
	}
	
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException 
	{
		doRequest(Verb.POST, req, resp);
	}
}
