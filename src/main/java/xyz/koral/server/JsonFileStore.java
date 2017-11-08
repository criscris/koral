package xyz.koral.server;

import java.io.File;

import xyz.koral.IO;

public class JsonFileStore<T> 
{
	public JsonFileStore(File file, Class<T> clazz)
	{
		this.file = file;
		this.clazz = clazz;
	}
	
	private File file;
	private Class<T> clazz;
	
	private long configLastModified;
	private T data;
	
	public synchronized T get()
	{
		if (data == null || file.lastModified() > configLastModified)
		{
			data = IO.readJSON(IO.istream(file), clazz);
			configLastModified = file.lastModified();
		}
		return data;
	}
	
	public synchronized void save()
	{
		if (data != null)
		{
			IO.writeJSON_prettyPrint(data, IO.ostream(file));
		}
	}
}
