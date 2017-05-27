package xyz.koral.internal;

import java.io.File;
import java.util.Map;

import xyz.koral.DataSource;
import xyz.koral.Table;

public interface KoralFunction
{
	void init(File basePath, String target, DataSource descriptor);
	String target();
	
	default void run()
	{
		run(null);
	}
	void run(Map<String, Table> tableCache);
}
