package xyz.koral.server;

import java.time.LocalDateTime;

import javax.servlet.http.HttpServlet;

import org.apache.catalina.Context;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.startup.Tomcat;

import xyz.koral.KoralError;

public class Server 
{
	Tomcat tomcat;
	
	public static void serve(int port, HttpServlet servlet)
	{
		Server s = new Server(port, servlet);
		Runtime.getRuntime().addShutdownHook(new Thread(() -> 
		{
			s.shutDown();
		}));
		
		s.awaitShutDown();
	}
	
	public Server(int port, HttpServlet servlet)
	{
		tomcat = new Tomcat();
		tomcat.setPort(port);

		//Context ctx = tomcat.addContext("/koral", null);
		//Tomcat.addServlet(ctx, "s", new StaticServlet(koralClientDir));
		//ctx.addServletMappingDecoded("/*", "s"); 
		
		Context projectContext = tomcat.addContext("", null);
		Tomcat.addServlet(projectContext, "s", servlet);
		projectContext.addServletMappingDecoded("/*", "s");
		
		try 
		{
			tomcat.start();
		} 
		catch (LifecycleException ex) 
		{
			throw new KoralError(ex);
		}
	}
	
	public void awaitShutDown()
	{
		tomcat.getServer().await();
	}
	
	public void shutDown()
	{
		System.out.println(LocalDateTime.now() + " Server shutdown.");
		try 
		{
			tomcat.stop();
			tomcat.destroy();
		} 
		catch (LifecycleException ex) 
		{
			throw new KoralError(ex);
		}
	}
}
