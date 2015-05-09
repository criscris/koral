package xyz.koral;

import java.io.StringWriter;
import java.util.Arrays;

import xyz.koral.InMemorySparseArray;
import xyz.koral.internal.ArrayStreamReader;
import xyz.koral.internal.ArrayStreamWriter;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class ArrayStreamTest extends TestCase
{
    public ArrayStreamTest(String testName)
    {
        super(testName);
    }

    public static Test suite()
    {
        return new TestSuite(ArrayStreamTest.class);
    }
    
    public void test0() throws Exception
    {
    	InMemorySparseArray a = new InMemorySparseArray("a", "test2", Double.class, 3, 1);
    	
    	a.add(1.0, 2.0, 3.0);
    	a.add(7.0, 8.0, 9.0);
    	a.add(12.0, 12.4, 12.3, 12.2, 12.1, 12.9);
    	
    	a.set(10, 0, 5.0, 5.1, 5.2);
    	
    	StringWriter writer = new StringWriter();
    	a.forEach(new ArrayStreamWriter(writer));
    	writer.close();
    	System.out.println("Result: " + writer.toString());
    }
    
    public void test1() throws Exception
    {
    	TestData t = ArrayTest.createRandom1(200, 100, 50, 1000);
    	ArrayTest.print(t.array);
    	
    	StringWriter writer = new StringWriter();
    	t.array.forEach(new ArrayStreamWriter(writer));
    	writer.close();
    	System.out.println("Result: " + writer.toString());
    }
    
    public void test2() throws Exception
    {
    	InMemorySparseArray a = new InMemorySparseArray("a", "test2", String.class, 2, 2);
    	a.add("Shit~Shit", "Nothing^", "go away, please.", "end of game");
    	
    	StringWriter writer = new StringWriter();
    	a.forEach(new ArrayStreamWriter(writer));
    	writer.close();
    	System.out.println("Result: " + writer.toString());
    }
    
    public void test4()
    {
    	printEntries("hoi|test");
    	System.out.println("--");
    	printEntries("`|first`second`third|");
    	System.out.println("--");
    	printEntries("shi~sha`~tra~tralala~");
    	System.out.println("--");
    	
    	printEntries("test^231313323212131^test2");
    	System.out.println("--");
    	printEntries("test\\323\\ui|2\\335\\^8^~n   oi`|");
    	System.out.println("--");
    	
    	printEntries("\\11\\^18^word");
    	System.out.println("--");
    	
    	
    }
    
    public void test5()
    {
    	TestData t1 = ArrayTest.createRandom1(200, 100, 50, 1000);
    	StringWriter writer = new StringWriter();
    	t1.array.forEach(new ArrayStreamWriter(writer, false));
    	
    	String s = writer.toString();
    	
    	StringWriter writer2 = new StringWriter();
    	new ArrayStreamReader(s).forEach(new ArrayStreamWriter(writer2, false));
    	assertEquals(s, writer2.toString());
    	System.out.println(s);
    	System.out.println(writer2.toString());
    }
    
    public void printEntries(String s)
    {
    	System.out.println(s);
    	
    	StringWriter writer = new StringWriter();
    	
    	new ArrayStreamReader(s).forEach(e ->
    	System.out.println("entry " + e.index() + " " + e.pitchIndex() + " " + Arrays.toString(e.getStrideS())));
    	
    	new ArrayStreamReader(s).forEach(new ArrayStreamWriter(writer, false));
    	assertEquals(s,  writer.toString());
    }
}