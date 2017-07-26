package xyz.koral;

import java.util.HashMap;
import java.util.Map;

public class DataSource 
{
	public boolean nostore;
	public String func;
	public String env;
	public Map<String, Arg> args;
	
	public DataSource(String func, String env)
	{
		this.func = func;
		this.env = env;
		args = new HashMap<>();
	}
	
	public DataSource(DataSource source)
	{
		nostore = source.nostore;
		func = source.func;
		env = source.env;
		args = new HashMap<>(source.args);
	}
	
	public DataSource addArg(String name, Arg arg)
	{
		args.put(name, arg);
		return this;
	}
}
