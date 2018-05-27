package xyz.koral;

import java.io.File;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.reflect.TypeToken;

import xyz.koral.compute.config.DataSource;
import xyz.koral.compute.config.Param;
import xyz.koral.internal.JavaFunction;
import xyz.koral.internal.KoralFunction;
import xyz.koral.internal.RFunction;
import xyz.koral.table.Table;

/**
 * 
 * javac -parameters
 *  must be used when compiling
 *  
 *  in Eclipse go to Project settings (right click, properties)
 *  Java Compiler tab.
 *  Activate "Store information about method parameters"
 *  
 */ 
public class Compute 
{
	public static void main(String... args) 
	{
		if (args.length < 1)
		{
			System.out.println("USAGE: configFile [dataSink ]*");
			return;
		}
		
		File file = new File(args[0]);
		File basePath = file.getParentFile();
		Map<String, DataSource> dataSources = load(file);
		
		String[] toCompute = args.length == 1 ? 
				getMissing(dataSources, basePath) : Arrays.copyOfRange(args, 1, args.length);
				
		compute(dataSources, basePath, false, toCompute);
	}
	
	public static Map<String, DataSource> load(File sourceFile)
	{
		return IO.readJSON(IO.istream(sourceFile), new TypeToken<Map<String, DataSource>>() { }.getType());
	}
	
	public static String[] getMissing(Map<String, DataSource> dataSources, File basePath)
	{
		return dataSources
				.entrySet()
				.stream()
				.filter(e -> !e.getValue().nostore && !new File(basePath, e.getKey()).exists() && !new File(basePath, e.getKey() + ".gz").exists())
				.map(e -> e.getKey())
				.toArray(n -> new String[n]);
	}
	
	public static void compute(Map<String, DataSource> dataSources, File basePath, boolean dryRun, String... toCompute)
	{
		if (toCompute == null || toCompute.length == 0)
		{
			System.out.println("Nothing to compute.");
			return;
		}
		
		LinkedHashMap<String, DataSource> dataSourcesToCompute = new LinkedHashMap<>();
		class SourceConsumer
		{
			public void consume(String source)
			{
				if (dataSourcesToCompute.containsKey(source)) return;
				DataSource ds = dataSources.get(source);
				if (ds == null)
				{
					throw new KoralError("DataSource " + source + " not specified.");
				}
				
				if (ds.params != null)
				{
					for (Param arg : ds.params.values())
					{
						if (arg.val == null)
						{
							if (arg.uri == null) throw new KoralError("uri for param in " + source + " not specified.");
							if (!new File(basePath, arg.uri).exists() && !new File(basePath, arg.uri + ".gz").exists())
								consume(arg.uri);
						}
					}
				}
				
				if (ds.func != null) dataSourcesToCompute.put(source, ds);
			}
		}
		SourceConsumer sc = new SourceConsumer();
		for (String source : toCompute)
		{
			sc.consume(source);
		}
		
		List<KoralFunction> tasks = new ArrayList<>();
		dataSourcesToCompute.forEach((name, info) -> 
		{
			KoralFunction func = null;
			switch (info.env.toLowerCase())
			{
			case "java": func = new JavaFunction(); break;
			case "r": func = new RFunction(); break;
			}
			if (func == null) throw new KoralError("Specified enviroment \"" + info.env + "\" not found.");
			func.init(basePath, name, info);
			tasks.add(func);
		});
		System.out.println(tasks.size() + " compute task" + (tasks.size() > 1 ? "s" : "") + ". " + Arrays.toString(tasks.stream().map(t -> t.target()).toArray()));
		
		if (dryRun || tasks.size() == 0) return;
		
		long startTime = System.currentTimeMillis();
		Map<String, Table> tableCache = new HashMap<>();
		for (int i=0; i<tasks.size(); i++)
		{
			KoralFunction task = tasks.get(i);
			long time = System.currentTimeMillis();
			task.run(() -> 
			{ 
				File outFile = new File(basePath, task.target());
				outFile.getParentFile().mkdirs();
				return IO.ostream(outFile);
			}, 
			tableCache);
			System.out.println("[Compute task " + (i+1) + " of " + tasks.size() + "] " + (System.currentTimeMillis() - time) + " ms for " + task.target());
		}
		System.out.println((System.currentTimeMillis() - startTime) + " ms total exec time.");
	}
	
	static KoralFunction init(File basePath, String name, DataSource ds)
	{
		KoralFunction func = null;
		switch (ds.env.toLowerCase())
		{
		case "java": func = new JavaFunction(); break;
		case "r": func = new RFunction(); break;
		}
		if (func == null) throw new KoralError("Specified enviroment \"" + ds.env + "\" not found.");
		func.init(basePath, name, ds);
		return func;
	}
	
	public static void compute(File basePath, String name, DataSource ds, OutputStream os)
	{
		init(basePath, name, ds).run(() -> os);
	}
	
	public static void compute(File basePath, String name, DataSource ds)
	{
		init(basePath, name, ds).run();
	}
}
