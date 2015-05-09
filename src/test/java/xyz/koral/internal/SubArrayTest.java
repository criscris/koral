package xyz.koral.internal;

import java.util.ArrayList;
import java.util.List;

import xyz.koral.Entry;
import xyz.koral.InMemorySparseArray;
import xyz.koral.internal.DenseLongVector;
import xyz.koral.internal.SubArray;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class SubArrayTest extends TestCase
{
    public SubArrayTest(String testName)
    {
        super(testName);
    }

    public static Test suite()
    {
        return new TestSuite(SubArrayTest.class);
    }
    
    public void test0() throws Exception
    {
    	InMemorySparseArray a = new InMemorySparseArray("n", "a", Double.class);
    	a.add(0.0, 1.1, 2.2, 3.3, 4.4, 5.5, 6.6);
    	
    	DenseLongVector indices = new DenseLongVector();
    	indices.add(1);
    	indices.add(4);
    	indices.add(5);
    	
    	SubArray sa = new SubArray(a, indices);
    	
    	List<Entry> entries = new ArrayList<>();
    	sa.forEach(entry -> entries.add(entry));
    	
    	assertEquals(3, entries.size());
    	assertEquals(0, entries.get(0).index());
    	assertEquals(1.1, entries.get(0).getD());
    	assertEquals(1, entries.get(1).index());
    	assertEquals(4.4, entries.get(1).getD());
    	assertEquals(2, entries.get(2).index());
    	assertEquals(5.5, entries.get(2).getD());
    }
    
    public void test1() throws Exception
    {
    	InMemorySparseArray a = new InMemorySparseArray("n", "a", Double.class, 1, 22);
    	a.set(83, 4, 1.0, 2.0);
    	a.set(104, 0, 5.0);
    	a.set(104, 15, 121.0);
    	a.set(11111, 0, 8.0, 9.0, 10.0);
    	
    	DenseLongVector indices = new DenseLongVector();
    	indices.add(104);
    	indices.add(11111);
    	
    	SubArray sa = new SubArray(a, indices);
    	
    	List<Entry> entries = new ArrayList<>();
    	sa.forEach(entry -> entries.add(entry));
    	
    	assertEquals(5, entries.size());
    	
    	assertEquals(0, entries.get(0).index());
    	assertEquals(0, entries.get(0).pitchIndex());
    	assertEquals(5.0, entries.get(0).getD());
    	
    	assertEquals(0, entries.get(1).index());
    	assertEquals(15, entries.get(1).pitchIndex());
    	assertEquals(121.0, entries.get(1).getD());
    	
    	assertEquals(1, entries.get(2).index());
    	assertEquals(0, entries.get(2).pitchIndex());
    	assertEquals(8.0, entries.get(2).getD());
    	
    	assertEquals(1, entries.get(3).index());
    	assertEquals(1, entries.get(3).pitchIndex());
    	assertEquals(9.0, entries.get(3).getD());
    	
    	assertEquals(1, entries.get(4).index());
    	assertEquals(2, entries.get(4).pitchIndex());
    	assertEquals(10.0, entries.get(4).getD());
    }
}
