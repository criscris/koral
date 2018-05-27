package xyz.koral.compute.java;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Iterator;
import java.util.Spliterators;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import xyz.koral.IO;
import xyz.koral.KoralError;
import xyz.koral.compute.config.DataFormat;
import xyz.koral.table.Table;

public class Conversion
{
	public static Supplier<Object> input(DataFormat format, Class<?> clazz, Type type, Supplier<InputStream> source, boolean parallel)
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
		case "byte[]":
			if (format != DataFormat.bin) throw new KoralError("Expected binary data. Actual specified dataformat: " + format);
			return () -> {
				ByteArrayOutputStream bos = new ByteArrayOutputStream();
				IO.copy(source.get(), bos);
				return bos.toByteArray();
			};
			
		case "xyz.koral.table.Table": 
			if (format != DataFormat.csv) throw new KoralError("Expected csv data. Actual specified dataformat: " + format);
			return () -> Table.csvToData(IO.readCSV(source.get()));
			
		case "java.util.stream.Stream": 
			Class<?> gclazz = genericClass.apply(gTypeName);
			return () -> parallel ? IO.readJSONStreamParallel(source.get(), gclazz) : IO.readJSONStream(source.get(), gclazz);
		case "java.util.List": 
		case "java.util.Collection":
			gclazz = genericClass.apply(gTypeName);
			return () -> IO.readJSONStream(source.get(), gclazz).collect(Collectors.toList());
		}
		
		// json to object
		return () -> IO.readJSON(source.get(), clazz);
	}
	
	public static void output(Object value, OutputStream os, DataFormat target)
	{
		if (value == null) return;
		
		if (target == DataFormat.func)
		{
			throw new KoralError("No function serialization supported.");
		}
		
		if (value instanceof byte[])
		{
			if (target != DataFormat.bin) throw new KoralError("Binary output expected. Actual=" + target);
			try 
			{
				os.write((byte[]) value);
				os.flush();
				os.close();
			} 
			catch (IOException ex)
			{
				throw new KoralError(ex);
			}
		}
		else if (target == DataFormat.json)
		{
			IO.writeJSON(value, os);
		}
		else if (target == DataFormat.csv)
		{
			if (value instanceof Table)
			{
				IO.writeCSV(((Table) value).toCSV(), os);
			}
			else
			{
				throw new KoralError("csv output expected. Actual" + value.getClass());
			}
		}
		else if (target == DataFormat.jsonl)
		{
			if (value instanceof Collection) value = ((Collection<?>) value).stream();
			else if (value instanceof Iterable) value = StreamSupport.stream(((Iterable<?>) value).spliterator(), false);
			else if (value instanceof Iterator) value = StreamSupport.stream(Spliterators.spliteratorUnknownSize(((Iterator<?>) value), 0), false);
			else if (value instanceof Object[]) value = Stream.of((Object[]) value);
			else if (value instanceof int[]) value = IntStream.of((int[]) value).boxed();
			else if (value instanceof double[]) value = DoubleStream.of((double[]) value).boxed();
			else if (value instanceof long[]) value = LongStream.of((long[]) value).boxed();
			
			if (value instanceof Stream)
			{
				IO.writeJSONStream((Stream<?>) value, os); 
			}
			else
			{
				throw new KoralError("Stream output expected. Actual" + value.getClass());
			}
		}
	}
}
