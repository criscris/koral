package xyz.koral.server;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import xyz.koral.server.config.ServerConf;
import xyz.koral.server.rules.Authentication;
import xyz.koral.server.rules.Browser;
import xyz.koral.server.rules.Home;
import xyz.koral.server.rules.Presentation;
import xyz.koral.server.rules.Resources;
import xyz.koral.server.rules.Versioning;

public class KoralServlet extends HttpServlet
{
	private static final long serialVersionUID = -3848472167349998166L;
	static final SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss");
	
	public enum Verb
	{
		GET,
		PUT,
		DELETE,
		POST
	}
	
	public enum Action
	{
		empty,
		signUp,
		signIn,
		signOut,
		listProjects,
		compute,
		listFiles,
		history,
		folderCreation,
		fileCreationOrUpdate,
		rename,
		commit,
		edit,
		setSlide,
		getSlide,
		shutDown
	}

	JsonFileStore<ServerConf> config;
	
	List<Method> rootMethods;
	List<Method> projectMethods;
	Runnable shutDown;

	public KoralServlet(File configFile, Method... serverMethods)
	{
		config = new JsonFileStore<>(configFile, ServerConf.class);
		
		Presentation presentation = new Presentation();
		
		rootMethods = Stream.concat(Stream.of(serverMethods), Stream.of(
				Home.shutDownMethod, Home.start, Home.listProjects,
				Authentication.signIn, Authentication.signOut, Authentication.signUpPage, Authentication.signUp)).collect(Collectors.toList());
		
		projectMethods = Arrays.asList(
				Resources.get, Browser.browseFolder, Browser.fileViewer, Browser.filesMeta, Versioning.history, 
				Resources.folderCreate, Resources.rename, Resources.delete, Resources.fileUpdate, Versioning.commit, 
				presentation.setSlide, presentation.getSlide);
	}
	
	static Request parseRequest(HttpServletRequest req)
	{
		Request r = new Request();
		r.req = req;
		String uri;
		try 
		{
			uri = URLDecoder.decode(req.getRequestURI().substring(req.getContextPath().length()), "UTF-8");
		} 
		catch (UnsupportedEncodingException e) 
		{
			return null;
		}
		
		
		if (uri.contains("..")) return null;
		int i1 = uri.indexOf("/");
		if (i1 != 0) return null;
		uri = uri.substring(1); // remove first slash
		
		if (uri.length() > 0)
		{
			i1 = uri.indexOf("/");
			if (i1 == -1)
			{
				r.projectName = uri;
				r.path = "";
			}
			else
			{
				r.projectName = uri.substring(0,  i1);
				r.path = uri.substring(i1 + 1);			
			}
		}
		
		String action = req.getParameter("action");
		r.action = Action.empty;
		if (action != null)
		{
	        try 
	        {
	        	r.action = Action.valueOf(action);
	        } 
	        catch(IllegalArgumentException ex) 
	        {
	        	return null;
	        }
		}
		return r;
	}
	
	public static String getFullURL(HttpServletRequest request) 
	{
	    String url = request.getRequestURL().toString();
	    String params = Collections.list(request.getParameterNames())
	    .stream()
	    .map(key -> key + "=" + request.getParameter(key))
	    .collect(Collectors.joining("&"));
	    return url + (params.length() > 0 ? "?" + params : "");
	}
	
	void doRequest(Verb verb, HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException 
	{
		Request r = parseRequest(req);
		if (r == null)
		{
			resp.sendError(HttpCode.Bad_request_400);
			return;
		}
		r.verb = verb;
		
		try
		{
			r.config = config.get();
		}
		catch (Exception ex)
		{
			resp.sendError(HttpCode.Internal_server_error_500, "Invalid server config json: " + ex.getMessage());
			return;
		}
		
		r.author = Authentication.authenticate(req);
		
		if (r.isServerRoot())
		{
			exec(r, rootMethods, resp);
			return;
		}
		
		r.project = r.config.projects.get(r.projectName);
		if (r.project == null)
		{
			resp.sendError(HttpCode.Unauthorized_401);
			return;
		}
		
		if (!r.project.published && !r.isAuthorOfProject())
		{
			resp.sendError(HttpCode.Unauthorized_401);
			return;
		}
		
		exec(r, projectMethods, resp);
	}
	
	void exec(Request r, List<Method> methods, HttpServletResponse resp)  throws ServletException, IOException
	{
		Method method = Method.match(methods, r);
		if (method == null)
		{
			resp.sendError(HttpCode.Bad_request_400);
			return;
		}
		method.executor.exec(r, resp);
		System.out.println(sdf.format(Calendar.getInstance().getTime()) + " " + ServerUtil.clientIP(r.req) + " " + r.verb + " " + getFullURL(r.req) + " author=" + r.author);
		if (r.configModified)
		{
			config.save();
		}
	}
	
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException 
	{
		doRequest(Verb.GET, req, resp);
	}

	protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException 
	{
		doRequest(Verb.PUT, req, resp);
	}

	protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException 
	{
		doRequest(Verb.DELETE, req, resp);
	}

	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException 
	{
		doRequest(Verb.POST, req, resp);
	}
}


 









