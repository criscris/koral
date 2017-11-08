package xyz.koral.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.nio.file.Files;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import xyz.koral.HTML;
import xyz.koral.IO;
import xyz.koral.server.KoralServlet;
import xyz.koral.server.Server;

public class ServerTest 
{
	int port;
	Server s;
	File configFile;
	File projectDir;
	
	@Before
	public void initServer() throws IOException
	{
		configFile = File.createTempFile("config", ".json");
		projectDir = Files.createTempDirectory("project").toFile();
		
		String config = ("{\n"
				+ "'koralClientURI':'http://koral.xyz/',\n"
				+ "'projects': {\n"
				+ "\t'project1' : {\n"
				+ "\t\t'path\":'" + projectDir.getAbsolutePath().replace("\\", "/") + "',\n"
				+ "\t\t'published':false,\n"
				+ "\t\t'tokenToAuthor': { 'abc123':'user1' }} }\n"
				+ "}").replace("'", "\"");
		System.out.println(config);
		IO.write(config, new FileOutputStream(configFile));
		
		ServerSocket ss = new ServerSocket(0);
		port = ss.getLocalPort();
		ss.close();
		s = new Server(port, new KoralServlet(configFile));
	}
		
	@After
	public void shutDownServer()
	{
		s.shutDown();
		IO.delete(configFile);
		IO.delete(projectDir);
	}
	
	
	@Test
	public void test1() throws IOException 
	{
		System.out.println("port=" + port);
		String baseURL = "http://127.0.0.1:" + port + "/project1/";
		String id = "access_token=abc123";
		
		assertEquals("[]", get(baseURL + "?action=listFiles&" + id));
		
		String csv = "x,y\n1,1\n2,4\n3,9\n";
		put(baseURL + "test.csv?action=fileCreationOrUpdate&" + id, csv, "text/csv");
		assertTrue(get(baseURL + "?action=listFiles&" + id).length() > 3);
		assertEquals(csv, get(baseURL + "test.csv?" + id));
		
		put(baseURL + "test.csv?action=rename&target=test2.csv&" + id);
		assertEquals(csv, get(baseURL + "test2.csv?" + id));
		
		assertTrue(get(baseURL + "?" + id).indexOf("<html>") != -1);
		
		put(baseURL + "testFolder?action=folderCreation&" + id);
		
		String html = HTML
				.koral("test", "http://koral.xyz/")
				.add("article", null, "class", "koral")
				.child()
				.add("h2", "This is the headline")
				.add("p", "text here.").create();
		put(baseURL + "testFolder/firstArticle.html?action=fileCreationOrUpdate&" + id, html, "text/html");
		assertEquals(html, get(baseURL + "testFolder/firstArticle.html?" + id));
		
		post(baseURL + "testFolder/firstArticle.html?action=commit&" + id);
		assertTrue(get(baseURL + "testFolder/firstArticle.html?action=history&" + id).indexOf("headline") != -1);
	}
	
	static String get(String url) throws IOException 
	{
		HttpClient browser = HttpClientBuilder.create().build();
		HttpGet g = new HttpGet(url);
		HttpResponse r = browser.execute(g);
		assertEquals(200, r.getStatusLine().getStatusCode());
		return EntityUtils.toString(r.getEntity());
	}
	
	static String put(String url) throws IOException
	{
		return put(url, null, null);
	}
	
	static String put(String url, String body, String mime) throws IOException
	{
		System.out.println("url=" + url);
		HttpClient browser = HttpClientBuilder.create().build();
		HttpPut g = new HttpPut(url);
		if (body != null) g.setEntity(new StringEntity(body, ContentType.create(mime, "UTF-8")));
		HttpResponse r = browser.execute(g);
		assertEquals(200, r.getStatusLine().getStatusCode());
		return EntityUtils.toString(r.getEntity());
	}
	
	static String post(String url) throws IOException
	{
		return post(url, null, null);
	}
	
	static String post(String url, String body,  String mime) throws IOException
	{
		HttpClient browser = HttpClientBuilder.create().build();
		HttpPost g = new HttpPost(url);
		if (body != null) g.setEntity(new StringEntity(body, ContentType.create(mime, "UTF-8")));
		HttpResponse r = browser.execute(g);
		assertEquals(200, r.getStatusLine().getStatusCode());
		return EntityUtils.toString(r.getEntity());
	}
	
	static String delete(String url) throws IOException
	{
		HttpClient browser = HttpClientBuilder.create().build();
		HttpDelete g = new HttpDelete(url);
		HttpResponse r = browser.execute(g);
		assertEquals(200, r.getStatusLine().getStatusCode());
		return EntityUtils.toString(r.getEntity());
	}
}
