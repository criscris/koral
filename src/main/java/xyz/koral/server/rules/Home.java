package xyz.koral.server.rules;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import xyz.koral.HTML;
import xyz.koral.server.KoralServlet.Action;
import xyz.koral.server.KoralServlet.Verb;
import xyz.koral.server.HttpCode;
import xyz.koral.server.Method;
import xyz.koral.server.ServerUtil;
import xyz.koral.server.config.ServerConf;
import xyz.koral.server.config.User;

public class Home 
{
	public static Method shutDownMethod = new Method(Arrays.asList(
			r -> r.action == Action.shutDown), 
	(req, resp) -> 
	{
		String client = ServerUtil.clientIP(req.req);
		if (!"127.0.0.1".equals(client) && !"localhost".equals(client))
		{
			resp.sendError(HttpCode.Unauthorized_401);
			return;
		}
			
		new Thread(() -> 
		{
			try 
			{
				Thread.sleep(3000);
			} 
			catch (InterruptedException e) 
			{

			}
			Runtime.getRuntime().exit(0);
		}).start();
	});
	
	public static Method start = new Method(
		Arrays.asList(
				r -> r.verb == Verb.GET,
				r -> r.action == Action.empty), 
		(req, resp) -> 
		{
			String html = 
			loginForm(
			HTML
			.koral("Koral", req.config.koralClientURI)
			.header()
			.add("script", " ", 
					"src", "/?action=listProjects",
					"type", "application/json",
					"id", "files")
			.body()
			.add("div", null, "class", "koralProjects"))
			.create();
			resp.setContentType(ServerUtil.htmlMediaType);
			resp.getWriter().append(html).close();
		});
	
	static HTML loginForm(HTML body)
	{
		return body.add("form", null, "id", "signInForm", "style", "margin:2rem")
		.child()
			.add("h3", "Sign in")
			.add("label", "Username", "for", "login")
			.add("input", null, "id", "username", "name", "login", "tabIndex", "1", "type", "text", 
					"autocapitalize", "off", "autocorrect", "off", "autofocus", "autofocus")
			.add("label", "Password", "for", "password", "style", "margin-top:1rem")
			.add("input", null, "id", "password", "name", "password", "tabIndex", "2", "type", "password")
			.add("br", null)
			.add("input", null, "value", "Sign in", "style", "margin-top:1rem", "tabIndex", "2", "type", "submit",
					"onclick", "signIn(); return false;")
			.add("script", "function signIn() { KoralInternal.signIn($('#username').val(), $('#password').val(), function() { $('#signInForm').remove(); })}")
		.parent();
	}
	
	public static Method listProjects = new Method(
		Arrays.asList(
				r -> r.verb == Verb.GET,
				r -> r.action == Action.listProjects), 
		(req, resp) -> 
		{
			Gson gson = new GsonBuilder().setPrettyPrinting().create();
			String json = gson.toJson(new ProjectList(req.config, req.author));
			resp.setContentType(ServerUtil.jsonMediaType);
			resp.getWriter().append(json).close();
		});
}

class ProjectList
{
	public List<String> authoredProjects;
	public List<String> publicProjects;
	
	public ProjectList(ServerConf config, String user)
	{
		Set<String> userProjects = null;
		if (user != null)
		{
			User u = config.users.get(user);
			if (u != null)
			{
				userProjects = u.projects;
				authoredProjects = u.projects.stream().sorted().collect(Collectors.toList());
			}
		}
		
		Set<String> userProjects_ = userProjects;
		publicProjects = config.projects.keySet().stream()
				.filter(p -> config.projects.get(p).published)
				.filter(p -> userProjects_ == null || !userProjects_.contains(p))
				.sorted()
				.collect(Collectors.toList());
		
	}
}
