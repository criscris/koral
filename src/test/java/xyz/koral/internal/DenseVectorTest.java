package xyz.koral.internal;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import xyz.koral.internal.DenseBooleanVector;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class DenseVectorTest extends TestCase
{
    public DenseVectorTest(String testName)
    {
        super(testName);
    }

    public static Test suite()
    {
        return new TestSuite(DenseVectorTest.class);
    }
    
    public void testBoolean() throws Exception
    {
    	int noOfElements = 65765677;
    	boolean[] ref = new boolean[noOfElements];
    	Random rnd = new Random(76676);
    	for (int i=0; i<ref.length; i++)
    	{
    		ref[i] = rnd.nextBoolean();
    	}
    	
    	DenseBooleanVector v = new DenseBooleanVector();
    	for (int i=0; i<ref.length; i++)
    	{
    		v.add(ref[i]);
    	}
    	assertEquals(noOfElements, v.size());
    	for (int i=0; i<ref.length; i++)
    	{
    		assertEquals(ref[i], v.getB(i));
    	}
    }
    
    public void testBoolean2() throws Exception
    {
    	int noOfElements = 100000;
    	Set<Integer> ones = new HashSet<>();
    	Random rnd = new Random(76676);
    	while (ones.size() < 1000)
    	{
    		ones.add(rnd.nextInt(noOfElements));
    	}
    	
    	DenseBooleanVector v = new DenseBooleanVector();
    	for (Integer i : ones)
    	{
    		v.set(i, true);
    	}
    	
    	
    	for (int i=0; i<v.size(); i++)
    	{
    		if (v.getB(i)) assertTrue(ones.contains(i));
    	}
    }
}
