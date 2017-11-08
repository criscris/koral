package xyz.koral.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.LongStream;

import org.junit.Test;

import xyz.koral.table.Table;
import xyz.koral.table.impl.DoubleMat;
import xyz.koral.table.impl.DoubleVec;

public class DoubleVecTest 
{

	@Test
	public void test() 
	{
		singleCol(new DoubleVec(), 15, 123);
		singleCol(new DoubleVec(), 15, 123);
		singleCol(new DoubleVec(), 101, 123);
		singleCol(new DoubleVec(), 1000, 765);
		singleCol(new DoubleVec(), 121212, 123);
		singleCol(new DoubleMat(15, 1), 15, 123);
		singleCol(new DoubleMat(100, 1), 100, 123);
		singleCol(new DoubleMat(1001, 1), 1001, 123);
		singleCol(new DoubleMat(121212, 1), 121212, 123);
	}
	
	public static void same(Table t1, Table t2)
	{
		assertEquals(t1.nrows(), t2.nrows());
		assertEquals(t1.ncols(), t2.ncols());
		for (long j=0; j<t1.ncols(); j++)
		{
			for (long i=0; i<t1.nrows(); i++)
			{
				assertEquals(t1.getD(i, j), t2.getD(i, j), 1e-7);
			}
		}
	}
	
	static void singleCol(Table t, int n, long seed)
	{
		t.set_m(0, 4);
		assertEquals(1L, t.ncols());
		t.set_m(1, 5);
		assertEquals(4, t.getD(0), 1e-7);
		
		for (int i=0; i<n; i++)
		{
			t.set_m(i, i);
		}
		for (int i=0; i<n; i++)
		{
			assertEquals(i, t.getD(i), 1e-7);
			assertEquals(i, t.getL(i));
		}
		
		double[] v = t.toArrayD();
		long[] vl = t.toArrayL();
		assertEquals(n, v.length);
		for (int i=0; i<n; i++)
		{
			assertEquals(t.getD(i), v[i], 1e-7);
			assertEquals(t.getL(i), vl[i]);
		}
		assertEquals(null, t.getColName(0));
		t.setColNames_m("abc");
		assertEquals("abc", t.getColName(0));
		

		apply(t);

		int k = n/2;
		for (int i=0; i<n; i++)
		{
			t.set_m(i, i < k ? 21 : 12);
		}
		Table ti = t.whichD(x -> x == 21);
		assertEquals(k, ti.nrows());
		
		ti = t.orderedIndices();
		for (int i=1; i<n; i++)
		{
			assertTrue(t.getD(ti.getL(i - 1)) <= t.getD(ti.getL(i)));
		}
		
		
		List<Double> s = IntStream.range(0,  n).boxed().map(i -> (double) i).collect(Collectors.toList());
		Collections.shuffle(s, new Random(seed));
		for (int i=0; i<n; i++)
		{
			t.set_m(i, s.get(i));
		}
		ti = t.orderedIndices();
		for (int i=1; i<n; i++)
		{
			assertTrue(t.getD(ti.getL(i - 1)) <= t.getD(ti.getL(i)));
		}
		ti = t.orderedIndicesDesc();
		for (int i=1; i<n; i++)
		{
			assertTrue(t.getD(ti.getL(i - 1)) >= t.getD(ti.getL(i)));
		}
		
		Table tt = t.transpose();
		assertEquals(t.ncols(), tt.nrows());
		assertEquals(t.nrows(), tt.ncols());
		same(t, tt.transpose());

		long[] indices = LongStream.range(0, n).filter(i -> i % 2 == 0).toArray();
		Table t8 = t.rows(indices);
		assertEquals(indices.length, t8.nrows());
		
		for (int i=0; i<indices.length; i++)
		{
			assertEquals(t.getD(indices[i]), t8.getD(i), 1e-7);
		}
		
		Table t9 = t.rowBind(t, t);
		for (int i=0; i<n; i++)
		{
			assertEquals(t.getD(i), t9.getD(i), 1e-7);
			assertEquals(t.getD(i), t9.getD(n + i), 1e-7);
			assertEquals(t.getD(i), t9.getD(n*2 + i), 1e-7);
		}
		
		same(t.sortRows(), t.rows(t.orderedIndices()));

		/*
		 * 		StringWriter w = new StringWriter();
		IO.writeCSV(t4.toCSV(), w);
		System.out.println("t4 " + w.toString());
		
		w = new StringWriter();
		IO.writeCSV(t5.toCSV(), w);
		System.out.println("t5 " + w.toString());
		 */
	}
	
	static void apply(Table t)
	{
		t = t.copy();
		Table t2 = t.copy();
		same(t, t2);
		
		t.applyD_m(x -> x + 1);
		for (int j=0; j<t.ncols(); j++)
		{
			for (int i=0; i<t.nrows(); i++)
			{
				assertEquals(t2.getD(i, j) + 1, t.getD(i, j), 1e-7);
			}
		}

		
		t2 = t2.applyD(x -> x + 1);
		same(t, t2);
		
		Table t3 = t.applyD((x1, x2) -> x1 + x2, t2);
		for (int j=0; j<t.ncols(); j++)
		{
			for (int i=0; i<t.nrows(); i++)
			{
				assertEquals(t.getD(i, j) + t2.getD(i, j), t3.getD(i, j), 1e-7);
			}
		}
		
		Table t4 = t3.add(1).mul(2).divideBy(2).sub(1);
		same(t3, t4);
		
		Table t5 = t4.copy().add_m(1).mul_m(2).divideBy_m(2).sub_m(1);		
		same(t4, t5);
		
		Table t6 = t4.copy().round();			
		same(t4.applyD(v -> Math.round(v)), t6);

		Table t7 = t4.pow(2).sqrt();
		same(t4, t7);
		

		double min = t7.min().transpose().min().getD();
		double max = t7.max().transpose().max().getD();
		double sum = t7.sum().transpose().sum().getD();

		double min_ = Double.MAX_VALUE;
		double max_ = -Double.MAX_VALUE;
		double sum_ = 0;
		for (int j=0; j<t7.ncols(); j++)
		{
			for (int i=0; i<t7.nrows(); i++)
			{
				double val = t7.getD(i, j);
				min_ = Math.min(val, min_);
				max_ = Math.max(val, max_);
				sum_ += val;
			}
		}
		assertEquals(min_, min, 1e-7);
		assertEquals(max_, max, 1e-7);
		assertEquals(sum_, sum, 0.001);
	
	}

}
