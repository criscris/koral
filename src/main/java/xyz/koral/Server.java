package xyz.koral;

import java.io.File;
import java.time.LocalDateTime;

import org.apache.catalina.Context;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.startup.Tomcat;

import xyz.koral.internal.KoralServlet;

public class Server 
{
	public static void main(String[] args) throws Exception
	{
		if (args.length != 2)
		{
			System.out.println("USAGE: port configFile");
			return;
		}
		int port = new Integer(args[0]);
		File configFile = new File(args[1]);
		if (!configFile.exists())
		{
			System.out.println("Cannot find " + configFile.getAbsolutePath());
			return;
		}
		
		Server s = new Server(port, configFile);
		Runtime.getRuntime().addShutdownHook(new Thread(() -> 
		{
			System.out.println(LocalDateTime.now() + " Server shutdown.");
			s.shutDown();
		}));
		
		s.awaitShutDown();
	}
	
	Tomcat tomcat;
	
	public Server(int port, File configFile)
	{
		tomcat = new Tomcat();
		tomcat.setPort(port);

		//Context ctx = tomcat.addContext("/koral", null);
		//Tomcat.addServlet(ctx, "s", new StaticServlet(koralClientDir));
		//ctx.addServletMappingDecoded("/*", "s"); 
		
		Context projectContext = tomcat.addContext("", null);
		Tomcat.addServlet(projectContext, "k", new KoralServlet(configFile));
		projectContext.addServletMappingDecoded("/*", "k");
		
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