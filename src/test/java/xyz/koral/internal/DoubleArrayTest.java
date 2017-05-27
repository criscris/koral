package xyz.koral.internal;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class DoubleArrayTest 
{
	@Test
	public void test1() 
	{
		for (int n : new int[] { 33, 100, 1000, 1234, 876543 }) 
		{
			t1(new GrowingDoubleArray(), n);
			t1(new GrowingBigDoubleArray(), n);
			t1(new GrowingDoubleArray(12), n);
			t1(new GrowingBigDoubleArray(0, 12, 100), n);
		}
	}
	
	static void t1(DoubleArray a, int n)
	{
		assertEquals(0, a.size());
		for (int i=0; i<n; i++)
		{
			a.add(i);
			assertEquals(i + 1, a.size());
		}
		for (int i=0; i<n; i++)
		{
			assertEquals(i, a.get(i), 1e-7);
		}
		for (int i=0; i<n*2; i++)
		{
			a.set(i, i*2);
			assertEquals(Math.max(n, i + 1), a.size());
		}
		for (int i=0; i<n*2; i++)
		{
			assertEquals(i*2, a.get(i), 1e-7);
		}
		
		double[] v = new double[n];
		for (int i=0; i<n; i++) v[i] = i*3;
		a.set(n/2, v);
		for (int i=0; i<n; i++)
		{
			assertEquals(i*3, a.get(n/2 + i), 1e-7);
		}
		
		DoubleArray a2  = a.copy();
		a.trim();
		for (int i=0; i<a2.size(); i++)
		{
			assertEquals(a.get(i), a2.get(i), 1e-7);
		}
		
		a.apply(x -> x + 1);
		for (int i=0; i<a2.size(); i++)
		{
			assertEquals(a2.get(i) + 1, a.get(i), 1e-7);
		}
		
		for (int i=0; i<a.size(); i++)
		{
			a.apply(x -> x - 1, i, 1);
		}
		for (int i=0; i<a2.size(); i++)
		{
			assertEquals(a.get(i), a2.get(i), 1e-7);
		}
		
		double[] ar = a.stream().toArray();
		for (int i=0; i<a2.size(); i++)
		{
			assertEquals(a2.get(i), ar[i], 1e-7);
		}
		
		int k = (int) Math.max(1, a.size()/10);
		for (int i=0; i<a2.size()-k; i+=k)
		{
			ar = a.stream(i, k).toArray();
			for (int j=0; j<k; j++)
			{
				double exp = a2.get(i + j);
				double act = ar[j];
				assertEquals(exp, act, 1e-7);
			}
		}
		
		DoubleArray a3 = a.copyAndApply(x -> x*3);
		for (int i=0; i<a.size(); i++)
		{
			assertEquals(a.get(i) * 3, a3.get(i), 1e-7);
		}
	}
}
