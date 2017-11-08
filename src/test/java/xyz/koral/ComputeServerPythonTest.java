package xyz.koral;

import java.io.File;
import java.io.IOException;

import org.apache.http.client.ClientProtocolException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.gson.Gson;

public class ComputeServerPythonTest 
{
	ComputeServerAPI api;
	Gson gson = new Gson();
	
	@Before
	public void initServer() throws IOException, InterruptedException
	{
		File koralDir = new File(System.getProperty("koralPath"));
		File pythonDir = new File(System.getProperty("pythonPath"));
		System.out.println("koral path=" + koralDir.getAbsolutePath());
		System.out.println("python execution path=" + pythonDir.getAbsolutePath());
		
		int port = 8103;
		File pythonComputeScript = new File(koralDir, "python/computeserver.py");
		
		if (Cmd.isWindows())
		{
			//new Thread(() -> Cmd.exec(pythonDir, new File(pythonDir, "python.exe").getAbsolutePath() + " " + pythonComputeScript.getAbsolutePath() + " " + port)).start();
			
			Process proc = Runtime.getRuntime().exec(new File(pythonDir, "python.exe").getAbsolutePath() + " " + pythonComputeScript.getAbsolutePath() + " " + port, null, null);
			System.out.println("after.");
			
			new Thread(() -> {
				IO.readLines(proc.getErrorStream()).forEach(line -> {
					System.out.println(line);
				});
			}).start();
			
			new Thread(() -> {
				IO.readLines(proc.getInputStream()).forEach(line -> {
					System.out.println(line);
				});
			}).start();
			
			Thread.sleep(5000);
			System.out.println("Server started. " + proc.isAlive());
		}
		else
		{
			throw new Error("No implementation for Linux");
		}
		
		api = new ComputeServerAPI(port);
	}
		
	@After
	public void shutDownServer() throws ClientProtocolException, IOException
	{
		api.shutDown();
	}
	
	@Test
	public void test1() throws ClientProtocolException, IOException 
	{
		api.addCode("def test1(a, b):\n"
				+ "\treturn a*a + b\n");
	}
}
