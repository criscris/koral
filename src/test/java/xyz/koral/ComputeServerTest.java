package xyz.koral;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.io.StringReader;
import java.net.ServerSocket;
import java.util.function.DoubleBinaryOperator;
import java.util.stream.IntStream;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.gson.Gson;

import xyz.koral.compute.config.DataFormat;
import xyz.koral.compute.config.DataSource;
import xyz.koral.compute.config.Param;
import xyz.koral.compute.java.ComputeServlet;
import xyz.koral.server.Server;
import xyz.koral.table.Table;

public class ComputeServerTest 
{
	Server s;
	ComputeServerAPI api;
	Gson gson = new Gson();
	
	@Before
	public void initServer() throws IOException
	{
		ServerSocket ss = new ServerSocket(0);
		int port = ss.getLocalPort();
		ss.close();
		s = new Server(port, new ComputeServlet());
		api = new ComputeServerAPI(port);
	}
		
	@After
	public void shutDownServer()
	{
		s.shutDown();
	}
	
	@Test
	public void test1() throws ClientProtocolException, IOException 
	{
		String uri = "test1.json";
		
		// nothing there yet
		HttpClient browser = HttpClientBuilder.create().build();
		HttpResponse r = api.get(uri);
		assertEquals(404, r.getStatusLine().getStatusCode());
		
		// compute
		double a = 5.0;
		double result = gson.fromJson(api.compute(uri, new DataSource("xyz.koral.ComputeServerTest::compute1", DataFormat.json).addParam("a", Param.createJson(a))), Double.class);
		assertEquals(compute1(a), result, 1e-7);
		
		// now cached
		r = api.get(uri);
		assertEquals(200, r.getStatusLine().getStatusCode());
		
		result = gson.fromJson(api.compute("test2.json", 
				new DataSource("xyz.koral.ComputeServerTest::compute1", DataFormat.json)
				.addParam("a", Param.create(DataFormat.json, uri))), Double.class);
		assertEquals(compute1(compute1(a)), result, 1e-7);
	}
	
	@Test
	public void test2() throws ClientProtocolException, IOException 
	{
		int n = 10;
		String result = api.compute("test1.csv", new DataSource("xyz.koral.ComputeServerTest::compute2", DataFormat.csv).addParam("n", Param.createJson(n)));
		assertNotNull(result);
		Table test1 = Table.csvToNumeric(IO.readCSV(new StringReader(result)));
		assertEquals(n, test1.nrowsI());
		
		int index = 3;
		result = api.compute("test2.json", new DataSource("xyz.koral.ComputeServerTest::compute3", DataFormat.json)
				.addParam("a", Param.create(DataFormat.csv, "test1.csv"))
				.addParam("index", Param.createJson(index)));
		assertEquals(test1.getD(index), new Double(result), 1e-7);
	}
	
	@Test
	public void test3() throws ClientProtocolException, IOException 
	{
		api.compute("test.func", 
				new DataSource("xyz.koral.ComputeServerTest::createModel", DataFormat.func)
				.setNoStore(true)
				.addParam("x", Param.createJson(17.0)));

		String result = api.compute("test2.json", new DataSource("xyz.koral.ComputeServerTest::computeWithModel", DataFormat.json)
				.addParam("model", Param.create(DataFormat.csv, "test.func"))
				.addParam("a", Param.createJson(2.0))
				.addParam("b", Param.createJson(3.0)));
		assertEquals(2.0*17.0+3.0, new Double(result), 1e-7);
	}
	

	
	public static double compute1(double a)
	{
		return a*2;
	}
	
	public static Table compute2(int n)
	{
		return Table.numeric(IntStream.range(0,  n)).setColNames_m("x");
	}
	
	public static double compute3(Table a, long index)
	{
		return a.getD(index);
	}
	
	public static DoubleBinaryOperator createModel(double x)
	{
		return (a, b) -> a*x + b;
	}
	
	public static double computeWithModel(double a, double b, DoubleBinaryOperator model)
	{
		return model.applyAsDouble(a,  b);
	}
}

class ComputeServerAPI
{
	public String baseUrl;
	Gson gson = new Gson();
	
	public ComputeServerAPI(int port)
	{
		baseUrl = "http://127.0.0.1:" + port + "/";
	}
	
	public void shutDown() throws IOException
	{
		try
		{
			HttpClientBuilder.create().build().execute(new HttpGet(baseUrl + "?action=shutDown"));
		}
		catch (Exception ex)
		{
			
		}
	}
	
	public HttpResponse get(String uri) throws IOException
	{
		return HttpClientBuilder.create().build().execute(new HttpGet(baseUrl + uri));
	}
	
	public String compute(String uri, DataSource ds) throws IOException
	{
		HttpPost p = new HttpPost(baseUrl + uri + "?action=compute");
		p.setEntity(new StringEntity(gson.toJson(ds), ContentType.create("application/json", "UTF-8")));
		HttpResponse r = HttpClientBuilder.create().build().execute(p);
		assertEquals(200, r.getStatusLine().getStatusCode());
		return EntityUtils.toString(r.getEntity());
	}
	
	public void addCode(String code) throws ClientProtocolException, IOException
	{
		HttpPost p = new HttpPost(baseUrl + "?action=addCode");
		p.setEntity(new StringEntity(code, ContentType.create("application/json", "UTF-8")));
		HttpResponse r = HttpClientBuilder.create().build().execute(p);
		assertEquals(200, r.getStatusLine().getStatusCode());
	}
}
