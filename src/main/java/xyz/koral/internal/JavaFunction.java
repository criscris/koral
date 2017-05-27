package xyz.koral.internal;

import java.io.File;
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

import xyz.koral.Arg;
import xyz.koral.DataSource;
import xyz.koral.IO;
import xyz.koral.KoralError;
import xyz.koral.Table;

public class JavaFunction implements KoralFunction
{
	String target;
	DataSource descriptor;
	
	File outFile;
	Method method;
	List<NamedSupplier> valueSupplier;
	
	public void init(File basePath, String target, DataSource descriptor) 
	{
		this.target = target;
		this.descriptor = descriptor;
		method = staticMethod(descriptor.func); 
		outFile = new File(basePath, target);
		
		Gson gson = new Gson();
		Class<?>[] params = method.getParameterTypes();
		Type[] paramsG = method.getGenericParameterTypes();
		
		Parameter[] paramNames = method.getParameters();
		valueSupplier = new ArrayList<>(params.length);
		for (int j=0; j<params.length; j++)
		{
			NamedSupplier s = null;
			String name = paramNames[j].getName();
			
			Arg arg = descriptor.args.get(name);
			if (arg == null) throw new KoralError("Unspecified parameter " + name + " for target " + target);
			
			boolean isInterface = params[j].isInterface();
			if ("param".equals(arg.type) && !isInterface)
			{
				String json = gson.toJson(arg.val);
				Object value = gson.fromJson(json, params[j]);
				s = new NamedSupplier(null, () -> value);
			}
			else if ("source".equals(arg.type))
			{
				s = new NamedSupplier(arg.val.toString(), input(params[j], paramsG[j], new File(basePath, arg.val.toString())));
			}
			
			if (s == null) throw new KoralError("Could not create a type mapping for parameter " + name + " for target " + target);
			valueSupplier.add(s);
		}
	}
	
	public String target()
	{
		return target;
	}

	public void run(Map<String, Table> tableCache) 
	{
		outFile.getParentFile().mkdirs();
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
			if (!descriptor.nostore) output(result, outFile);
		} 
		catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException ex)
		{
			throw new KoralError(ex);
		}
	}
	
	static Supplier<Object> input(Class<?> clazz, Type type, File source)
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
		case "xyz.koral.Table": 
			return () -> Table.csvToData(IO.readCSV(IO.istream(source)));
		case "java.util.stream.Stream": 
			Class<?> gclazz = genericClass.apply(gTypeName);
			return () -> IO.readJSONStream(IO.istream(source), gclazz);
		case "java.util.List": 
		case "java.util.Collection":
			gclazz = genericClass.apply(gTypeName);
			return () -> IO.readJSONStream(IO.istream(source), gclazz).collect(Collectors.toList());
		}
		
		// json to object
		return () -> IO.readJSON(IO.istream(source), clazz);
	}
	
	static void output(Object result, File outFile)
	{
		if (result instanceof Stream)
		{
			IO.writeJSONStream((Stream<?>) result, IO.ostream(outFile)); 
		}
		else if (result instanceof Table)
		{
			IO.writeCSV(((Table) result).toCSV(), IO.ostream(outFile));
		}
		else if (result instanceof Collection)
		{
			IO.writeJSONStream(((Collection<?>) result).stream(), IO.ostream(outFile));
		}
		else
		{
			IO.writeJSON(result, IO.ostream(outFile));
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
