package xyz.koral.server;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import xyz.koral.IO;

/**
 * No head, no cache, always serving current file.
 */
public class StaticServlet extends HttpServlet
{
	private static final long serialVersionUID = 8486465147604085863L;
	File dir;
	
	public StaticServlet(File dir)
	{
		this.dir = dir;
	}
	
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException 
	{
		String path = req.getRequestURI().substring( req.getContextPath().length());		
		if (path.contains("..")) return;

		File file = new File(dir, path);
		if (!file.exists() || file.isDirectory())
		{
			resp.sendError(404);
			return;
		}
		
		resp.setContentType(ServerUtil.getContentType(file.getName()));
		resp.setContentLengthLong(file.length());
		IO.copy(new FileInputStream(file), resp.getOutputStream());
	}
}
