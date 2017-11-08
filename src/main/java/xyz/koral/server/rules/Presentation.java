package xyz.koral.server.rules;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import xyz.koral.server.KoralServlet.Action;
import xyz.koral.server.Method;
import xyz.koral.server.ServerUtil;

public class Presentation 
{
	SlidePresenter slidePresenter = new SlidePresenter();
	
	public Method setSlide = new Method(
		Arrays.asList(
				r -> r.action == Action.setSlide,
				Resources.isAuthor,
				Resources.fileExists), 
		(req, resp) -> 
		{
			int slide = new Integer(req.req.getParameter("slide"));
			slidePresenter.set(req.uri(), slide);
		});
	
	public Method getSlide = new Method(
		Arrays.asList(
				r -> r.action == Action.getSlide,
				Resources.isAuthor,
				Resources.fileExists), 
		(req, resp) -> 
		{
			int last = new Integer(req.req.getParameter("slide"));
			int newSlide = slidePresenter.waitForChange(req.uri(), last, 120000L);
			resp.setContentType(ServerUtil.jsonMediaType);
			resp.getWriter().append("" + newSlide).close();
		});
}

class SlidePresenter
{
	Map<String, Integer> toCurrentSlide = new HashMap<>();
	
	public void set(String url, int slideIndex)
	{
		toCurrentSlide.put(url, slideIndex);
	}
	
	public int get(String url)
	{
		Integer slide = toCurrentSlide.get(url);
		return slide == null ? 0 : slide;
	}
	
	public int waitForChange(String url, int lastIndex, long timeOutInMs)
	{
		long startTime = System.currentTimeMillis();
		do 
		{
			int newIndex = get(url);
			if (newIndex != lastIndex) return newIndex;
			try 
			{
				Thread.sleep(200);
			} 
			catch (InterruptedException e) 
			{

			}
		}
		while (System.currentTimeMillis() - startTime < timeOutInMs);
		return get(url);
	}
}
