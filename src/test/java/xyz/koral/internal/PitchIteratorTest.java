package xyz.koral.internal;

import java.util.ArrayList;
import java.util.List;

import xyz.koral.Entry;
import xyz.koral.InMemorySparseArray;
import xyz.koral.internal.PitchIterator;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;


public class PitchIteratorTest extends TestCase
{
    public PitchIteratorTest(String testName)
    {
        super(testName);
    }

    public static Test suite()
    {
        return new TestSuite(PitchIteratorTest.class);
    }
    
    public void test0() throws Exception
    {
    	InMemorySparseArray a = new InMemorySparseArray("n", "a", Double.class, 1, 20);
    	a.set(0, 0, 1.0, 2.0, 3.0);
    	a.set(1, 0, 2.0, 2.1, 2.2, 2.2);
    	a.set(2, 0, 0.5);
    	a.set(18, 15, 16.0);
    	
    	PitchIterator iter = new PitchIterator(a);
    	List<List<Entry>> result = new ArrayList<>();
    	
    	iter.forEach(entries -> 
    	{
    		System.out.print(entries.get(0).index());
    		for (Entry entry : entries)
    		{
    			System.out.print(" " + entry.getD());
    		}
    		System.out.println();
    		result.add(entries);
    	});
    	
    	assertEquals(3, result.get(0).size());
    	assertEquals(4, result.get(1).size());
    	assertEquals(2.2, result.get(1).get(3).getD());
    	assertEquals(1, result.get(2).size());
    	assertEquals(1, result.get(3).size());
    }
}
