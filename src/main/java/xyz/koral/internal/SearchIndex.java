package xyz.koral.internal;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.DoubleField;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.document.LongField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.NumericRangeQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import xyz.koral.Array;
import xyz.koral.ArrayQuery;
import xyz.koral.CompositeQuery;
import xyz.koral.Entry;
import xyz.koral.Koral;

public class SearchIndex 
{
	static final String indexFieldName = "index";
	
	Path sourceFile;
	List<LazyLoadSparseArray> arrays;
	String[][] fieldNames;
	
	Searcher searcher;
	Analyzer analyzer = new StandardAnalyzer();
	
	public SearchIndex(Path sourceFile, List<LazyLoadSparseArray> arrays)
	{
		this.sourceFile = sourceFile;
		this.arrays = arrays;
		fieldNames = new String[arrays.size()][];
		for (int i=0; i<arrays.size(); i++)
		{
			LazyLoadSparseArray array = arrays.get(i);
			if (array.strideSize() == 1)
			{
				fieldNames[i] = new String[] { array.qid().get() };
			}
			else
			{
				fieldNames[i] = new String[array.strideSize()];
				for (int j=0; j<fieldNames[i].length; j++)
				{
					fieldNames[i][j] = array.qid().get() + "." + array.strideName(j);
				}
			}
		}
		
		if (!indexExists()) createIndex();
		
		try
		{
			searcher = new Searcher(getSearchIndexDir());
		}
		catch (IOException ex)
		{
			throw new KoralError(ex);
		}
	}
	
	public long[] search(xyz.koral.Query query)
	{
		class QueryConverter
		{
			Query get(xyz.koral.Query kquery)
			{
				if (kquery instanceof ArrayQuery)
				{
					ArrayQuery q = (ArrayQuery) kquery;
					String field = q.getArrayID().get();
					if (!Double.isNaN(q.getMin()) && !Double.isNaN(q.getMaxEx()))
					{
						return NumericRangeQuery.newDoubleRange(field, q.getMin(), q.getMaxEx(), true, false);
					}
					else
					{
						QueryParser parser = new QueryParser(field, analyzer);
						try 
						{
							return parser.parse(q.getTextQuery());
						} 
						catch (ParseException ex) 
						{
							throw new KoralError(ex);
						}
					}
				}
				else if (kquery instanceof CompositeQuery)
				{
					CompositeQuery q = (CompositeQuery) kquery;
					BooleanQuery lq = new BooleanQuery();
					
					
					for (xyz.koral.Query subq : q.getQueries())
					{
						Occur occur = null;
						switch (subq.getOccur())
						{
						case MUST: occur = Occur.MUST; break;
						case SHOULD: occur = Occur.SHOULD; break;
						case NOT: occur = Occur.MUST_NOT; break;
						}
						Query lsubq = get(subq);
						if (lsubq != null) lq.add(lsubq, occur);
					}
					return lq;
				}
				return null;
			}
		}
		Query lq = new QueryConverter().get(query);
		
		try 
		{
			return searcher.search(lq, indexFieldName);
		} 
		catch (ParseException | IOException ex) 
		{
			throw new KoralError(ex);
		} 
	}
	
	
	boolean indexExists()
	{
		File dir = getSearchIndexDir();
		return dir.exists() && dir.isDirectory() && dir.list().length > 0;
	}
	
	File getSearchIndexDir()
	{
		File sourceFile = this.sourceFile.toFile();
		String sourceFileName = sourceFile.getName();
		File indexDir = new File(sourceFile.getParentFile(), ".index");
		int i1 = sourceFileName.lastIndexOf(".");
		return new File(indexDir, sourceFileName.substring(0, i1) + ".inverted" + sourceFileName.substring(i1) + "/");
	}
	
	void createIndex()
	{
		File dir = getSearchIndexDir();
		try
		{
			if (dir.exists())
			{
				Files.walkFileTree(dir.toPath(), new SimpleFileVisitor<Path>() 
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
			dir.mkdirs();
			
			System.out.print("Inverted Indexing " + sourceFile + " ");
			long time = System.currentTimeMillis();
			Indexer indexer = new Indexer(dir, true);
			
			Store stored = Field.Store.NO;
			abstract class FieldCreator
			{
				abstract void create(Document doc, int arrayIndex, List<Entry> entries);
			}
			
			class DoubleFieldCreator extends FieldCreator
			{
				void create(Document doc, int arrayIndex, List<Entry> entries) 
				{
					if (entries == null) return;
					for (Entry entry : entries)
					{
						for (int i=0; i<entry.strideSize(); i++)
						{
							doc.add(new DoubleField(fieldNames[arrayIndex][i], entry.getD(i), stored));
						}
					}
				}
			}
			
			class TextFieldCreator extends FieldCreator
			{
				void create(Document doc, int arrayIndex, List<Entry> entries) 
				{
					if (entries == null) return;
					for (Entry entry : entries)
					{
						for (int i=0; i<entry.strideSize(); i++)
						{
							doc.add(new TextField(fieldNames[arrayIndex][i], entry.getS(i), stored));
						}
					}
				}
			}
			FieldCreator[] fieldCreators = new FieldCreator[arrays.size()];
			for (int i=0; i<arrays.size(); i++)
			{
				fieldCreators[i] = Number.class.isAssignableFrom(arrays.get(i).type()) ? new DoubleFieldCreator() : new TextFieldCreator();
			}
			
			
			class DocCreator implements Consumer<List<List<Entry>>>
			{	
				long maxCount;
				long count = 0;
				
				public void accept(List<List<Entry>> arrayEntries) 
				{
					Document doc = new Document();
					
					long index = -1;
					for (List<Entry> entries : arrayEntries)
					{
						if (entries != null && entries.size() > 0)
						{
							index = entries.get(0).index();
						}
					}
					doc.add(new LongField(indexFieldName, index, Field.Store.YES));
					
					for (int i=0; i<arrayEntries.size(); i++)
					{
						fieldCreators[i].create(doc, i, arrayEntries.get(i));
					}
					
					try
					{
						indexer.addDocument(doc);
					}
					catch (IOException ex)
					{
						throw new KoralError(ex);
					}
					
					count++;
					if (count % (maxCount/10) == 0) System.out.print(".");
				}

			}
			List<Array> as = new ArrayList<>(arrays);
			Koral k = new Koral(as);
			
			DocCreator dc = new DocCreator();
			for (Array a : arrays) dc.maxCount = Math.max(dc.maxCount, a.size());
			
			k.asTable(as).forEach(dc);
			
			System.out.print("c");
			indexer.close(true);
			System.out.println(". " + (System.currentTimeMillis() - time) + " ms.");
		
		}
		catch (IOException ex)
		{
			throw new KoralError(ex);
		}
	}
}


class Indexer 
{
	IndexWriter writer;

	public Indexer(File dir, boolean createNew) throws IOException 
	{
		Directory directory = FSDirectory.open(Paths.get(dir.toURI()));
		Analyzer analyzer = new StandardAnalyzer();
		IndexWriterConfig iwc = new IndexWriterConfig(analyzer);

		if (createNew) 
		{
			// Create a new index in the directory, removing any
			// previously indexed documents:
			iwc.setOpenMode(OpenMode.CREATE);
		} 
		else 
		{
			// Add new documents to an existing index:
			iwc.setOpenMode(OpenMode.CREATE_OR_APPEND);
		}

		// Optional: for better indexing performance, if you
		// are indexing many documents, increase the RAM
		// buffer. But if you do this, increase the max heap
		// size to the JVM (eg add -Xmx512m or -Xmx1g):
		//
		iwc.setRAMBufferSizeMB(1000.0);

		writer = new IndexWriter(directory, iwc);

	}

	public void addDocument(Document doc) throws IOException  
	{
		writer.addDocument(doc);
	}

	public void close(boolean noMoreWrites) throws IOException  
	{
		// NOTE: if you want to maximize search performance,
		// you can optionally call forceMerge here. This can be
		// a terribly costly operation, so generally it's only
		// worth it when your index is relatively static (ie
		// you're done adding documents to it):
		//
		if (noMoreWrites) writer.forceMerge(1);

		writer.close();
	}
}

class Searcher 
{
	IndexReader reader;
	IndexSearcher searcher;
	Analyzer analyzer;

	public Searcher(File indexDir) throws IOException
	{
		reader = DirectoryReader.open(FSDirectory.open(Paths.get(indexDir.toURI())));
		searcher = new IndexSearcher(reader);
		analyzer = new StandardAnalyzer();
	}
	
	public long[] search(Query q, String indexField) throws ParseException, IOException
	{
		//System.out.println("Start search in " + reader.numDocs() + " docs. q=" + q.getClass());
		
		long time = System.currentTimeMillis();
		TopDocs results = searcher.search(q, 1);
		//System.out.println("query " + q + " took " + (System.currentTimeMillis() - time) + " ms with " + results.totalHits + " hits.");
		
		
		if (results.totalHits == 0) return new long[0];
			
		time = System.currentTimeMillis();
		results = searcher.search(q, results.totalHits);
		long[] indices = new long[results.scoreDocs.length];
		for (int i=0; i<results.scoreDocs.length; i++)
		{
			ScoreDoc sdoc = results.scoreDocs[i];
			Document doc = searcher.doc(sdoc.doc);
			long originalIndex = new Long(doc.get(indexField));
			indices[i] = originalIndex;
		}
		return indices;
	}

	public void close() throws Exception 
	{
		reader.close();
	}
}
