package xyz.koral.internal;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.LongField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.index.MultiReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.FSDirectory;

import xyz.koral.KoralTest;
import xyz.koral.internal.Indexer;

public class SearchIndexTest extends TestCase
{
    public SearchIndexTest(String testName)
    {
        super(testName);
    }

    public static Test suite()
    {
        return new TestSuite(SearchIndexTest.class);
    }
    
    void createIndex(File dir, int totalDocs, Map<String, List<String>> fieldToContent) throws Exception
    {
    	Indexer indexer = new Indexer(dir, true);
    	
	
    	for (int i=0; i<totalDocs; i++)
    	{
        	Document doc = new Document();
    		doc.add(new LongField("index", i, Field.Store.YES));
    		
    		for (String field : fieldToContent.keySet())
    		{
    			doc.add(new TextField(field, fieldToContent.get(field).get(i), Field.Store.YES));
    		}
    		
    		indexer.addDocument(doc);
    	}
    	
    	indexer.close(true);
    }
    
    public void testLuceneAppendingIndices() throws Exception
    {
    	File dir = Files.createTempDirectory("koral").toFile();
    	
    	File index1 = new File(dir, "index1");
    	File index2 = new File(dir, "index2");
    	
    	try
    	{
    		String[] texts = { "hinz", "kunz", "hanz", "hinz", "kunz hanz", "nada", "iiieh", "eight hinz", "nine", "last" };
    		Map<String, List<String>> fieldToContent = new HashMap<>();
    		fieldToContent.put("content", Arrays.stream(texts).collect(Collectors.toList()));
    		createIndex(index1, texts.length, fieldToContent);
    		
    		
    		String[] texts2 = { "schnick", "schnack", "hinz" };
    		fieldToContent = new HashMap<>();
    		fieldToContent.put("content2", Arrays.stream(texts).collect(Collectors.toList()));
    		createIndex(index2, texts2.length, fieldToContent);
    		
    		IndexReader r1 = DirectoryReader.open(FSDirectory.open(Paths.get(index1.toURI())));
    		IndexReader r2 = DirectoryReader.open(FSDirectory.open(Paths.get(index2.toURI())));
    		
    		MultiReader reader = new MultiReader(r1, r2);
    		IndexSearcher searcher =  new IndexSearcher(reader);
    		
    		
    		
    		BooleanQuery q = new BooleanQuery();
    		q.add(new TermQuery(new Term("content", "hinz")), BooleanClause.Occur.SHOULD);
    		q.add(new TermQuery(new Term("content2", "hinz")), BooleanClause.Occur.SHOULD);
    		
    		TopDocs results = searcher.search(q, 1);
    		printResults(results, searcher, q);
    		
    		reader.close();
    	}
    	finally
    	{
    		KoralTest.deleteTemp(Paths.get(dir.toURI()));
    	}
    }
    
    public void testLuceneBooleanTrees() throws Exception
    {
    	File dir = Files.createTempDirectory("koral").toFile();
    	try
    	{
    		int noOfDocs = 10;
    		Map<String, List<String>> fieldToContent = new HashMap<>();
    		
    		fieldToContent.put("field1", Arrays.stream(new String[] 
    				{"1", "1", "6", "1", "1", "1", "2", "2", "2", "3"}
    		).collect(Collectors.toList()));
    		fieldToContent.put("field2", Arrays.stream(new String[] 
    				{"a", "a", "a", "b", "b", "b", "b", "b", "b", "b"}
    		).collect(Collectors.toList()));
    		fieldToContent.put("field3", Arrays.stream(new String[] 
    				{"x", "y", "y", "x z", "y z", "z", "z", "z", "y", "last"}
    		).collect(Collectors.toList()));
    		
    		createIndex(dir, noOfDocs, fieldToContent);
    		
    		IndexReader reader = DirectoryReader.open(FSDirectory.open(Paths.get(dir.toURI())));
    		IndexSearcher searcher =  new IndexSearcher(reader);
    		
    		BooleanQuery q = new BooleanQuery();
    		BooleanQuery subq = new BooleanQuery();
    		subq.add(new TermQuery(new Term("field1", "1")), Occur.SHOULD);
    		subq.add(new TermQuery(new Term("field2", "b")), Occur.SHOULD);
    		q.add(subq, Occur.MUST);
    		q.add(new TermQuery(new Term("field3", "y")), Occur.MUST);
    		
    		
    		TopDocs results = searcher.search(q, 1);
    		printResults(results, searcher, q);
    		
    		reader.close();
    	}
    	finally
    	{
    		KoralTest.deleteTemp(Paths.get(dir.toURI()));
    	}
    }
    
    static void printResults(TopDocs results, IndexSearcher searcher, Query q) throws IOException
    {
		System.out.println(results.totalHits + " hits.");
		if (results.totalHits > 0)
		{
			results = searcher.search(q, results.totalHits);
			for (int i=0; i<results.scoreDocs.length; i++)
			{
				ScoreDoc sdoc = results.scoreDocs[i];
				Document doc = searcher.doc(sdoc.doc);
				
				System.out.print("Doc");
				for (IndexableField field : doc.getFields())
				{
					System.out.print(" " + field.name() + "=" + field.stringValue());
				}
				System.out.println();
			}
		}
    }
}
