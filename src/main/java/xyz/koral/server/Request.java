package xyz.koral.server;

import java.io.File;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import xyz.koral.server.KoralServlet.Action;
import xyz.koral.server.KoralServlet.Verb;
import xyz.koral.server.config.Project;
import xyz.koral.server.config.ServerConf;

public class Request
{
	public Verb verb;
	public String projectName;
	public String path;
	
	public HttpServletRequest req;
	public String author;
	public Project project;
	
	public ServerConf config;
	public boolean configModified;
	
	public boolean isAuthorOfProject()
	{
		if (projectName == null || author == null) return false;
		Set<String> p = config.users.get(author).projects;
		if (p == null) return false;
		return p.contains(projectName);
	}
	
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
	
	public boolean isServerRoot()
	{
		return projectName == null;
	}
	
	public Action action;
}