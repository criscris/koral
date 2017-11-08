package xyz.koral.compute.java;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

public class SerializerTest 
{
	@Test
	public void test1() 
	{
		Test[] t = new Test[10];
		Object o = t;
		assert o instanceof Object[];
		
		int[] arr = new int[5];
		assert arr instanceof int[];
		
		List<List<String>> x = new ArrayList<>();
		
		Object z = x;
		
		assert z instanceof List<?>;
		
		
		List<String> l1 = new ArrayList<>();
		List<List<String>> l2 = new ArrayList<>();
		
		System.out.println(l1.getClass().toGenericString());
		System.out.println(l2.getClass().toGenericString());
		
		
		System.out.println(l2.getClass());
	}
}
