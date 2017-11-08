package xyz.koral.server.rules;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Arrays;
import java.util.function.Predicate;

import com.google.common.io.ByteStreams;

import xyz.koral.IO;
import xyz.koral.server.KoralServlet.Action;
import xyz.koral.server.KoralServlet.Verb;
import xyz.koral.server.HttpCode;
import xyz.koral.server.Method;
import xyz.koral.server.Request;
import xyz.koral.server.ServerUtil;

public class Resources 
{
	public static Predicate<Request> fileExists = r -> r.file().exists();
	public static Predicate<Request> isAuthor = r -> r.isAuthorOfProject();
	
	public static Method get = new Method(
		Arrays.asList(
				r -> r.verb == Verb.GET,
				r -> r.action == Action.empty,
				r -> !r.isProjectRoot() && !r.file().isDirectory()), 
		(req, resp) -> 
		{
			if (!fileExists.test(req))
			{
				resp.sendError(HttpCode.Not_found_404);
				return;
			}
			
			File file = req.file();
			resp.setContentType(ServerUtil.getContentType(file.getName()));
			
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
			//IO.copy(IO.istream(file), resp.getOutputStream()); don't dezip
			IO.copy(new BufferedInputStream(new FileInputStream(file)), resp.getOutputStream());
		});

	public static Method folderCreate = new Method(
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
	
	public static Method rename = new Method(
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
	
	public static Method fileUpdate = new Method(
		Arrays.asList(
				r -> r.verb == Verb.PUT,
				r -> r.action == Action.fileCreationOrUpdate,
				isAuthor), 
		(req, resp) -> 
		{
			IO.copy(req.req.getInputStream(), new FileOutputStream(req.file()));
		});
	
	public static Method delete = new Method(
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
}
