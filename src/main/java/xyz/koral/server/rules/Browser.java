package xyz.koral.server.rules;

import java.util.Arrays;

import xyz.koral.HTML;
import xyz.koral.IO;
import xyz.koral.server.KoralServlet.Action;
import xyz.koral.server.KoralServlet.Verb;
import xyz.koral.server.Method;
import xyz.koral.server.ServerUtil;

public class Browser 
{
	public static Method browseFolder = new Method(
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
			resp.setContentType(ServerUtil.htmlMediaType);
			resp.getWriter().append(html).close();
		});
	
	public static Method fileViewer = new Method(
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
			.add("div", " ", "class", "viewer", 
					"data-url", "/" + req.projectName + "/" + req.path,
					"data-size", "" + req.file().length())
			.create();
			resp.setContentType(ServerUtil.htmlMediaType);
			resp.getWriter().append(html).close();
		});
	
	public static Method filesMeta = new Method(
		Arrays.asList(
				r -> r.verb == Verb.GET,
				r -> r.action == Action.listFiles,
				Resources.fileExists,
				r -> r.file().isDirectory()), 
		(req, resp) -> 
		{
			String json = IO.listFilesJSON(req.file(), req.path);
			resp.setContentType(ServerUtil.jsonMediaType);
			resp.getWriter().append(json).close();
		});
}
