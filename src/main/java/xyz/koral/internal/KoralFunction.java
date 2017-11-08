package xyz.koral.internal;

import java.io.File;
import java.io.OutputStream;
import java.util.Map;
import java.util.function.Supplier;

import xyz.koral.IO;
import xyz.koral.compute.config.DataSource;
import xyz.koral.table.Table;

public interface KoralFunction
{
	void init(File basePath, String target, DataSource descriptor);
	File basePath();
	String target();
	
	default void run(Supplier<OutputStream> os)
	{
		run(os, null);
	}
	void run(Supplier<OutputStream> os, Map<String, Table> tableCache);
	
	default void run()
	{
		run(() -> IO.ostream(new File(basePath(), target())));
	}
}
