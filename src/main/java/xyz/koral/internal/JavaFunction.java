package xyz.koral.internal;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.gson.Gson;

import xyz.koral.IO;
import xyz.koral.KoralError;
import xyz.koral.compute.config.DataSource;
import xyz.koral.compute.config.Param;
import xyz.koral.table.Table;

public class JavaFunction implements KoralFunction
{
	File basePath;
	String target;
	DataSource descriptor;
	
	Method method;
	List<NamedSupplier> valueSupplier;
	
	public void init(File basePath, String target, DataSource descriptor) 
	{
		this.basePath = basePath;
		this.target = target;
		this.descriptor = descriptor;
		method = staticMethod(descriptor.func); 

		Gson gson = new Gson();
		Class<?>[] params = method.getParameterTypes();
		Type[] paramsG = method.getGenericParameterTypes();
		
		Parameter[] paramNames = method.getParameters();
		valueSupplier = new ArrayList<>(params.length);
		for (int j=0; j<params.length; j++)
		{
			NamedSupplier s = null;
			String name = paramNames[j].getName();
			
			Param arg = descriptor.params.get(name);
			if (arg == null) throw new KoralError("Unspecified parameter " + name + " for target " + target);
			
			boolean isInterface = params[j].isInterface();
			if (arg.val != null && !isInterface)
			{
				String json = gson.toJson(arg.val);
				Object value = gson.fromJson(json, params[j]);
				s = new NamedSupplier(null, () -> value);
			}
			else if (arg.uri != null)
			{
				s = new NamedSupplier(arg.uri, input(params[j], paramsG[j], new File(basePath, arg.uri), arg.parallel));
			}
			
			if (s == null) throw new KoralError("Could not create a type mapping for parameter " + name + " for target " + target);
			valueSupplier.add(s);
		}
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
		try 
		{
			Object result = method.invoke(null, valueSupplier.stream().map(s -> 
			{
				Table cachedTable = tableCache == null ? null : tableCache.get(s.sourceName);
				if (cachedTable != null) return cachedTable;
				
				
				Object source = s.supplier.get();
				if (tableCache != null && source instanceof Table) tableCache.put(s.sourceName, (Table) source);
				
				return source;
			}).toArray());
			
			if (tableCache != null && result instanceof Table) tableCache.put(target, (Table) result);
			if (!descriptor.nostore) output(result, os.get());
		} 
		catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException ex)
		{
			throw new KoralError(ex);
		}
	}
	
	static Supplier<Object> input(Class<?> clazz, Type type, File source, boolean parallel)
	{
		String typeName = clazz.getTypeName();
		String gTypeName = type.getTypeName();
		
		Function<String, Class<?>> genericClass = genericTypeName -> 
		{	
			int i1 = genericTypeName.indexOf("<");
			int i2 = genericTypeName.lastIndexOf(">");
			if (i1 == -1 || i2 == -1) throw new KoralError("Unknown generic type for " + genericTypeName);
			String className = genericTypeName.substring(i1 + 1, i2);
			try 
			{
				return Class.forName(className);
			} 
			catch (ClassNotFoundException ex) 
			{
				throw new KoralError(ex);
			}
		};
		
		switch (typeName)
		{
		case "xyz.koral.table.Table": 
			return () -> Table.csvToData(IO.readCSV(IO.istream(source)));
		case "java.util.stream.Stream": 
			Class<?> gclazz = genericClass.apply(gTypeName);
			return () -> parallel ? IO.readJSONStreamParallel(IO.istream(source), gclazz) : IO.readJSONStream(IO.istream(source), gclazz);
		case "java.util.List": 
		case "java.util.Collection":
			gclazz = genericClass.apply(gTypeName);
			return () -> IO.readJSONStream(IO.istream(source), gclazz).collect(Collectors.toList());
		}
		
		// json to object
		return () -> IO.readJSON(IO.istream(source), clazz);
	}
	
	static void output(Object result, OutputStream os)
	{
		if (result instanceof Stream)
		{
			IO.writeJSONStream((Stream<?>) result, os); 
		}
		else if (result instanceof Table)
		{
			IO.writeCSV(((Table) result).toCSV(), os);
		}
		else if (result instanceof Collection)
		{
			IO.writeJSONStream(((Collection<?>) result).stream(), os);
		}
		else if (result instanceof byte[])
		{
			try 
			{
				os.write((byte[]) result);
				os.flush();
				os.close();
			} 
			catch (IOException ex)
			{
				throw new KoralError(ex);
			}
		}
		else
		{
			IO.writeJSON(result, os);
		}
	}
	
	
	
	static Method staticMethod(String methodReference)
	{
		if (methodReference == null || methodReference.trim().length() == 0) throw new KoralError("No method reference specified");
		methodReference = methodReference.trim();
		int i = methodReference.indexOf("::");
		if (i == -1) throw new KoralError("Invalid method reference: " + methodReference);
		Class<?> clazz = null;
		try 
		{
			clazz = Class.forName(methodReference.substring(0,  i));
		} 
		catch (ClassNotFoundException ex) 
		{
			throw new KoralError(ex);
		}
		
		String methodName = methodReference.substring(i + 2);
		
		List<Method> methods = new ArrayList<>();
		for (Method m : clazz.getMethods())
		{
			if (m.getName().equals(methodName) && Modifier.isStatic(m.getModifiers()) && !m.getReturnType().equals(Void.TYPE))
			{
				methods.add(m);
			}
		}
		if (methods.size() == 0) throw new KoralError("No public static non-void method '" + methodName + "' found in '" + clazz.getName() + "'");
		if (methods.size() > 1) throw new KoralError("Method reference " + methodReference + " is ambiguous due to method overloading. " + methods.size() + " methods match.");
		return methods.get(0);
	}
}

class NamedSupplier
{
	String sourceName;
	Supplier<Object> supplier;
	
	public NamedSupplier(String sourceName, Supplier<Object> supplier)
	{
		this.sourceName = sourceName;
		this.supplier = supplier;
	}
}
