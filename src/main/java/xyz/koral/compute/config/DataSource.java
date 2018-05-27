package xyz.koral.compute.config;

import java.util.HashMap;
import java.util.Map;

public class DataSource 
{
	public boolean nostore;
	public String func;
	public DataFormat type;
	public String env;
	public Map<String, Param> params;
	
	public DataSource()
	{
		
	}
	
	public DataSource(String func, String env)
	{
		this.func = func;
		this.env = env;
		params = new HashMap<>();
	}
	
	public DataSource(String func, DataFormat type)
	{
		this.func = func;
		this.type = type;
		params = new HashMap<>();
	}
	
	public DataSource(DataSource source)
	{
		nostore = source.nostore;
		func = source.func;
		env = source.env;
		type = source.type;
		params = new HashMap<>(source.params);
	}
	
	public DataSource addParam(String name, Param arg)
	{
		if (params == null) params = new HashMap<>();
		params.put(name, arg);
		return this;
	}
	
	public DataSource setNoStore(boolean nostore)
	{
		this.nostore = nostore;
		return this;
	}
}
