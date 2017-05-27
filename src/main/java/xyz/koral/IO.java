package xyz.koral;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Serializable;
import java.io.Writer;
import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.math.BigInteger;
import java.nio.charset.Charset;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.SecureRandom;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Spliterators;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.XMLEvent;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import xyz.koral.internal.Notifier;

public interface IO 
{
	Charset cs = Charset.forName("UTF-8");
	char csvSeparator = ',';
	char quoteChar = '"';
	char escapeChar = '\\';
	
	char[] escapeChars = new char[] {csvSeparator, quoteChar, '\n', '\r'};
	String[] escaped = { "\\\"", "\\n", "\\r"};
	String[] unescaped = { "\"", "\n", "\r"};
	
	String quote = "\"";
	
	static <T> Stream<T> stream(Iterator<T> iter)
	{
		return StreamSupport.stream(Spliterators.spliteratorUnknownSize(iter, 0), false);
	}
	
	static String generateRandomToken()
	{
		SecureRandom random = new SecureRandom();
		return new BigInteger(130, random).toString(32);
	}
	
	Supplier<DecimalFormat> dfF = () -> {
		DecimalFormat df = new DecimalFormat("0", DecimalFormatSymbols.getInstance(Locale.ENGLISH));
		df.setMaximumFractionDigits(340); // == DecimalFormat.DOUBLE_FRACTION_DIGITS
		return df;
	};
	DecimalFormat df = dfF.get();
	
	static String numberToString(double value) 
	{
		String v = "" + value;
		if (v.endsWith(".0")) v = v.substring(0, v.length() - 2);
		if (v.startsWith("0.")) v = v.substring(1);
		if (v.contains("E"))
		{
	    	String v2 = df.format(value);
	    	if (v2.length() < v.length()) v = v2;
		}
		return v;
	}
	
	static double stringToNumber(String value)
	{
		if (value == null) return Double.NaN;
		value = value.trim();
		if (value.length() == 0) return 0;
		
		try
		{
			return new Double(value);
		}
		catch (NumberFormatException ex)
		{
			throw new KoralError(ex);
		}
	}
	
	static InputStream istream(File file)
	{
		try 
		{
			boolean gz = file.getName().endsWith(".gz");
			if (!gz && !file.exists())
			{
				File gzFile = new File(file.getAbsolutePath() + ".gz");
				if (gzFile.exists())
				{
					return new GZIPInputStream(new FileInputStream(gzFile));
				}
			}
			return gz ? new GZIPInputStream(new FileInputStream(file)) : new FileInputStream(file);
		} 
		catch (IOException ex) 
		{
			throw new KoralError(ex);
		}
	}
	
	static OutputStream ostream(File file)
	{
		try 
		{
			return file.getName().endsWith(".gz") ? new GZIPOutputStream(new FileOutputStream(file)) 
					: new FileOutputStream(file);
		} 
		catch (IOException ex) 
		{
			throw new KoralError(ex);
		}
	}
	
	static Stream<String> readLines(InputStream is)
	{
		return readLines(new InputStreamReader(is, cs));
	}
	
	static Stream<String> readLines(Reader reader_)
	{
		return stream(new Iterator<String>() 
		{
			BufferedReader reader = null;
			boolean closed = false;
			String line = null;
			
			public boolean hasNext() 
			{
				if (line != null) return true;
				if (closed) return false;
				
				try
				{			
					if (reader != null)
					{
						if ((line = reader.readLine()) != null) return true;
						reader.close();
						closed = true;
						return false;
					}
					else
					{
						reader = new BufferedReader(Notifier.instance().start(reader_));
						return hasNext();
					}
				
				} 
				catch (IOException ex) 
				{
					throw new KoralError(ex);
				}
			}

			public String next() 
			{
				if (line == null) throw new NoSuchElementException();
				String s = line;
				line = null;
				return s;
			}
		});
	}
	
	static void write(String text, OutputStream os)
	{
		Writer w = new BufferedWriter(new OutputStreamWriter(os, cs));
		try 
		{
			w.write(text);
			w.close();
		} 
		catch (IOException ex) 
		{
			throw new KoralError(ex);
		}
	}
	
	static void writeJSON(Object o, OutputStream os)
	{
		Gson gson = new Gson();
		write(gson.toJson(o), os);
	}
	
	static void writeLines(Stream<String> lines, OutputStream os)
	{
		writeLines(lines, new OutputStreamWriter(os, cs));
	}
	
	static void writeLines(Stream<String> lines, Writer writer_)
	{
		BufferedWriter writer = new BufferedWriter(Notifier.instance().start(writer_));
		Iterator<String> iter = lines.iterator();
		try
		{
			while (iter.hasNext())
			{
				writer.write(iter.next());
				writer.write("\n");
			}
			writer.close();
		}
		catch (IOException ex)
		{
			throw new KoralError(ex);
		}
	}
	
	static Stream<List<String>> readCSV(InputStream is)
	{
		return readCSV(new InputStreamReader(is, cs));
	}
	
	static Stream<List<String>> readCSV(Reader reader_)
	{	
		return readLines(reader_).map(line -> splitCSVLine(line));
	}
	
	static <T> Stream<T> readCSV(InputStream is, Class<T> clazz)
	{
		boolean[] isInitialized = new boolean[1];
		List<BiConsumer<String, Object>> fieldSetter = new ArrayList<>(); // for each csv column
		
		return readCSV(is)
		.peek(line -> {
			if (isInitialized[0]) return;
			isInitialized[0] = true;
			
			Map<String, Integer> nameToIndex = new HashMap<>();
			for (int i=0; i<line.size(); i++)
			{
				fieldSetter.add(null);
				String colName = line.get(i).trim().toLowerCase().replace(".", "");
				nameToIndex.put(colName, i);
			}
			
			for (Field field : clazz.getFields())
			{
				String name = field.getName().toLowerCase();
				Integer index = nameToIndex.get(name);
				if (index == null) continue;
				field.setAccessible(true);
				
				Function<Function<String, Object>, BiConsumer<String, Object>> fs = f -> {
					return (value, object) -> 
					{
						try 
						{
							field.set(object, f.apply(value));
						} 
						catch (IllegalArgumentException | IllegalAccessException ex) 
						{
							throw new KoralError(ex);
						}
					};
				};
				
				BiConsumer<String, Object> f = null;
				switch (field.getGenericType().getTypeName())
				{
				case "int": f = fs.apply(s -> new Integer(s)); break;
				case "long": f = fs.apply(s -> new Long(s)); break;
				case "double": f = fs.apply(s -> new Double(s)); break; 
				case "float": f = fs.apply(s -> new Float(s)); break;  
				case "java.lang.String": f = fs.apply(s -> s); break;
				case "boolean": f = fs.apply(s -> new Boolean(s)); break;
				}
				fieldSetter.set(index, f);
			}
		})
		.skip(1)
		.map(line -> 
		{
			try 
			{
				T o = clazz.newInstance();
				int n = Math.min(line.size(), fieldSetter.size());
				for (int i=0; i<n; i++)
				{
					BiConsumer<String, Object> f = fieldSetter.get(i);
					if (f != null) 
					{
						String value = line.get(i);
						value = value.trim();
						if (value.length() > 0) f.accept(value, o);
					}
				}
				return o;
			} 
			catch (InstantiationException | IllegalAccessException ex) 
			{
				throw new KoralError(ex);
			}
		});
	}
	
	/**
	 * XML of the form
	 * <root>
	 * 	[<entry>...</entry>]*
	 * </root>
	 * where entry maps to the JAXB annotated class of Type T.
	 */
	static <T> Stream<T> readXML(InputStream is, Class<T> clazz)
	{
		try 
		{
			JAXBContext context = JAXBContext.newInstance(clazz);
			
			Unmarshaller u = context.createUnmarshaller();
			
			XMLInputFactory factory = XMLInputFactory.newInstance(); 
			XMLEventReader reader = factory.createXMLEventReader(is);
			
			// ignore root element
			while (reader.hasNext()) 
			{
				XMLEvent c = reader.nextEvent();
				if (c.isStartElement()) break;
			}
			
			return stream(new Iterator<T>() 
			{
				boolean hasNext = false;
				
				public boolean hasNext() 
				{
					if (hasNext) return true;
					while (reader.hasNext()) 
					{
						try 
						{
							XMLEvent e = reader.peek();
							if (!e.isStartElement())
							{
								reader.nextEvent();
								continue;
							}
						} 
						catch (XMLStreamException ex) 
						{
							throw new KoralError(ex);
						}
						
						hasNext = true;
						return true;
					}
					hasNext = false;
					return false;
				}

				public T next() 
				{
					if (!hasNext && !hasNext()) throw new KoralError("no more elements.");
					try 
					{
						hasNext = false;
						return u.unmarshal(reader, clazz).getValue();
					} 
					catch (JAXBException ex) 
					{
						throw new KoralError(ex);
					}
				}
			});
		} 
		catch (JAXBException | XMLStreamException ex) 
		{
			throw new KoralError(ex);
		}
	}

	static String unescapeCSV(String value)
	{
		if (value.length() == 0) return value;
		
		int n = value.length();
		if (value.charAt(0) == quoteChar && n >= 2 && value.charAt(n - 1) == quoteChar)
		{
			value = value.substring(1, n - 1);
			for (int i=0; i<escaped.length; i++)
			{
				value = value.replace(escaped[i], unescaped[i]);
			}
		}
		
		return value;
	}
	
	static String escapeCSV(String value)
	{
		if (value == null) return null;
		boolean needsEscaping = false;
		for (int i=0; i<escapeChars.length; i++)
		{
			if (value.indexOf(escapeChars[i]) >= 0)
			{
				needsEscaping = true;
				break;
			}
		}
		
		if (needsEscaping)
		{
			for (int i=0; i<unescaped.length; i++)
			{
				value = value.replace(unescaped[i], escaped[i]);
			}
			return quote + value + quote;
		}
		
		return value;
	}
	
	static String mergeCSVLine(List<String> entries) 
	{
		StringBuilder sb = new StringBuilder();
		
		for (int i=0; i<entries.size(); i++)
		{
			String v = escapeCSV(entries.get(i));
			if (v != null) sb.append(v);
			if (i < entries.size() - 1) sb.append(csvSeparator);
		}
		
		return sb.toString();
	}
	
	static List<String> splitCSVLine(String line) 
	{
		List<String> parts = new ArrayList<String>();
		if (line.trim().length() == 0) return parts;
		
		boolean escape = false;
		int startEntry = 0;
		boolean inQuote = false;
		int index = 0;
		
		while (index < line.length())
		{
			char c = line.charAt(index);
			if (inQuote)
			{
				if (escape)
				{
					escape = false;
				}
				else
				{
					if (c == escapeChar)
					{
						escape = !escape;
					}
					else if (c == quoteChar)
					{
						inQuote = false;
					}
				}
			}
			else
			{
				if (c == csvSeparator)
				{
					parts.add(unescapeCSV(line.substring(startEntry, index)));
					startEntry = index + 1;
				}
				else if (c == quoteChar)
				{
					inQuote = true;
				}
			}
			
			index++;
		}
		parts.add(unescapeCSV(line.substring(startEntry, line.length())));

		return parts;
	}
	
	static void writeCSV(Stream<List<String>> lines, OutputStream os)
	{
		writeCSV(lines, new OutputStreamWriter(new BufferedOutputStream(os), cs));
	}
	
	static void writeCSV(Stream<List<String>> lines, Writer writer)
	{
		writeLines(lines.map(list -> mergeCSVLine(list)), writer);
	}
	
	static <T> T readJSON(InputStream is, Class<T> clazz)
	{
		return readJSON(is, (Type) clazz);
	}
	
	static <T> T readJSON(InputStream is, Type typeofT)
	{
		Gson gson = new Gson();
		T t = gson.fromJson(new InputStreamReader(is, cs), typeofT);;
		try 
		{
			is.close();
		} 
		catch (IOException ex) 
		{
			throw new KoralError(ex);
		}
		return t;
	}

	/**
	 * a json stream contains one object per line
	 **/
	static <T> Stream<T> readJSONStream(InputStream is, Class<T> clazz)
	{
		return readJSONStream(is, (Type) clazz);
	}
	
	/**
	 * a json stream contains one object per line
	 **/
	static <T> Stream<T> readJSONStream(InputStream is, Type typeofT)
	{
		Gson gson = new Gson();
		return readLines(is).map(line -> gson.fromJson(line, typeofT));
	}
	
	/**
	 * a json stream contains one object per line
	 * order of objects is not guaranteed due to parallelization
	 **/
	static <T> Stream<T> readJSONStreamParallel(InputStream is, Class<T> clazz)
	{
		return readJSONStreamParallel(is, (Type) clazz);
	}
	
	/**
	 * a json stream contains one object per line
	 * order of objects is not guaranteed due to parallelization
	 **/
	static <T> Stream<T> readJSONStreamParallel(InputStream is, Type typeofT)
	{
		Gson gson = new Gson();
		return readLines(is).parallel().map(line -> gson.fromJson(line, typeofT));
	}
	
	static void writeJSONStream(Stream<?> objects, OutputStream os)
	{
		writeJSONStream(objects, new OutputStreamWriter(os, cs));
	}
	
	static void writeJSONStream(Stream<?> objects, Writer writer)
	{
		Gson gson = new Gson();
		writeLines(objects.map(s -> gson.toJson(s)), writer);
	}
	
	static void serialize(Serializable object, OutputStream os)
	{
		os = Notifier.instance().start(new BufferedOutputStream(os));
		try
		{
			ObjectOutputStream oos = new ObjectOutputStream(os);
			oos.writeObject(object);
			oos.close();
		}
		catch (IOException ex)
		{
			throw new KoralError(ex);
		}
	}
	
	@SuppressWarnings("unchecked")
	static <T> T deserialize(InputStream is)
	{
		is = Notifier.instance().start(is);
		try
		{
			ObjectInputStream ois = new ObjectInputStream(is);
			Object data = ois.readObject();
			is.close();
			return (T) data;
		}
		catch (IOException | ClassNotFoundException ex)
		{
			throw new KoralError(ex);
		}
	}
	
	static long copy(InputStream in, OutputStream out, boolean closeInputStream, boolean closeOutputStream)
	{
		try 
		{
			long byteCount = 0;
			byte[] buffer = new byte[16384];
			int bytesRead = -1;
			while ((bytesRead = in.read(buffer)) != -1) 
			{
				out.write(buffer, 0, bytesRead);
				byteCount += bytesRead;
			}
			out.flush();
			return byteCount;
		}
		catch (IOException ex) 
		{
			throw new KoralError(ex);
		}
		finally 
		{
			try 
			{
				if (closeInputStream) in.close();
				if (closeOutputStream) out.close();
			} 
			catch (IOException ex) 
			{
				throw new KoralError(ex);
			}
		}
	}
	
	static long copy(InputStream in, OutputStream out)
	{
		return copy(in, out, true, true);
	}
	
	static boolean isWindows() 
	{
		String os = System.getProperty("os.name").toLowerCase();
		return (os.indexOf("win") >= 0);
	}
	
	static List<String> exec(File workingDirectory, String... commands)
	{
		try
		{
			boolean w = isWindows();
			
			StringBuilder sb = new StringBuilder();
			for (int i=0; i<commands.length; i++)
			{
				sb.append(commands[i]);
				if (i < commands.length - 1) sb.append(w ? " & " : "\n");
			}
			String c = sb.toString();
			
			String[] script = w ? new String[] { "cmd.exe", "/C", c, "2>&1" } : 
				new String[] {"/bin/sh", "-c", sb.toString()};
			
			Process proc = Runtime.getRuntime().exec(script, null, workingDirectory);
			List<String> lines = readLines(proc.getInputStream()).collect(Collectors.toList());
			return lines;
		}
		catch (Exception ex)
		{
			throw new KoralError(ex);
		}
	}
	
	static String listFilesJSON(File dir, String baseURI)
	{
		class FileLister
		{
			List<FileSystemEntry> files(File dir, String uri)
			{
				List<FileSystemEntry> files = new ArrayList<>();
				for (File file : dir.listFiles())
				{
					FileSystemEntry e = null;
					
					if (file.isDirectory())
					{
						DirectoryEntry d = new DirectoryEntry();
						d.files = files(file, uri + file.getName() + "/");
						e = d;
						e.name = file.getName() + "/";
					}
					else
					{
						FileEntry f = new FileEntry();
						f.size = file.length();
						f.lastModified = file.lastModified();
						e = f;
						e.name = file.getName();
					}
					
					files.add(e);
				}
				Collections.sort(files, (f1, f2) -> {
					int d = f1.type.compareTo(f2.type);
					return d == 0 ? f1.name.compareTo(f2.name) : d;
				});
				
				return files;
			}
		}
		if (baseURI == null) baseURI = "";
		if (baseURI.length() > 0 && !baseURI.endsWith("/")) baseURI += "/";
		FileLister f = new FileLister();
		List<FileSystemEntry> files = f.files(dir, baseURI);
		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		return gson.toJson(files);
	}
	
    public static void delete(File fileOrDirectory)
    {
    	try 
    	{
			Files.walkFileTree(fileOrDirectory.toPath(), new SimpleFileVisitor<Path>() 
			{
				public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException 
				{
					Files.delete(file);
					return FileVisitResult.CONTINUE;
				}

				public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException 
				{
					Files.delete(dir);
					return FileVisitResult.CONTINUE;
				}
			});
		} 
    	catch (IOException ex) 
    	{
    		throw new KoralError(ex);
		}
    }
}

class FileSystemEntry
{
	public String type;
	public String name;
	
	public FileSystemEntry(String type)
	{
		this.type = type;
	}
}

class FileEntry extends FileSystemEntry
{
	public FileEntry()
	{
		super("file");
	}
	
	public long size;
	public long lastModified;
}

class DirectoryEntry extends FileSystemEntry
{
	public DirectoryEntry()
	{
		super("directory");
	}
	
	public List<FileSystemEntry> files;
}
