package xyz.koral;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import xyz.koral.Array;
import xyz.koral.Entry;
import xyz.koral.InMemorySparseArray;
import xyz.koral.Koral;
import xyz.koral.Query.Occur;
import xyz.koral.internal.ArrayStreamWriter;
import xyz.koral.internal.XmlDocument;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class KoralTest extends TestCase
{
    public KoralTest(String testName)
    {
        super(testName);
    }

    public static Test suite()
    {
        return new TestSuite(KoralTest.class);
    }
    
    public void test0() throws Exception
    {
    	InMemorySparseArray a = new InMemorySparseArray("na", "a", Double.class);
    	a.add(1.0, 2.0, 3.0, 4.0, 5.0);
    	
    	InMemorySparseArray b = new InMemorySparseArray("na", "b", Double.class);
    	b.add(28.0, 24.0);
    	
    	Koral k = new Koral(a, b);
    	ByteArrayOutputStream bos = new ByteArrayOutputStream();
    	KoralIO.save(k, bos);
    	bos.flush();
    	String s = new String(bos.toByteArray(), XmlDocument.cs);
    	System.out.println(s);
    }
    
    public void test1() throws Exception
    {
    	Path path = Files.createTempDirectory("koral");
    	try
    	{
        	File file = new File(path.toString(), "testkoral.xml");
        	write(file, createHeader("na") + 
        		"<array id=\"a\" type=\"numeric\" stride=\"lower~upper\" maxPitch=\"100\" count=\"1002\">1.0~1|2.0~0`1~23.0|3.0~1|4.0~4|5.0~4\\1000\\88~6|^99^12~3</array>\n"
        	  + "<array id=\"b\" type=\"numeric\" count=\"4\">1000.0|2000.0|3|4.5</array>\n"
        	  + "<array id=\"c\" type=\"string\" count=\"3\">heinz|hinz\n|kunz</array>\n" + endTag);
        	
        	Koral k = KoralIO.load(file);
  
        	Array a = k.asArray("na.a");
        	
        	//a.forEach(entry -> System.out.println("e" + entry.index() + " " + entry.pitchIndex() + " " + entry.getD()));
        	
        	
        	assertEquals(3.0, a.get(2).getD());
        	assertEquals(1.0, a.get(1, 1).getD());
        	assertEquals(1002, a.size());
        	assertEquals(88.0, a.get(1000).getD());
        	assertEquals(3.0, a.get(1001, 99).getD(1));
        	
        	
        	Array c = k.asArray("na.c");
        	assertEquals(3, c.size());
        	assertEquals("kunz", c.get(2).getS());
        	
        	ByteArrayOutputStream bos = new ByteArrayOutputStream();
        	KoralIO.save(k, bos);
        	String s = new String(bos.toByteArray(), XmlDocument.cs);
        	System.out.println(s);
    	}
    	finally
    	{
    		deleteTemp(path);
    	}
    }
    
    public void test2() throws Exception
    {
    	Path path = Files.createTempDirectory("koral");
    	try
    	{
        	File file = new File(path.toString(), "testkoral.xml");
        	
        	Random rnd = new Random(123);
        	double[] test = new double[100000];
        	for (int i=0; i<test.length; i++)
        	{
        		test[i] = rnd.nextDouble();
        	}
        	
        	StringBuilder sb = new StringBuilder();
        	sb.append(test[0]);
        	for (int i=1; i<test.length; i++)
        	{
        		sb.append("|");
        		sb.append(test[i]);
        	}
        	
        	write(file, createHeader("na") + 
        		"<array id=\"a\" type=\"numeric\" count=\"" + test.length + "\">" + sb.toString() + "</array>\n"
        	  + endTag);
        	
        	Koral k = KoralIO.load(file);
  
        	Array a = k.asArray("na.a");
        	
        	//a.forEach(entry -> System.out.println("e" + entry.index() + " " + entry.pitchIndex() + " " + entry.getD()));
 
        	for (int i=0; i<test.length; i++)
        	{
        		Entry entry = a.get(i);
        		assertEquals(test[i], entry.getD());
        	}
    	}
    	finally
    	{
    		deleteTemp(path);
    	}
    }
    
    public void test2b() throws Exception
    {
    	Path path = Files.createTempDirectory("koral");
    	try
    	{
        	File file = new File(path.toString(), "testkoral.xml");
        	
        	TestData testData = ArrayTest.createRandom1(1000, 10000, 1000000, 10000);
        	StringWriter writer = new StringWriter();
        	ArrayStreamWriter w = new ArrayStreamWriter(writer);
        	testData.array.forEach(w);
        	String content = writer.toString();
        	//System.out.println(content);
        	
        	
        	write(file, createHeader("na") + 
        		"<array id=\"a\" type=\"numeric\" maxPitch=\"" + testData.rows + "\" count=\"" + testData.cols + "\">" + content + "</array>\n"
        	  + endTag);
        	
        	Koral k = KoralIO.load(file);
  
        	Array a = k.asArray("na.a");
        	
        	a.forEach(entry -> 
        	{
        		//System.out.println("e" + entry.index() + " " + entry.pitchIndex() + " " + entry.getD());
        		assertEquals(testData.expected(entry.pitchIndex(), entry.index()), entry.getD());
        	});

    	}
    	finally
    	{
    		deleteTemp(path);
    	}
    }
    
    
    final String endTag = "</koral>";
    String createHeader(String namespace)
    {
    	return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
    		"<koral id=\"" + namespace + "\" version=\"0.2\" xmlns=\"http://koral.io/schema\">\n";
    }
    
    
    
    void write(File file, String content) throws Exception
    {
    	//System.out.println("write\n" + content);
    	BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), "UTF-8"));
    	writer.write(content);
    	writer.close();
    }
    
    public static void deleteTemp(Path path) throws IOException
    {
    	Files.walkFileTree(path, new SimpleFileVisitor<Path>() 
    	{
    		public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException 
    		{
    			Files.delete(file);
    			//System.out.println(file + " deleted.");
    			return FileVisitResult.CONTINUE;
    		}

    		public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException 
    		{
    			Files.delete(dir);
    			//System.out.println(dir + " deleted.");
    			return FileVisitResult.CONTINUE;
    		}
    	});
    }
    
    public void testTable() throws Exception
    {
    	InMemorySparseArray a = new InMemorySparseArray("n", "a", Double.class);
    	a.add(1.0, 2.0, 3.0, 5.0);
    	
    	InMemorySparseArray b = new InMemorySparseArray("n", "b", Double.class);
    	b.add(1.1, 2.1, 3.1);
    	b.set(77, 0, 1.6);
    	
    	InMemorySparseArray c = new InMemorySparseArray("n", "c", String.class, 1, 3);
    	c.add("heinz", "hinz", "hunz");
    	c.set(66, 0, "ku");
    	c.set(77, 0, "neu", "nu");
    	
    	InMemorySparseArray d = new InMemorySparseArray("n", "d", Double.class, 2, 1);
    	d.add(1.1, 1.2, 2.1, 2.2, 3.1, 3.2);
    	
    	InMemorySparseArray e = new InMemorySparseArray("n", "e", String.class, 2, 2);
    	e.add("a.1", "a.2", "b.1", "b.2", "2a.1", "2a.2");
    	
    	Koral k = new Koral(a, b, c, d, e);
    	k.asTable(TestR.class, "n.a", "n.b", "n.c", "n.d", "n.e").forEach(t -> System.out.println(t));
    }
    
    public void testTableMap() throws Exception
    {
    	InMemorySparseArray a = new InMemorySparseArray("n", "a", Double.class);
    	a.add(1.0, 2.0, 3.0, 5.0);
    	
    	InMemorySparseArray b = new InMemorySparseArray("n", "b", Double.class);
    	b.add(1.1, 2.1, 3.1);
    	b.set(77, 0, 1.6);
    	
    	InMemorySparseArray c = new InMemorySparseArray("n", "c", String.class, 1, 3);
    	c.add("heinz", "hinz", "hunz");
    	c.set(66, 0, "ku");
    	c.set(77, 0, "neu", "nu");
    	
    	InMemorySparseArray d = new InMemorySparseArray("n", "d", Double.class, 2, 1);
    	d.add(1.1, 1.2, 2.1, 2.2, 3.1, 3.2);
    	
    	InMemorySparseArray e = new InMemorySparseArray("n", "e", String.class, 2, 2);
    	e.add("a.1", "a.2", "b.1", "b.2", "2a.1", "2a.2");
    	
    	Koral k = new Koral(a, b, c, d, e);
    	List<String> ids = new ArrayList<String>();
    	ids.add("a");
    	ids.add("b");
    	ids.add("c");
    	ids.add("d");
    	ids.add("e");
    	
    	k.asTable("n", ids).forEach(t -> {
    		System.out.println("--");
    		List<String> keys = new ArrayList<>(t.keySet());
    		Collections.sort(keys);
    		for (String key : keys)
    		{
    			List<Entry> entries = t.get(key);
    			System.out.print(entries.get(0).index() + " " + key);
    			for (int i=0; i<entries.size(); i++)
    			{
    				System.out.print(" " + entries.get(i).getS());
    			}
    			System.out.println();
    		}
    	});
    }
    
    public void testLogicalCombination()
    {
    	assertEquals(
    			new long[] {1}, 
    			Koral.logicalCombination(
    			new long[] {1}, 
    			new long[] {1}, Occur.MUST));
    	
    	assertEquals(
    			new long[] {}, 
    			Koral.logicalCombination(
    			new long[] {}, 
    			new long[] {}, Occur.MUST));
    	
    	assertEquals(
    			new long[] {}, 
    			Koral.logicalCombination(
    			new long[] {}, 
    			new long[] {1, 2, 3}, Occur.MUST));

    	assertEquals(
    			new long[] {3, 8, 17, 24, 25}, 
    			Koral.logicalCombination(
    			new long[] {3, 8, 17, 24, 25}, 
    			new long[] {3, 8, 17, 24, 25}, Occur.MUST));
    	
    	assertEquals(
    			new long[] {1, 4}, 
    			Koral.logicalCombination(
    			new long[] {1, 3, 4, 18, 21}, 
    			new long[] {1, 4, 5, 19, 20, 24, 25}, Occur.MUST));
    	
    	assertEquals(
    			new long[] {223}, 
    			Koral.logicalCombination(
    			new long[] {223}, 
    			new long[] {1, 4, 5, 19, 20, 24, 25, 223, 224}, Occur.MUST));
    	
    	assertEquals(
    			new long[] {1, 2}, 
    			Koral.logicalCombination(
    			new long[] {1}, 
    			new long[] {2}, Occur.SHOULD));
    	
    	assertEquals(
    			new long[] {1, 2, 8, 22, 2000, 4000, 4001, 4002}, 
    			Koral.logicalCombination(
    			new long[] {1, 8, 22, 2000, 4000}, 
    			new long[] {2, 8, 22, 2000, 4001, 4002}, Occur.SHOULD));
    	
    	assertEquals(
    			new long[] {1, 4000}, 
    			Koral.logicalCombination(
    			new long[] {1, 8, 22, 2000, 4000}, 
    			new long[] {2, 8, 22, 2000, 4001, 4002}, Occur.NOT));
    }
    
    void assertEquals(long[] expected, long[] actual)
    {
    	assertEquals(expected.length, actual.length);
    	for (int i=0; i<expected.length; i++)
    	{
    		assertEquals(expected[i], actual[i]);
    	}
    }
    
    
    public void testAsTableArray()
    {
    	InMemorySparseArray a = new InMemorySparseArray("n", "a", Double.class);
    	a.add(0, 1.0, 2.0, 3.0, 4.0);
    	
    	InMemorySparseArray b = new InMemorySparseArray("n", "b", Double.class);
    	b.add(0, 10.0, 20.0, 30.0, 40.0);
    	
    	InMemorySparseArray c = new InMemorySparseArray("n", "c", String.class);
    	c.add("a", "b", "c", "d", "e");
    	
    	Koral k = new Koral(a, b, c);
    	
    	List<List<List<Entry>>> result = new ArrayList<>();
    	k.asTable(k.arrays).forEach(arrays -> 
    	{
    		result.add(arrays);
    	});
    	assertEquals(5, result.size());
		for (int j=0; j<a.size(); j++)
		{
			assertEquals(a.get(j).getD(), result.get(j).get(0).get(0).getD());
			assertEquals(b.get(j).getD(), result.get(j).get(1).get(0).getD());
			assertEquals(c.get(j).getS(), result.get(j).get(2).get(0).getS());
		}
    }
}

class TestR
{
	long index;
	double a = Double.NaN;
	double b = Double.NaN;
	List<String> c;
	double[] d;
	List<List<String>> e;
	
	public String toString() 
	{
		StringBuilder sb = new StringBuilder();
		if (c != null)
		{
			for (String s : c) sb.append(" " + s);
		}
		String cStr = sb.toString().trim();
		
		sb = new StringBuilder();
		if (e != null)
		{
			for (int i=0; i<e.size(); i++)
			{
				sb.append("(");
				
				List<String> es = e.get(i);
				for (int j=0; j<es.size(); j++)
				{
					sb.append(es.get(j));
					if (j < es.size() - 1) sb.append(" ");
				}
				
				sb.append(") ");
			}
		}
		String eStr = sb.toString().trim();
		
		return index + " a=" + a + " b=" + b + " c=" + cStr + " d=" + (d != null ? Arrays.toString(d) : "") + " e=" + eStr;
	}
}
