package xyz.koral.internal;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import com.google.common.io.ByteStreams;

import xyz.koral.Git;
import xyz.koral.HTML;
import xyz.koral.IO;
import xyz.koral.internal.KoralServlet.Action;
import xyz.koral.internal.KoralServlet.Verb;

public class KoralServlet extends HttpServlet
{
	private static final long serialVersionUID = -3848472167349998166L;
	private static final SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss");
	
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
		listFiles,
		history,
		folderCreation,
		fileCreationOrUpdate,
		rename,
		commit,
		edit
	}

	File configFile;
	
	long configLastModified;
	ServerConfiguration config;
	List<Method> methods;
	
	
	public KoralServlet(File configFile)
	{
		this.configFile = configFile;
		
		Predicate<Request> isAuthor = r -> r.author != null;
		Predicate<Request> fileExists = r -> r.file().exists();

		Method get = new Method(
				Arrays.asList(
						r -> r.verb == Verb.GET,
						r -> r.action == Action.empty,
						r -> !r.isProjectRoot() && !r.file().isDirectory(),
						fileExists), 
				(req, resp) -> 
				{
					File file = req.file();
					resp.setContentType(getContentType(file.getName()));
					
					String limit = req.req.getParameter("limit");
					if (limit != null)
					{
						long limitBytes = Math.min(10000000L, new Long(limit));
						if (limitBytes < file.length())
						{
							IO.copy(ByteStreams.limit(IO.istream(file), limitBytes), resp.getOutputStream());
							return;
						}
					}
			
					resp.setContentLengthLong(file.length());
					IO.copy(IO.istream(file), resp.getOutputStream());
				});
		
		Method browseFolder = new Method(
				Arrays.asList(
						r -> r.verb == Verb.GET,
						r -> r.action == Action.empty,
						r -> r.file().isDirectory()), 
				(req, resp) -> 
				{
					String html = HTML
					.koral(req.projectName, req.config.koralClientURI)
					.header()
					.add("script", " ", 
							"src", "?action=listFiles",
							"type", "application/json",
							"id", "files")
					.body()
					.add("div", null, "class", "koralExplorer")
					.child()
					.add("div", " ", "class", "browser")
					.add("div", " ", "class", "viewer")
					.create();
					resp.setContentType(htmlMediaType);
					resp.getWriter().append(html).close();
				});
		
		Method fileViewer = new Method(
				Arrays.asList(
						r -> r.verb == Verb.GET,
						r -> r.action == Action.edit,
						r -> !r.file().isDirectory()), 
				(req, resp) -> 
				{
					String html = HTML
					.koral(req.projectName, req.config.koralClientURI)
					.header()
					.add("script", " ", 
							"src", "./?action=listFiles",
							"type", "application/json",
							"id", "files")
					.body()
					.add("div", null, "class", "koralExplorer")
					.child()
					.add("div", " ", "class", "browser")
					.add("div", " ", "class", "viewer", "data-url", "/" + req.projectName + "/" + req.path + "?limit=500000")
					.create();
					resp.setContentType(htmlMediaType);
					resp.getWriter().append(html).close();
				});
		
		Method filesMeta = new Method(
				Arrays.asList(
						r -> r.verb == Verb.GET,
						r -> r.action == Action.listFiles,
						fileExists,
						r -> r.file().isDirectory()), 
				(req, resp) -> 
				{
					String json = IO.listFilesJSON(req.file(), req.path);
					resp.setContentType(jsonMediaType);
					resp.getWriter().append(json).close();
				});
				
		Method history = new Method(
				Arrays.asList(
						r -> r.verb == Verb.GET,
						r -> r.action == Action.history,
						isAuthor, 
						fileExists), 
				(req, resp) -> 
				{
					if (req.file().isDirectory())
					{
						resp.sendError(404);
						return;
					}
					String html = Git.historyHTML(new File(req.project.path), req.path, req.config.koralClientURI);
					resp.setContentType(htmlMediaType);
					resp.getWriter().append(html).close();
				});
		
		Method folderCreate = new Method(
				Arrays.asList(
						r -> r.verb == Verb.PUT,
						r -> r.action == Action.folderCreation,
						isAuthor,
						fileExists.negate()), 
				(req, resp) -> 
				{
					if (!req.file().mkdirs())
					{
						resp.sendError(400, "Could not create folder.");
					}
				});
		
		Method rename = new Method(
				Arrays.asList(
						r -> r.verb == Verb.PUT,
						r -> r.action == Action.rename,
						isAuthor, 
						fileExists,
						r -> r.req.getParameter("target") != null && 
						!r.req.getParameter("target").contains("..")), 
				(req, resp) -> 
				{
					String target = req.req.getParameter("target");
					File newFile = new File(req.project.path, target);
					if (newFile.exists())
					{
						resp.sendError(400, "Target path already exists.");
						return;
					}
					if (!req.file().renameTo(newFile))
					{
						resp.sendError(400, "Could not rename file.");
					}
				});
		
		Method delete = new Method(
				Arrays.asList(
						r -> r.verb == Verb.DELETE,
						r -> r.action == Action.empty,
						isAuthor, 
						fileExists,
						r -> !r.isProjectRoot()), 
				(req, resp) -> 
				{
					IO.delete(req.file());
				});
		
		Method fileUpdate = new Method(
				Arrays.asList(
						r -> r.verb == Verb.PUT,
						r -> r.action == Action.fileCreationOrUpdate,
						isAuthor), 
				(req, resp) -> 
				{
					IO.copy(req.req.getInputStream(), new FileOutputStream(req.file()));
				});
		
		Method commit = new Method(
				Arrays.asList(
						r -> r.verb == Verb.POST,
						r -> r.action == Action.commit,
						isAuthor,
						fileExists), 
				(req, resp) -> 
				{
					String message = req.req.getParameter("message");
					String commitMessage = clientIP(req.req);
					if (message != null) commitMessage += ": " + message;
					Git.commit(new File(req.project.path), req.path, req.author, commitMessage);
				});
		
		methods = Arrays.asList(get, browseFolder, fileViewer, filesMeta, history, 
				folderCreate, rename, delete, fileUpdate, commit);
	}
	
	public ServerConfiguration getConfig()
	{
		if (config == null || configFile.lastModified() > configLastModified)
		{
			config = IO.readJSON(IO.istream(configFile), ServerConfiguration.class);
			configLastModified = configFile.lastModified();
		}
		return config;
	}
	
	
	static final String htmlMediaType = "text/html;charset=utf-8";
	static final String jsonMediaType = "application/json;charset=utf-8";
	static String getContentType(String fileName)
	{
		int i1 = fileName.lastIndexOf(".");
		if (i1 == -1) return "";
		String extension = fileName.substring(i1 + 1).toLowerCase();
			
		switch (extension)
		{
		case "js": return "application/javascript;charset=utf-8";
		case "css": return "text/css;charset=utf-8";
		case "png": return "image/png";
		case "svg": return "image/svg+xml";
		case "html": return htmlMediaType;
		case "json": return jsonMediaType;
		case "jsonl": return "application/json;charset=utf-8";
		case "csv": return "text/csv;charset=utf-8";
		}
		return "";
	}
	
	static String getAuthor(HttpServletRequest req, Project project)
	{
		HttpSession session = req.getSession(false);
		if (session != null && project.sessionIDs.contains(session.getId())) return (String) session.getAttribute("author");

		String token = req.getParameter("access_token");
		if (project.tokenToAuthor != null && token != null)
		{
			String author = project.tokenToAuthor.get(token);
			if (author != null)
			{
				session = req.getSession(true);
				session.setMaxInactiveInterval(12*3600);
				session.setAttribute("author", author);
				project.sessionIDs.add(session.getId());
				return author;
			}
		}
		return null;
	}
	
	static String clientIP(HttpServletRequest req)
	{
		String requestHost = req.getHeader("X-Forwarded-For"); // potentially hidden ip because of apache port forwarding
		if (requestHost == null)
		{
			requestHost = req.getRemoteAddr();
			String h = req.getRemoteHost();
			if (h != null && !h.equals(requestHost)) requestHost += "(" + h + ")";
		}
		return requestHost;
	}
	
	static Request parseRequest(HttpServletRequest req)
	{
		Request r = new Request();
		r.req = req;
		String uri = req.getRequestURI().substring(req.getContextPath().length());
		
		if (uri.contains("..")) return null;
		int i1 = uri.indexOf("/");
		if (i1 != 0) return null;
		uri = uri.substring(1); // remove first slash
		
		i1 = uri.indexOf("/");
		if (i1 == -1) return null;
		r.projectName = uri.substring(0,  i1);
		r.path = uri.substring(i1 + 1);
		
		String action = req.getParameter("action");
		r.action = Action.empty;
		if (action != null)
		{
			switch (action)
			{
			case "listFiles": r.action = Action.listFiles; break;
			case "fileCreationOrUpdate": r.action = Action.fileCreationOrUpdate; break;
			case "folderCreation": r.action = Action.folderCreation; break;
			case "rename": r.action = Action.rename; break;
			case "commit": r.action = Action.commit; break;
			case "history": r.action = Action.history; break;
			case "edit": r.action = Action.edit; break;
			}
		}
		return r;
	}
	
	public static String getFullURL(HttpServletRequest request) 
	{
	    String url = request.getRequestURL().toString();
	    String params = Collections.list(request.getParameterNames())
	    .stream()
	    .map(key -> key + "=" + (key.equals("access_token") ? "[hidden]" : request.getParameter(key)))
	    .collect(Collectors.joining("&"));
	    return url + (params.length() > 0 ? "?" + params : "");
	}
	
	void doRequest(Verb verb, HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException 
	{
		Request r = parseRequest(req);
		if (r == null)
		{
			resp.sendError(400);
			return;
		}
		r.verb = verb;
		
		r.config = getConfig();
		if (r.config == null)
		{
			resp.sendError(500);
			return;
		}
		
		r.project = r.config.projects.get(r.projectName);
		if (r.project == null)
		{
			resp.sendError(401);
			return;
		}
		
		r.author = getAuthor(req, r.project);
		System.out.println(sdf.format(Calendar.getInstance().getTime()) + " " + clientIP(req) + " " + r.verb + " " + getFullURL(req) + " author=" + r.author);
		
		if (!r.project.published && r.author == null)
		{
			resp.sendError(401);
			return;
		}
		

		Method method = null;
		for (Method m : methods)
		{
			boolean use = true;
			for (Predicate<Request> assertion : m.conditions)
			{
				if (!assertion.test(r))
				{
					use = false;
					break;
				}
			}
			if (use)
			{
				method = m;
				break;
			}
		}
		
		if (method == null)
		{
			resp.sendError(400, "No matching method.");
			return;
		}
		else
		{
			method.executor.exec(r, resp);
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
 

class Method
{
	List<Predicate<Request>> conditions; 
	MethodExecutor executor;
	
	public Method(List<Predicate<Request>> conditions, MethodExecutor executor)
	{
		this.conditions = conditions;
		this.executor = executor;
	}
}

interface MethodExecutor
{
	void exec(Request req, HttpServletResponse resp) throws ServletException, IOException;
}

class Request
{
	public Verb verb;
	public String projectName;
	public String path;
	
	
	public HttpServletRequest req;
	public String author;
	public Project project;
	
	public ServerConfiguration config;
	
	public String uri()
	{
		return projectName + "/" + path;
	}
	
	public File file()
	{
		return new File(project.path, path);
	}
	
	public boolean isProjectRoot()
	{
		return path.trim().isEmpty();
	}
	
	public Action action;
}

class ServerConfiguration 
{
	public String koralClientURI;
	public Map<String, Project> projects;
}

class Project
{
	public String path;
	public boolean published;
	public Map<String, String> tokenToAuthor;
	
	transient Set<String> sessionIDs = new HashSet<>();
}
