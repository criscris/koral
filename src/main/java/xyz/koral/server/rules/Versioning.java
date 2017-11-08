package xyz.koral.server.rules;

import java.io.File;
import java.util.Arrays;

import xyz.koral.Git;
import xyz.koral.server.KoralServlet.Action;
import xyz.koral.server.KoralServlet.Verb;
import xyz.koral.server.Method;
import xyz.koral.server.ServerUtil;

public class Versioning 
{
	public static Method history = new Method(
		Arrays.asList(
				r -> r.verb == Verb.GET,
				r -> r.action == Action.history,
				Resources.isAuthor, 
				Resources.fileExists), 
		(req, resp) -> 
		{
			if (req.file().isDirectory())
			{
				resp.sendError(404);
				return;
			}
			String html = Git.historyHTML(new File(req.project.path), req.path, req.config.koralClientURI);
			resp.setContentType(ServerUtil.htmlMediaType);
			resp.getWriter().append(html).close();
		});
	
	public static Method commit = new Method(
		Arrays.asList(
				r -> r.verb == Verb.POST,
				r -> r.action == Action.commit,
				Resources.isAuthor,
				Resources.fileExists), 
		(req, resp) -> 
		{
			String message = req.req.getParameter("message");
			String commitMessage = ServerUtil.clientIP(req.req);
			if (message != null) commitMessage += ": " + message;
			Git.commit(new File(req.project.path), req.path, req.author, commitMessage);
		});
}
