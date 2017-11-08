package xyz.koral;

import java.io.File;

import xyz.koral.server.KoralServlet;
import xyz.koral.server.Server;

public class FileServer 
{
	public static void main(String[] args)
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
	
		Server.serve(port,  new KoralServlet(configFile));
	}
}