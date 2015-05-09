package xyz.koral.internal;

import java.util.LinkedHashSet;
import java.util.Set;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class LRUQueueTest extends TestCase
{
    public LRUQueueTest(String testName)
    {
        super(testName);
    }

    public static Test suite()
    {
        return new TestSuite(LRUQueueTest.class);
    }
    
    public void test0() throws Exception
    {
 
    	Set<String> set = new LinkedHashSet<>();
    	set.add("knirz");
    	set.add("knurz");
    	set.add("koz");
    	set.add("knirz");
    	set.remove("knurz");
    	set.add("aaa");
    	
    	for (int i=0; i<100; i++)
    	{
    		set.add("a" + i);
    	}
    	
    	for (String s : set)
    	{
    		System.out.println(s);
    	}
    }
}
