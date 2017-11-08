package xyz.koral.server;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;

import xyz.koral.IO;
import xyz.koral.compute.config.DataFormat;

public class ServerUtil 
{
	public static final String htmlMediaType = "text/html;charset=utf-8";
	public static final String jsonMediaType = "application/json;charset=utf-8";
	public static final String csvMediaType = "text/csv;charset=utf-8";
	public static final String txtMediaType = "text/plain;charset=utf-8";
	public static final String svgMediaType = "image/svg+xml;charset=utf-8";
	public static final String jsMediaType = "application/javascript;charset=utf-8";
	public static final String cssMediaType = "text/css;charset=utf-8";

	public static String getContentType(String fileName)
	{
		int i1 = fileName.lastIndexOf(".");
		if (i1 == -1) return "";
		String extension = fileName.substring(i1 + 1).toLowerCase();
			
		switch (extension)
		{
		case "js": return jsMediaType;
		case "css": return cssMediaType;
		case "svg": return svgMediaType;
		case "html": return htmlMediaType;
		case "json": return jsonMediaType;
		case "jsonl": return jsonMediaType;
		case "csv": return csvMediaType;
		case "txt": return txtMediaType;
		case "png": return "image/png";
		case "jpeg": return "image/jpeg";
		case "jpg": return "image/jpeg";
		default: return "";
		}
	}
	
	public static String getContentType(DataFormat format)
	{
		switch (format)
		{
		case json: return jsonMediaType;
		case jsonl: return jsonMediaType;
		case csv: return csvMediaType;
		case txt: return txtMediaType;
		case bin: return "application/octet-stream";
		default: return "";
		}
	}
	
	public static String clientIP(HttpServletRequest req)
	{
		String requestHost = req.getHeader("X-Forwarded-For"); // potentially hidden ip because of apache port forwarding
		if (requestHost == null)
		{
			requestHost = req.getRemoteAddr();
		}
		return requestHost;
	}
	
	public static <T> T body(HttpServletRequest req, Class<T> clazz)
	{
		try
		{
			return IO.readJSON(req.getInputStream(), clazz);
		} 
		catch (IOException ex) 
		{
			return null;
		}
	}
}
