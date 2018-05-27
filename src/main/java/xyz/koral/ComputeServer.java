package xyz.koral;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

import xyz.koral.compute.java.ComputeServlet;
import xyz.koral.server.Server;

public class ComputeServer 
{
	public static void main(String[] args)
	{
		if (args.length != 1)
		{
			System.out.println("USAGE: port");
			return;
		}
		int port = new Integer(args[0]);

		terminateOldServer(port);
		Server.serve(port,  new ComputeServlet());
	}
	
	public static void terminateOldServer(int port)
	{
		try 
		{
			((HttpURLConnection) new URL("http://127.0.0.1:" + port + "/?action=shutDown").openConnection()).getResponseCode();
			Thread.sleep(500);
		} 
		catch (IOException | InterruptedException ex) 
		{
			// there is no other instance of the server at that port
		}
	}
}
