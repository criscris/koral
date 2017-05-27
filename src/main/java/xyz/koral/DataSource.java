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
	
	public DataSource addArg(String name, Arg arg)
	{
		args.put(name, arg);
		return this;
	}
}
