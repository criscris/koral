package xyz.koral.internal;

import java.io.File;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import com.google.gson.Gson;

import xyz.koral.Arg;
import xyz.koral.DataSource;
import xyz.koral.IO;
import xyz.koral.KoralError;
import xyz.koral.R;
import xyz.koral.Table;

public class RFunction implements KoralFunction 
{
	File basePath;
	String target;
	DataSource descriptor;
	
	File scriptFile;
	String functionName;
	
	public void init(File basePath, String target, DataSource descriptor) 
	{
		this.basePath = basePath;
		this.target = target;
		this.descriptor = descriptor;

		int i1 = descriptor.func.indexOf("::");
		if (i1 == -1) throw new KoralError("Invalid method reference: " + descriptor.func);
		
		String scriptFileName = descriptor.func.substring(0,  i1);
		scriptFile = new File(basePath, scriptFileName);
		if (!scriptFile.exists()) throw new KoralError("R Script file not found: " + scriptFileName);
		functionName = descriptor.func.substring(i1 + 2);
	}
	
	public String target()
	{
		return target;
	}
	
	public File basePath() 
	{
		return basePath;
	}

	public void run(Supplier<OutputStream> os, Map<String, Table> tableCache) 
	{
		R r = new R();
		Gson gson = new Gson();
		
		r.exec(IO.readLines(IO.istream(scriptFile)).collect(Collectors.joining("\n")));
		StringBuilder callString = new StringBuilder("result = " + functionName + "(");
		
		List<Entry<String, Arg>> argList = new ArrayList<>(descriptor.args.entrySet());
		// get param data into R
		for (int i=0; i<argList.size(); i++)
		{
			String name = argList.get(i).getKey();
			Arg arg = argList.get(i).getValue();
			
			if ("param".equals(arg.type))
			{
				if (arg.val instanceof String || arg.val instanceof Number)
				{
					r.exec(name + "=" + arg.val);
				}
				else
				{
					String json = gson.toJson(arg.val);
					// is.installed = function(mypkg) is.element(mypkg, installed.packages()[,1]) 
					// is.installed('jsonlite') 
					r.exec("require(jsonlite); " + name + "=fromJSON('" + json + "')");
				}
			}
			else if ("source".equals(arg.type))
			{
				String source = arg.val.toString();
				Table t = tableCache == null ? null : tableCache.get(source);
				if (t == null)
				{
					t = Table.csvToData(IO.readCSV(IO.istream(new File(basePath, source))));
				}
				r.set(name, t);
			}
			
			if (i > 0) callString.append(", ");
			callString.append(name + "=" + name);
		}
		callString.append(");");
		
		// the actual function call
		r.exec(callString.toString());
		
		Table result = r.get("result");
		r.close();
		
		if (tableCache != null) tableCache.put(target, result);
		
		if (!descriptor.nostore)
		{
			IO.writeCSV(result.toCSV(), os.get());
		}
	}
}
