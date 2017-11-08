package xyz.koral;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.util.stream.DoubleStream;

import org.apache.http.client.ClientProtocolException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.gson.Gson;

import xyz.koral.compute.config.DataFormat;
import xyz.koral.compute.config.DataSource;
import xyz.koral.compute.config.Param;
import xyz.koral.internal.DoubleVecTest;
import xyz.koral.table.Table;

public class ComputeServerRTest 
{
	ComputeServerAPI api;
	Gson gson = new Gson();
	
	@Before
	public void initServer() throws IOException, InterruptedException
	{
		File koralDir = new File(System.getProperty("koralPath"));
		File rDir = new File(System.getProperty("rPath"));
		System.out.println("koral path=" + koralDir.getAbsolutePath());
		System.out.println("R execution path=" + rDir.getAbsolutePath());
		
		
		int port = 8101;
		File rLibs = new File(koralDir, "r/libs");
		File rComputeScript = new File(koralDir, "r/computeServer.R");
		
		if (Cmd.isWindows())
		{
			new Thread(() -> Cmd.exec(rDir,  "SET R_LIBS=" + rLibs.getAbsolutePath(), "RScript.exe " + rComputeScript.getAbsolutePath() + " " + port)).start();
		}
		else
		{
			// export R_LIBS ...
			
			throw new Error("No implementation for Linux");
		}

		
		Thread.sleep(5000);
		System.out.println("Server started.");
		
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
		api.addCode("test1 = function(a, b) a*a + b");
		api.addCode("test2 = function(x, y) {\n"
				+ "	x*y\n"
				+ "}");
		api.addCode("test3 = function(s) {\n"
				+ "z = data.frame(x=c(1, 2, 3), y=c(4, 5, 6))\n"
				+ "z = z * s\n"
				+ "z\n"
				+ "}");
		
		String uri = "test.json";
		double a = 4.0;
		double b = 3.0;
		String result = api.compute(uri, 
				new DataSource("test1", DataFormat.json)
				.addParam("a", Param.createJson(a))
				.addParam("b", Param.createJson(b)));
		double r = gson.fromJson(result, Double.class);
		assertEquals(a*a + b, r, 1e-7);
			
		double x = 10.0;
		String uri2 = "test2.json";
		result = api.compute(uri2, 
				new DataSource("test2", DataFormat.json)
				.addParam("x", Param.createJson(x))
				.addParam("y", Param.create(DataFormat.json, "test.json")));
		double r2 = gson.fromJson(result, Double.class);
		assertEquals(x*r, r2, 1e-7);
		
		String uri3 = "test3.csv";
		int s = 2;
		result = api.compute(uri3, 
				new DataSource("test3", DataFormat.csv)
				.addParam("s", Param.createJson(s)));
		Table exp = Table.colBind(Table.numeric(DoubleStream.of(1, 2, 3)), Table.numeric(DoubleStream.of(4, 5, 6)));
		exp.applyD_m(v -> v*s);
		Table r3 = Table.csvToNumeric(IO.readCSV(new StringReader(result)));
		DoubleVecTest.same(exp, r3);
	}
}

class Cmd
{
	public static boolean isWindows() 
	{
		String os = System.getProperty("os.name").toLowerCase();
		return (os.indexOf("win") >= 0);
	}

	public static void exec(File workingDirectory, String... commands)
	{
		try
		{
			boolean w = isWindows();
			
			StringBuilder sb = new StringBuilder();
			for (int i=0; i<commands.length; i++)
			{
				sb.append(commands[i]);
				if (i < commands.length - 1) sb.append(w ? " & " : "\n");
			}
			String c = sb.toString();
			
			String[] script = w ? new String[] { "cmd.exe", "/C", c, "2>&1" } : 
				new String[] {"/bin/sh", "-c", sb.toString()};
			
			Process proc = Runtime.getRuntime().exec(script, null, workingDirectory);
			
			new Thread(() -> {
				IO.readLines(proc.getErrorStream()).forEach(line -> {
					System.out.println(line);
				});
			}).start();
			
			IO.readLines(proc.getInputStream()).forEach(line -> {
				System.out.println(line);
			});
		}
		catch (Exception ex)
		{
			throw new KoralError(ex);
		}
	}
}
