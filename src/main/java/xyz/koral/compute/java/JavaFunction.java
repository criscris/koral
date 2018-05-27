package xyz.koral.compute.java;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.lang.reflect.Type;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import org.apache.tomcat.util.http.fileupload.ByteArrayOutputStream;

import com.google.gson.Gson;

import xyz.koral.KoralError;
import xyz.koral.compute.config.DataSource;
import xyz.koral.compute.config.Param;

public class JavaFunction 
{
	public static void compute(String outputUri, DataSource descriptor, Cache cache, OutputStream os) 
	{
		// find method
		Method method = parseMethodReference(descriptor.func); 
		
		// input parameters
		Class<?>[] params = method.getParameterTypes();
		Type[] paramsG = method.getGenericParameterTypes();
		Parameter[] paramNames = method.getParameters();
		List<Supplier<Object>> valueSupplier = new ArrayList<>(params.length);
		Gson gson = new Gson();
		for (int j=0; j<params.length; j++)
		{
			String name = paramNames[j].getName();
			Param arg = descriptor.params.get(name);
			if (arg == null) throw new KoralError("Unspecified parameter " + name);
			
			Supplier<Object> s = null;
			if (arg.val == null && cache.exists(arg.uri))
			{
				Object val = cache.value(arg.uri);
				
				if (params[j].equals(val.getClass()) || params[j].isInstance(val))
				{
					s = () -> val;
				}
			}
			else
			{
				Supplier<InputStream> sis = null;
				
				if (cache.exists(arg.uri))
				{
					ByteArrayOutputStream bos = new ByteArrayOutputStream();
					Conversion.output(cache.value(arg.uri), bos, cache.format(arg.uri));
					sis = () -> new ByteArrayInputStream(bos.toByteArray());
				}
				else if (arg.val != null)
				{
					String val = gson.toJson(arg.val);
					sis = () -> {
						return new ByteArrayInputStream(val.getBytes(Charset.forName("UTF-8")));
					};
				}
				else
				{
					sis = () -> {
						try {
							return new URL(arg.url).openStream();
						} 
						catch (MalformedURLException ex) 
						{
							throw new KoralError(ex);
						} 
						catch (IOException ex) 
						{
							throw new KoralError(ex);
						}
					};
				}
				s = Conversion.input(arg.type, params[j], paramsG[j], sis, arg.parallel);
			}
			valueSupplier.add(s);
		}
		
		// run
		try 
		{
			Object result = method.invoke(null, valueSupplier.stream().map(s -> s.get()).toArray());
			cache.add(outputUri, result, descriptor.type);
			if (os != null) Conversion.output(result, os, descriptor.type);
		} 
		catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException ex)
		{
			throw new KoralError(ex);
		}
	}
	
	public static Method parseMethodReference(String methodReference)
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
