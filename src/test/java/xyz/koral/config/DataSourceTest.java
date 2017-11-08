package xyz.koral.config;

import org.junit.Test;

import com.google.gson.Gson;

import xyz.koral.compute.config.DataSource;

public class DataSourceTest 
{
	@Test
	public void test1() 
	{
		Gson gson = new Gson();
		DataSource d = gson.fromJson("{ 'args':{ 'param1': { 'type_':'txt'}}, 'func':'testFunc'}".replace("'", "\""), DataSource.class);
		System.out.println(gson.toJson(d));
	}
}
