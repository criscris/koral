package xyz.koral;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import xyz.koral.Entry;
import xyz.koral.InMemorySparseArray;
import xyz.koral.internal.DenseIntegerVector;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class ArrayTest extends TestCase
{
    public ArrayTest(String testName)
    {
        super(testName);
    }

    public static Test suite()
    {
        return new TestSuite(ArrayTest.class);
    }
    
    public void test0() throws Exception
    {
    	DenseIntegerVector v = new DenseIntegerVector();
    	v.add(1);
    	v.add(2);
    	v.add(3);
    	
    	assertEquals(0, v.binarySearch(1));
    }
    
    public void testPitch() throws Exception
    {
    	InMemorySparseArray a = new InMemorySparseArray("a", "test", Double.class, 1, 100);
    	a.set(0, 0, 1.0, 2.0, 3.0);
    	a.set(1, 0, 0.0);
    	a.set(1, 50, 57.0);
    	a.set(1,  73, 12.0);
    	a.set(1000, 10, 0.0, 1.0, 2.0, 3.0);
    	
    	List<Entry> entries = a.getPitch(1);
    	assertEquals(3, entries.size());
    	compareEntry(entries.get(0), 1, 0, 0.0);
    	compareEntry(entries.get(1), 1, 50, 57.0);
    	compareEntry(entries.get(2), 1, 73, 12.0);
    }
    
    void compareEntry(Entry actual, long expectedIndex, long expectedPitchIndex, double expectedValue)
    {
    	assertEquals(expectedIndex, actual.index());
    	assertEquals(expectedPitchIndex, actual.pitchIndex());
    	assertEquals(expectedValue, actual.getD());
    }
    
    public void test1() throws Exception
    {
    	InMemorySparseArray a = new InMemorySparseArray("a", "test", Double.class);
    	
    	a.add(3.0);
    	a.add(4.0);
    	a.add(5.0);
    	
    	
    	assertEquals(3.0, a.get(0).getD());
    	assertEquals(4.0, a.get(1).getD());
    	assertEquals(5.0, a.get(2).getD());
    	
    	
    	a = new InMemorySparseArray("a", "test2", Double.class, 3, 1);
    	
    	a.add(1.0, 2.0, 3.0);
    	a.add(7.0, 8.0, 9.0);
    	a.add(12.0, 12.4, 12.3, 12.2, 12.1, 12.9);
    	
    	a.set(10, 0, 5.0, 5.1, 5.2);
    	

    	double[] v = a.get(0).getStrideD();
    	assertEquals(1.0, v[0]);
    	assertEquals(2.0, v[1]);
    	assertEquals(3.0, v[2]);
    	v = a.get(1).getStrideD();
    	assertEquals(7.0, v[0]);
    	assertEquals(8.0, v[1]);
    	assertEquals(9.0, v[2]);
    	assertEquals(11, a.size());
    	
    	
    	a = new InMemorySparseArray("a", "test3", Double.class, 1, 3);
    	
    	a.add(1.0);
    	a.add(2.0);
    	a.add(3.0);
    	
    	a.add(4.0);
    	a.add(5.0);
    	a.add(6.0);
    	
    	assertEquals(3, a.pitchSize(1));
    	assertEquals(5.0, a.get(1, 1).getD(0));
    	
    	print(a);
    	
    	System.out.println(a.memorySize() + " bytes in memory.");
    }
    
    public void test2() throws Exception
    {
    	TestData t = createRandom1(200, 100, 50, 1000);
    	print(t.array);
	
    	
    	for (int x=0; x<t.cols; x++)
    	{
    		for (int y=0; y<t.rows; y++)
    		{
    			double v = t.expected(y, x);
    			if (v != 0.0) assertEquals(v, t.array.get(x, y).getD());
    		}
    	}
    }
    
    public void test2str() throws Exception
    {
    	int rows = 200;
    	int cols = 100;
    	String[] testExpected = new String[rows*cols];
    	Random rnd = new Random(1234);
    	
    	int currentIndex = 0;
    	for (int i=0; i<50; i++)
    	{
    		currentIndex += rnd.nextInt(1000);
    		testExpected[currentIndex % testExpected.length] = "" + (char) (33 + rnd.nextInt(64));
    	}
    	
    	InMemorySparseArray array = new InMemorySparseArray("a", "test3", String.class, 1, rows);
    	for (int x=0; x<cols; x++)
    	{
    		for (int y=0; y<rows; y++)
    		{
    			String v = testExpected[x*rows + y];
    			if (v != null) array.set(x, y, v);
    		}
    	}
    	print(array);
    	
    	for (int x=0; x<cols; x++)
    	{
    		for (int y=0; y<rows; y++)
    		{
    			String v = testExpected[x*rows + y];
    			if (v != null) 
    			{
    				assertEquals(v, array.get(x, y).getS());
    			}
    		}
    	}
    }
    
    static void print(InMemorySparseArray a)
    {
    	DenseIntegerVector cols = (DenseIntegerVector) a.cols;
    	System.out.print("c ");
    	for (int i=0; i<cols.size(); i++)
    	{
    		System.out.print("\t" + cols.getI(i));
    	}
    	System.out.println();
    	
    	DenseIntegerVector pitchOffsets = (DenseIntegerVector) a.pitchOffsets;
    	System.out.print("po");
    	for (int i=0; i<pitchOffsets.size(); i++)
    	{
    		System.out.print("\t" + pitchOffsets.getI(i));
    	}
    	System.out.println();
    	
    	DenseIntegerVector pitches = (DenseIntegerVector) a.pitches;
    	System.out.print("p ");
    	for (int i=0; i<pitches.size(); i++)
    	{
    		System.out.print("\t" + pitches.getI(i));
    	}
    	System.out.println();
    	
 
    	System.out.print("d");
    	for (int i=0; i<a.values.size(); i++)
    	{
    		System.out.print(" " + a.values.getS(i));
    	}
    	System.out.println();
    }
    
    static TestData createRandom1(int rows, int cols, int entries, int maxStep)
    {
    	TestData t = new TestData();
    	
    	t.rows = rows;
    	t.cols = cols;
    	t.expected = new double[t.rows*t.cols];
    	Random rnd = new Random(1234);
    	
    	long currentIndex = 0;
    	for (int i=0; i<entries; i++)
    	{
    		currentIndex += rnd.nextInt(maxStep);
    		t.expected[(int) (currentIndex % t.expected.length)] = 1.0 + rnd.nextDouble();
    	}
    	
    	t.array = new InMemorySparseArray("a", "test3", Double.class, 1, t.rows);
    	for (int x=0; x<t.cols; x++)
    	{
    		for (int y=0; y<t.rows; y++)
    		{
    			double v = t.expected(y, x);
    			if (v != 0.0) 
    			{
    				//System.out.println("e" + x + " " + y + " " + v);
    				t.array.set(x, y, v);
    			}
    		}
    	}
    	
    	return t;
    }
    
    public void test3a() throws Exception
    {
    	InMemorySparseArray a = new InMemorySparseArray("n", "a", Double.class, 1, 10);
    	a.set(10, 4, 34);
    	a.set(10, 6, 8.0);
    	a.set(22, 0, 1, 2, 3);
    	a.set(23, 3, 0.8);
    	
    	List<Entry> entries = new ArrayList<>();
    	a.forEach(entry -> entries.add(entry));
    	
    	assertEquals(34.0, entries.get(0).getD());
    	assertEquals(10, entries.get(0).index());
    	assertEquals(4, entries.get(0).pitchIndex());
    	
    	assertEquals(8.0, entries.get(1).getD());
    	assertEquals(10, entries.get(1).index());
    	assertEquals(6, entries.get(1).pitchIndex());
    	
    	assertEquals(1.0, entries.get(2).getD());
    	assertEquals(22, entries.get(2).index());
    	assertEquals(0, entries.get(2).pitchIndex());
    	
    	assertEquals(2.0, entries.get(3).getD());
    	assertEquals(22, entries.get(3).index());
    	assertEquals(1, entries.get(3).pitchIndex());
    	
    	assertEquals(3.0, entries.get(4).getD());
    	assertEquals(22, entries.get(4).index());
    	assertEquals(2, entries.get(4).pitchIndex());
    	
    	assertEquals(0.8, entries.get(5).getD());
    	assertEquals(23, entries.get(5).index());
    	assertEquals(3, entries.get(5).pitchIndex());
    	
    	
    }
    
    public void test3() throws Exception
    {
    	TestData t = createRandom1(200, 100, 50, 1000);
    	print(t.array);
    	
    	t.array.forEach(e -> {
    		//System.out.println(e.index() + " " + e.pitchIndex() + " " + e.getD());
    		assertEquals(t.expected((int) e.pitchIndex(), (int) e.index()), e.getD());
    	});
    	
    	System.out.println("count=" + t.array.stream().filter(e -> e.getD() == 1.2597899429702417).count());
    }
}

class TestData
{
	int rows;
	int cols;
	double[] expected;
	
	double expected(long row, long col)
	{
		return expected[(int) (col*rows + row)]; 
	}
	
	InMemorySparseArray array;
}