package xyz.koral;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class KoralIOTest extends TestCase
{
    public KoralIOTest(String testName)
    {
        super(testName);
    }

    public static Test suite()
    {
        return new TestSuite(KoralIOTest.class);
    }
    
    public void test0() throws Exception
    {
    	List<TestObject> data = new ArrayList<>();
    	
    	for (int i=0; i<1000; i++)
    	{
        	data.add(new TestObject(0.3 + i, "hallo1", "jo", "je", "ju"));
        	data.add(new TestObject(0.6 + i, "holla2", "nix", "nee"));
    	}
    	
    	exec(data);
    }
    
    public void test1() throws Exception
    {
    	List<TestObject> data = new ArrayList<>();
    	data.add(new TestObject(0.1, null, "hoi", "hu"));
    	data.add(new TestObject(Double.NaN, "nee", "hoi2", "hu2"));
    	
    	TestObject t = new TestObject();
    	t.number1 = 3.0;
    	data.add(t);
    	exec(data);
    }
    
    public void test2() throws Exception
    {
    	long time = System.currentTimeMillis();
    	List<TestObject> data = new ArrayList<>();
    	
    	for (int i=0; i<1000000; i++)
    	{
        	data.add(new TestObject(i, "a" + i, "hu"));
    	}
    	System.out.println((System.currentTimeMillis() - time) + " ms object creation.");
    	
    	exec(data);
    	System.out.println((System.currentTimeMillis() - time) + " ms total time.");
    }
    
    void exec(List<TestObject> data) throws Exception
    {
    	Path path = Files.createTempDirectory("koral");
    	QID qid = new QID("com.test");
    	try
    	{
        	File file = new File(path.toString(), "testkoral.xml");
        	long time = System.currentTimeMillis();
        	KoralIO.save(data, TestObject.class, qid, new FileOutputStream(file));
        	System.out.println((System.currentTimeMillis() - time) + " ms overall saving time.");
        	
        	Koral k = KoralIO.load(false, file);
        	k.asTable(qid, TestObject.class, k.arrayIDs(qid)).forEach(t ->
        	{
        		TestObject reference = data.get((int) (t.index));
        		compare(reference, t);
        	});
    	}
    	finally
    	{
    		KoralTest.deleteTemp(path);
    	}
    }
    
    void compare(TestObject ref, TestObject act)
    {
    	assertEquals(ref.number1, act.number1);
    	assertEquals(ref.text1, act.text1);
    	
    	if (ref.values == null) assertEquals(ref.values, act.values);
    	else
    	{
    		assertEquals(ref.values.size(), act.values.size());
    		for (int i=0; i<ref.values.size(); i++)
    		{
    			assertEquals(ref.values.get(i), act.values.get(i));
    		}
    	}
    }
}

class TestObject
{
	public long index;
	
	public double number1;
	public String text1;
	public List<String> values;
	
	public TestObject()
	{
		
	}
	
	public TestObject(double number1, String text1, String... values) 
	{
		this.number1 = number1;
		this.text1 = text1;
		this.values = new ArrayList<String>();
		for (String v : values) this.values.add(v);
	}

	public String toString() 
	{
		return "TestObject " + number1 + " " + text1 + " " + Arrays.toString(values.toArray(new String[0]));
	}	
}