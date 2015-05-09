package xyz.koral;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.lang.reflect.Field;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.w3c.dom.Element;

import xyz.koral.Query.Occur;
import xyz.koral.internal.ArrayInfo;
import xyz.koral.internal.ArrayStream;
import xyz.koral.internal.ArrayStreamWriter;
import xyz.koral.internal.DenseLongVector;
import xyz.koral.internal.FileSource;
import xyz.koral.internal.Index;
import xyz.koral.internal.KeyIndices;
import xyz.koral.internal.KoralError;
import xyz.koral.internal.LRUQueue;
import xyz.koral.internal.LazyLoadSparseArray;
import xyz.koral.internal.PitchIterator;
import xyz.koral.internal.SearchIndex;
import xyz.koral.internal.Source;
import xyz.koral.internal.StreamIterable;
import xyz.koral.internal.XmlDocument;

public class Koral 
{
	List<Array> arrays = new ArrayList<>();
	
	// optional filters for iterators
	long[] usedIndices;
	long indexLimitStart = 0;
	long indexLimitCount = Long.MAX_VALUE;
	
	Map<Path, SearchIndex> pathToSearchIndex = new HashMap<>();
	
	
	public Koral()
	{
		
	}
	
	public Koral(List<Array> arrays)
	{
		this.arrays = arrays;
	}
	
	private Koral(List<Array> arrays, long[] usedIndices, Map<Path, SearchIndex> pathToSearchIndex)
	{
		this.arrays = arrays;
		this.usedIndices = usedIndices;
		this.pathToSearchIndex = pathToSearchIndex;
	}
	
	public Koral(File... files) 
	{
		for (File f : files) add(f);
	}
	
	public int noOfArrays()
	{
		return arrays.size();
	}
	
	public List<String> arrayIDs(String baseNamespace)
	{
		List<String> ids = new ArrayList<String>();
		QID parent = new QID(baseNamespace);
		for (Array array : arrays)
		{
			QID child = array.qid().split(parent);
			if (child != null) ids.add(child.get());
		}
		return ids;
	}
	
	public XmlDocument add(File file)
	{
		File indexFile = new Index(file).getIndexFile();
		try 
		{
			XmlDocument xml = new XmlDocument(new FileInputStream(indexFile));
			FileSource source = new FileSource(file);
			List<LazyLoadSparseArray> arraysOfFile = add("", xml.root(), source);
			pathToSearchIndex.put(source.getFile(), new SearchIndex(source.getFile(), arraysOfFile));
			return xml;
		} 
		catch (FileNotFoundException ex) 
		{
			throw new KoralError(ex);
		}
		
	}
	
	private LazyLoadSparseArray parseDiskArray(Source source, String namespace, Element arrayElem)
	{
		String maxPitch = arrayElem.getAttribute("maxPitch");
		String stride = arrayElem.getAttribute("stride");
		String[] strideNames = null;
		if (stride == null || stride.length() < 1)
		{
			strideNames = new String[] { "" };
		}
		else
		{
			strideNames = stride.split("" + ArrayStream.strideSep);
		}
		String typeInfo = arrayElem.getAttribute("type");
		ArrayInfo meta = new ArrayInfo(new QID(namespace, arrayElem.getAttribute("id")), 
				maxPitch == null || maxPitch.length() < 1 ? 1 : new Long(maxPitch), strideNames,
				typeInfo.equals("numeric") ? Double.class : String.class);
		long count = new Long(arrayElem.getAttribute("count"));
		long sourceOffset = new Long(arrayElem.getAttribute(XmlDocument.sourceOffsetAtt));
		long noOfBytes = new Long(arrayElem.getAttribute(XmlDocument.noOfBytesAtt));
		
		KeyIndices keyIndices = new KeyIndices(arrayElem);
		keyIndices.indices.add(count);
		keyIndices.byteOffsets.add(noOfBytes);
		for (int i=0; i<keyIndices.byteOffsets.size(); i++)
		{
			keyIndices.byteOffsets.addTo(i, sourceOffset);
		}
		
		LRUQueue queue = new LRUQueue(50000000);
		LazyLoadSparseArray a = new LazyLoadSparseArray(meta, typeInfo, source, keyIndices.indices, keyIndices.byteOffsets, queue);
		return a;
	}
	
	private List<LazyLoadSparseArray> add(String namespace, Element elem, Source source)
	{
		String id = elem.getAttribute("id");
		List<LazyLoadSparseArray> list = new ArrayList<>();
		switch (elem.getTagName())
		{
		case "array": 
			LazyLoadSparseArray a = parseDiskArray(source, namespace, elem);
			add(a);
			list.add(a);
			break;
		case "koral": 
			namespace += namespace.length() > 0 && id.length() > 0 ? "." : "";
			namespace += id;
			for (Element child : XmlDocument.children(elem, "array", "koral"))
			{
				list.addAll(add(namespace, child, source));
			}
			break;
		}
		return list;
	}
	
	public Koral(Array... arrays)
	{
		add(arrays);
	}
	
	public void add(Array... arrays)
	{
		for (Array a : arrays)
		{
			this.arrays.add(a);
		}
	}

	
	public void save(OutputStream os) 
	{
		if (arrays.size() == 0) throw new KoralError("cannot save KoralResource: no array.");
		
		QID base = arrays.get(0).qid().getNamespaceQID();
		for (int i=1; i<arrays.size(); i++)
		{
			QID base1 = arrays.get(i).qid().getNamespaceQID();
			int levels = base.noOfSameLevels(base1);
			base = base.base(levels);
		}
		System.out.println("root namespace == " + base);
		
		class Intender
		{
			String intend = "    ";
			int intendLevel = 0;
			
			void up()
			{
				intendLevel++;
			}
			
			void down()
			{
				intendLevel--;
			}
			
			String get()
			{
				StringBuilder sb = new StringBuilder();
				for (int i=0; i<intendLevel; i++) sb.append(intend);
				return sb.toString();
			}
		}
		Intender intender = new Intender();
		
		
		BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os, XmlDocument.cs));
		try
		{
			writer.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
			writer.write("<koral id=\"" + base + "\" version=\"0.2\" xmlns=\"http://koral.xyz/schema\">\n");
		
			for (int i=0; i<arrays.size(); i++)
			{
				Array a = arrays.get(i);
				QID aQID = a.qid().split(base);
				String namespace = aQID.getNamespace();
				
				if (namespace.length() > 0)
				{
					intender.up();
					writer.write(intender.get());
					writer.write("<koral id=\"" + namespace + "\">\n");
					
				}
				
				intender.up();
				writer.write(intender.get());
				writer.write("<array ");
				writer.write("id=\"" + aQID.getID() + "\" ");
				
				writer.write("type=\"" + a.typeLiteral() + "\" ");
				if (! (a.strideSize() == 1 && a.strideName(0).length() == 0)) 
				{
					String s = a.strideName(0);
					for (int j=1; j<a.strideSize(); j++)
					{
						s += ArrayStream.strideSep + a.strideName(j);
					}
					writer.write("stride=\"" + s + "\" ");
				}
					
				if (a.maxPitchSize() > 1) writer.write("maxPitch=\"" + a.maxPitchSize() + "\" ");
				writer.write("count=\"" + a.size() + "\"");
				writer.write(">");
				
				a.forEach(new ArrayStreamWriter(writer, true));
				
				writer.write("</array>\n");
				intender.down();
				
				if (namespace.length() > 0)
				{
					writer.write(intender.get());
					writer.write("</koral>\n");
					intender.down();
				}
			}
			
			writer.write("</koral>");
			writer.close();
		}
		catch (IOException ex)
		{
			throw new KoralError(ex);
		}
	}
	
	
	void replace(Array oldArray, Array newArray)
	{
		int index = arrays.indexOf(oldArray);
		arrays.set(index, newArray);
	}
	
	public Koral indices(Query... queries)
	{
		return indices(null, queries);
	}
	
	public Koral indices(QID baseNamespace, Query... queries)
	{
		if (queries.length == 0) return this;
		Query root = queries.length > 1 ? new CompositeQuery(queries) : queries[0];

		class QueryArrayVerifier
		{
			Set<Path> sourceFiles = new HashSet<>();
			
			void consume(Query query)
			{
				if (query instanceof ArrayQuery)
				{
					ArrayQuery q = (ArrayQuery) query;
					
					QID qid = new QID(baseNamespace, q.getArrayID());
					Array array = asArray(qid);
					if (array == null) array = asArray(qid.parent());
					if (array == null) throw new KoralError("Array not found: " + qid);
					if (!(array instanceof LazyLoadSparseArray)) throw new KoralError("Array type is not supported for indices searching.");
					LazyLoadSparseArray sa = (LazyLoadSparseArray) array;
					sourceFiles.add(sa.getSource().getFile());
				}
				else if (query instanceof CompositeQuery)
				{
					CompositeQuery q = (CompositeQuery) query;
					for (Query subq : q.getQueries())
					{
						consume(subq);
					}
				}
			}
		}
		QueryArrayVerifier av = new QueryArrayVerifier();
		av.consume(root);
		if (av.sourceFiles.size() > 1) throw new KoralError("More than one files sources not supported for indices searching."); 
		
		SearchIndex searchIndex = pathToSearchIndex.get(new ArrayList<>(av.sourceFiles).get(0));
		
		long[] indices = searchIndex.search(root);
		Arrays.sort(indices);
		
		if (usedIndices != null)
		{
			usedIndices = logicalCombination(usedIndices, indices, root.occur);
		}
		else
		{
			usedIndices = indices;
		}
		
		return this;
	}
	
	
	/**
	 * l1 and l2 needs to be sorted and contain unique entries each.
	 */
	static long[] logicalCombination(long[] l1, long[] l2, Occur occur)
	{
		DenseLongVector result = new DenseLongVector();
		int l1Index = 0;
		int l2Index = 0;
		
		switch (occur)
		{
		case MUST: // l1 AND l2
			while (l1Index < l1.length && l2Index < l2.length)
			{
				long l1Val = l1[l1Index];
				long l2Val = l2[l2Index];
				
				if (l1Val == l2Val)
				{
					result.add(l1Val);
					l1Index++;
					l2Index++;
				}
				else if (l1Val < l2Val)
				{
					l1Index++;
				}
				else
				{
					l2Index++;
				}
			}
			break;
		case SHOULD: // l1 OR l2
			while (l1Index < l1.length && l2Index < l2.length)
			{
				long l1Val = l1[l1Index];
				long l2Val = l2[l2Index];
				
				if (l1Val == l2Val)
				{
					result.add(l1Val);
					l1Index++;
					l2Index++;
				}
				else if (l1Val < l2Val)
				{
					result.add(l1Val);
					l1Index++;
				}
				else
				{
					result.add(l2Val);
					l2Index++;
				}
			}
			// rest
			while (l1Index < l1.length)
			{
				result.add( l1[l1Index]);
				l1Index++;
			}
			while (l2Index < l2.length)
			{
				result.add(l2[l2Index]);
				l2Index++;
			}
		case NOT: // l1 NOT l2
			while (l1Index < l1.length && l2Index < l2.length)
			{
				long l1Val = l1[l1Index];
				long l2Val = l2[l2Index];
				
				if (l1Val == l2Val)
				{
					l1Index++;
					l2Index++;
				}
				else if (l1Val < l2Val)
				{
					result.add(l1Val);
					l1Index++;
				}
				else
				{
					l2Index++;
				}
			}
			// rest
			while (l1Index < l1.length)
			{
				result.add( l1[l1Index]);
				l1Index++;
			}
			break;
		}
		return result.getL(0, result.size());
	}
	
	public long maxNoOfEntries()
	{
		if (usedIndices == null) return Long.MAX_VALUE;
		return usedIndices.length;
	}
	
	public Koral limit(long fromIndex, long maxNoOfIndices)
	{
		Koral k = new Koral(arrays, usedIndices, pathToSearchIndex);
		k.indexLimitStart = fromIndex;
		k.indexLimitCount = maxNoOfIndices;
		return k;
	}
	
	public Array asArray(String qID)
	{
		return asArray(new QID(qID));
	}
	
	public Array asArray(QID qID)
	{
		for (Array a : arrays)
		{
			if (qID.equals(a.qid()))
			{
				return a;
			}
		}
		return null;
	}
	
	public <T> StreamIterable<T> asTable(Class<T> clazz, String... qualifiedIDs)
	{
		return asTable(null, clazz, qualifiedIDs);
	}
	
	private FieldSetter getFieldSetter(Field field)
	{
		FieldSetter setter = null;
		switch (field.getGenericType().getTypeName())
		{
		case "int": setter = (o, entries) -> field.setInt(o, (int) entries.get(0).getD()); break;
		case "long": setter = (o, entries) -> field.setLong(o, (long) entries.get(0).getD()); break;
		case "double": setter = (o, entries) -> field.setDouble(o, entries.get(0).getD()); break;
		case "java.lang.String": setter = (o, entries) -> field.set(o, entries.get(0).getS()); break;
		case "java.util.List<java.lang.String>": 
			setter = (o, entries) -> {
				List<String> values = new ArrayList<>();
				for (Entry e : entries) values.add(e.getS());
				field.set(o, values);
			};
			break;
		case "java.util.List<java.lang.Double>": 
			setter = (o, entries) -> {
				List<Double> values = new ArrayList<>();
				for (Entry e : entries) values.add(e.getD());
				field.set(o, values);
			};
			break;
		case "double[]":
			setter = (o, entries) -> {
				double[] values = new double[entries.size()];
				for (int i=0; i<entries.size(); i++) values[i] = entries.get(i).getD();
				field.set(o, values);
			};	
			break;
		case "java.lang.String[]": 
			setter = (o, entries) -> {
				String[] values = new String[entries.size()];
				for (int i=0; i<entries.size(); i++) values[i] = entries.get(i).getS();
				field.set(o, values);
			};	
			break;
		case "java.util.List<java.util.List<java.lang.String>>":
			setter = (o, entries) -> {
				List<List<String>> values = new ArrayList<>(entries.size());
				for (Entry e : entries)
				{
					List<String> strideValues = new ArrayList<>(e.strideSize());
					for (int i=0; i<e.strideSize(); i++) strideValues.add(e.getS(i));	
					values.add(strideValues);
				}
				field.set(o, values);
			};
			break;
		case "java.util.List<java.util.List<java.lang.Double>>":
			setter = (o, entries) -> {
				List<List<Double>> values = new ArrayList<>(entries.size());
				for (Entry e : entries)
				{
					List<Double> strideValues = new ArrayList<>(e.strideSize());
					for (int i=0; i<e.strideSize(); i++) strideValues.add(e.getD(i));	
					values.add(strideValues);
				}
				field.set(o, values);
			};
			break;				
		}
		return setter;
	}
	
	public <T> T get(String baseNamespace, Class<T> clazz, long index, String... ids)
	{
		Map<String, Array> idToArray = new HashMap<String, Array>();
		for (String qID : ids)
		{
			QID qid = new QID(baseNamespace, qID);
			Array a = asArray(qid);
			if (a != null) idToArray.put(qid.getID(), a);
		}
		if (idToArray.size() == 0) throw new KoralError("No arrays matched.");
		
		try 
		{
			T o = clazz.newInstance();
			
			for (Field field : clazz.getDeclaredFields())
			{
				Array a = idToArray.get(field.getName());
				if (a == null) continue;
				
				List<Entry> entries = a.getPitch(index);
				if (entries == null || entries.size() == 0) continue;
				
				FieldSetter setter = getFieldSetter(field);
				if (setter == null) continue;
				
				setter.set(o, entries);
			}
			
			return o;
		}
		catch (InstantiationException | IllegalAccessException ex) 
		{
			throw new KoralError(ex);
		}

	}
	
	private Map<String, Array> idToArray(String baseNamespace, String... ids)
	{
		Map<String, Array> idToArray = new HashMap<String, Array>();
		for (String qID : ids)
		{
			QID qid = new QID(baseNamespace, qID);
			Array a = asArray(qid);
			if (a != null) idToArray.put(qid.getID(), a);
		}
		if (idToArray.size() == 0) throw new KoralError("No arrays matched.");
		return idToArray;
	}
	
	public <T> StreamIterable<T> asTableFiltered(String baseNamespace, Class<T> clazz, String... ids)
	{
		if (usedIndices == null) throw new KoralError("apply indices filter before calling Koral.asTableFiltered");
		Map<String, Array> idToArray = idToArray(baseNamespace, ids);
		
		class Column
		{
			Array array;
			FieldSetter setter;
			
			public Column(Array array, FieldSetter setter) 
			{
				this.array = array;
				this.setter = setter;
			}
			
			void fill(long index, Object o)
			{
				List<Entry> entries = array.getPitch(index);
				if (entries == null) return; 
				
				try 
				{
					setter.set(o, entries);
				} 
				catch (IllegalArgumentException | IllegalAccessException ex) 
				{
					throw new KoralError(ex);
				}
			}
		}
		List<Column> cols = new ArrayList<>();
		class Indexer
		{
			Field field;	
		}
		Indexer indexer = new Indexer();
		for (Field field : clazz.getDeclaredFields())
		{
			Array a = idToArray.get(field.getName());
			if (a == null) 
			{
				if (field.getName().equals("index") && field.getGenericType().getTypeName().equals("long"))
				{
					indexer.field = field;
				}
				continue;
			}
			
			FieldSetter setter = getFieldSetter(field);
			if (setter == null) continue;
			cols.add(new Column(a, setter));
		}
		if (cols.size() == 0) throw new KoralError("no class members matched with array ids.");
		
		
		int firstIterIndex = (int) Math.max(0, Math.min(usedIndices.length, indexLimitStart));
		int lastIterIndexEx = indexLimitCount < Long.MAX_VALUE ? (int) Math.max(0, Math.min(usedIndices.length, indexLimitStart + indexLimitCount)) : usedIndices.length;
		
		return new StreamIterable<T>() 
		{
			public Iterator<T> iterator() 
			{
				return new Iterator<T>() 
				{
					int currentIndex = firstIterIndex;
					
					public boolean hasNext()
					{
						return currentIndex < lastIterIndexEx;
					}

					public T next() 
					{
						if (!hasNext()) return null;
						
						try
						{
							T o = clazz.newInstance();
							long dataIndex = usedIndices[currentIndex];
							for (Column col : cols)
							{
								col.fill(dataIndex, o);
							}
							if (indexer.field != null)
							{
								indexer.field.setLong(o, dataIndex);
							}
							
							currentIndex++;
							return o;
						}
						catch (IllegalAccessException | InstantiationException ex)
						{
							throw new KoralError(ex);
						} 
					}
				};
			}
		};
	}
	
	public <T> StreamIterable<T> asTable(String baseNamespace, Class<T> clazz, String... ids)
	{
		Map<String, Array> idToArray = idToArray(baseNamespace, ids);
		
		class Column
		{
			Iterator<Entry> iter;
			FieldSetter setter;
			
			List<Entry> entries = new ArrayList<>();
			Entry lastElement = null;
			
			public Column(Iterator<Entry> iter, FieldSetter setter) 
			{
				this.iter = iter;
				this.setter = setter;
			}
			
			long nextIndex()
			{
				if (entries.size() > 0) return entries.get(0).index();
				
				Entry e = lastElement != null ? lastElement : (iter.hasNext() ? iter.next() : null);
				if (e == null) return -1;
				entries.add(e);

				lastElement = null;
				while (iter.hasNext())
				{
					e = iter.next();
					if (e.index() == entries.get(0).index())
					{
						entries.add(e);
					}
					else
					{
						lastElement = e;
						break;
					}
				}
				
				return entries.get(0).index();
			}
			
			boolean fill(long index, Object o)
			{
				if (entries.size() == 0 || index != entries.get(0).index()) return false;
	
				try 
				{
					setter.set(o, entries);
				} 
				catch (IllegalArgumentException | IllegalAccessException ex) 
				{
					throw new KoralError(ex);
				}
				
				entries = new ArrayList<Entry>();
				return true;
			}
		}
		List<Column> cols = new ArrayList<>();
		class Indexer
		{
			Field field;	
		}
		Indexer indexer = new Indexer();
		for (Field field : clazz.getDeclaredFields())
		{
			Array a = idToArray.get(field.getName());
			if (a == null) 
			{
				if (field.getName().equals("index") && field.getGenericType().getTypeName().equals("long"))
				{
					indexer.field = field;
				}
				continue;
			}
			
			FieldSetter setter = getFieldSetter(field);
			if (setter == null) continue;
			cols.add(new Column(a.iterator(), setter));
		}
		if (cols.size() == 0) throw new KoralError("no class members matched with array ids.");

		return new StreamIterable<T>() 
		{
			public Iterator<T> iterator() 
			{
				return new Iterator<T>() 
				{
					T o = null;
					
					public boolean hasNext()
					{
						if (o != null) return true;
						
						try 
						{

							long nextMinIndex = Long.MAX_VALUE;
							for (Column col : cols)
							{
								long index = col.nextIndex();
								if (index != -1) nextMinIndex = Math.min(nextMinIndex, index);
							}
							if (nextMinIndex == Long.MAX_VALUE) return false;
							
							o = clazz.newInstance();
							for (Column col : cols)
							{
								col.fill(nextMinIndex, o);
							}
							if (indexer.field != null)
							{
								indexer.field.setLong(o, nextMinIndex);
							}
							return true;
							
						} 
						catch (InstantiationException | IllegalAccessException ex) 
						{
							throw new KoralError(ex);
						}
					}

					public T next() 
					{
						if (!hasNext()) return null;
						T result = o;
						o = null;
						return result;
					}
				};
			}
		};
	}
	
	public StreamIterable<List<List<Entry>>> asTable()
	{
		return asTable(arrays);
	}
	
	public StreamIterable<List<List<Entry>>> asTable(List<Array> arrays)
	{
		class Column
		{
			Iterator<List<Entry>> iter;
			List<Entry> last = null;
			
			public Column(Array a)
			{
				iter = new PitchIterator(a).iterator();
			}
			
			List<Entry> get()
			{
				List<Entry> result = last;
				last = null;
				return result;
			}
			
			long nextIndex()
			{
				if (last == null)
				{
					if (!iter.hasNext()) return -1;
					last = iter.next();
				}
				return last.get(0).index();
			}
		}
		List<Column> cols = new ArrayList<>();
		for (Array a : arrays) cols.add(new Column(a));
		
		return new StreamIterable<List<List<Entry>>>() 
		{
			public Iterator<List<List<Entry>>> iterator() 
			{
				return new Iterator<List<List<Entry>>>() 
				{
					List<List<Entry>> o = null;

					public boolean hasNext()
					{
						if (o != null) return true;
						
						long minIndex = Long.MAX_VALUE;
						for (Column col : cols)
						{
							long index = col.nextIndex();
							if (index == -1) continue;
							minIndex = Math.min(index, minIndex);
						}
						if (minIndex == Long.MAX_VALUE) return false;
						
						o = new ArrayList<>();
						for (Column col : cols)
						{
							o.add(col.nextIndex() == minIndex ? col.get() : null);
						}
						
						return true;
					}

					public List<List<Entry>> next() 
					{
						if (!hasNext()) return null;
						List<List<Entry>> result = o;
						o = null;
						return result;
					}
				};
			}
		};
	}
	
	public StreamIterable<Map<String, List<Entry>>> asTable(String baseNamespace, List<String> ids)
	{
		Map<String, Array> idToArray = new HashMap<String, Array>();
		for (String qID : ids)
		{
			QID qid = new QID(baseNamespace, qID);
			Array a = asArray(qid);
			if (a != null) idToArray.put(qid.getID(), a);
		}
		if (idToArray.size() == 0) throw new KoralError("No arrays matched.");
		
		class Column
		{
			String id;
			Iterator<Entry> iter;
			
			List<Entry> entries = new ArrayList<>();
			Entry lastElement = null;
			
			public Column(String id, Iterator<Entry> iter) 
			{
				this.id = id;
				this.iter = iter;
			}
			
			long nextIndex()
			{
				if (entries.size() > 0) return entries.get(0).index();
				
				Entry e = lastElement != null ? lastElement : (iter.hasNext() ? iter.next() : null);
				if (e == null) return -1;
				entries.add(e);

				lastElement = null;
				while (iter.hasNext())
				{
					e = iter.next();
					if (e.index() == entries.get(0).index())
					{
						entries.add(e);
					}
					else
					{
						lastElement = e;
						break;
					}
				}
				
				return entries.get(0).index();
			}
			
			List<Entry> get(long index)
			{
				if (entries.size() == 0 || index != entries.get(0).index()) return null;
				List<Entry> result =  entries;	
				entries = new ArrayList<Entry>();
				return result;
			}
		}
		List<Column> cols = new ArrayList<>();
		List<String> idsSorted = new ArrayList<String>(idToArray.keySet());
		Collections.sort(idsSorted);
		for (String id : idsSorted)
		{
			Array a = idToArray.get(id);
			cols.add(new Column(id, a.iterator()));
		}
		
		return new StreamIterable<Map<String, List<Entry>>>() 
		{
			public Iterator<Map<String, List<Entry>>> iterator() 
			{
				return new Iterator<Map<String, List<Entry>>>() 
				{
					Map<String, List<Entry>> o = null;
					
					public boolean hasNext()
					{
						if (o != null) return true;
						
						long nextMinIndex = Long.MAX_VALUE;
						for (Column col : cols)
						{
							long index = col.nextIndex();
							if (index != -1) nextMinIndex = Math.min(nextMinIndex, index);
						}
						if (nextMinIndex == Long.MAX_VALUE) return false;
						
						o = new HashMap<>(); 
						for (Column col : cols)
						{
							List<Entry> entries = col.get(nextMinIndex);
							if (entries != null)
							{
								o.put(col.id, entries);
							}
						}
						return true;
					}

					public Map<String, List<Entry>> next() 
					{
						if (!hasNext()) return null;
						Map<String, List<Entry>> result = o;
						o = null;
						return result;
					}
				};
			}
		};
	}
}

interface FieldSetter
{
	void set(Object o, List<Entry> entries) throws IllegalArgumentException, IllegalAccessException;
}