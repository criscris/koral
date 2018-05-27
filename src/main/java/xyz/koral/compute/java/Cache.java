package xyz.koral.compute.java;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.stream.Stream;

import xyz.koral.compute.Tuple2;
import xyz.koral.compute.config.DataFormat;

public class Cache 
{
	Map<String, Tuple2<Object, DataFormat>> uriToSource = new HashMap<>();
	
	public boolean exists(String uri)
	{
		return uriToSource.containsKey(uri);
	}
	
	public void add(String uri, Object source, DataFormat format)
	{
		// no stream (potentially too big and no common generic type (potential type matching issue)
		if (source instanceof Stream ||
		    source instanceof Iterable ||
		    source instanceof Iterator) return;
		uriToSource.put(uri, new Tuple2<>(source, format));
	}
	
	public Object value(String uri)
	{
		return uriToSource.get(uri)._1;
	}
	
	public DataFormat format(String uri)
	{
		return uriToSource.get(uri)._2;
	}
}