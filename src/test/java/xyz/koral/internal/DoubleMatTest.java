package xyz.koral.internal;

import static org.junit.Assert.assertEquals;

import java.io.StringReader;
import java.util.List;
import java.util.Random;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import java.util.stream.LongStream;

import org.junit.Test;

import xyz.koral.IO;
import xyz.koral.Table;

public class DoubleMatTest 
{
	@Test
	public void test1() 
	{
		BiFunction<Long, Long, Table> table = (nrows, ncols) -> new DoubleMat(nrows, ncols);
		
		mat(table, 100, 10, 123);
		mat(table, 1000, 11, 124);
		mat(table, 10000, 12, 123);
		mat(table, 89765, 4, 123);
		mat(table, 189765, 44, 123);
	}
	
	@Test
	public void test2() 
	{
		Table t = Table.csvToNumeric(IO.readCSV(new StringReader(
				  "A,B,C\n"
				+ "1,2,1\n"
				+ "1,2,2\n"
				+ "2,2,3\n")));
		assertEquals(3, t.ncols());
		assertEquals(3, t.nrows());
		
		Table ta = t.aggregateByCols(a -> a.sum(), 1);
		assertEquals(3, ta.ncols());
		assertEquals(1, ta.nrows());
		assertEquals(t.cols(0).sum().getD(), ta.getD(0, 0), 1e-7);
		assertEquals(t.cols(2).sum().getD(), ta.getD(0, 2), 1e-7);
	}
	
	static void fill(Table t, int nrows, int mcols, Random rnd)
	{
		for (int j=0; j<mcols; j++)
		{
			for (int i=0; i<nrows; i++)
			{
				t.set_m(i, j, rnd.nextDouble());
			}
		}
	}
	
	static void mat(BiFunction<Long, Long, Table> table, long nrows, long mcols, long seed)
	{
		Table t1 = table.apply(nrows, mcols);
		Random rnd = new Random(seed);
		fill(t1, (int) nrows, (int) mcols, rnd);
		assertEquals(nrows, t1.nrows());
		assertEquals(mcols, t1.ncols());
		
		Table t2 = t1.copy();
		DoubleVecTest.same(t1, t2);
		
		int col = (int) Math.max(0, mcols - 2);
		double[] v = t1.streamD(col).toArray();
		for (int i=0; i<nrows; i++)
		{
			assertEquals(t1.getD(i, col), v[i], 1e-7);
		}
		
		List<double[]> rows = t1.streamRowsD().collect(Collectors.toList());
		for (int i=0; i<nrows; i++)
		{
			double[] row = rows.get(i);
			for (int j=0; j<mcols; j++)
			{
				assertEquals(t1.getD(i, j), row[j], 1e-7);
			}
		}
		
		assertEquals(null, t1.getColName(col));
		t1.setColNames_m(col, "abc");
		assertEquals("abc", t1.getColName(col));
		
		DoubleVecTest.apply(t1);
		
		
		long[] aggCol = LongStream.range(0, nrows).map(i -> i % 2).toArray();
		Table t3 = t1.copy();
		for (int i=0; i<nrows; i++) t3.set_m(i, aggCol[i]);
		Table t3a = t3.aggregateByCols(a -> a.sum(), 0);
		assertEquals(2, t3a.nrows());
		for (int j=1; j<mcols; j++)
		{
			assertEquals(t3.cols(j).sum().getD(), t3a.cols(j).sum().getD(), 1e-4);
		}
		
		aggCol = LongStream.range(0, nrows).map(i -> i % 3).toArray();
		t3 = t1.copy();
		for (int i=0; i<nrows; i++)
		{
			t3.set_m(i, aggCol[i]);
			t3.set_m(i, 1, aggCol[i] + (i%2));
		}
		t3a = t3.aggregateByCols(a -> a.sum(), 0, 1);
		assertEquals(6, t3a.nrows());
		for (int j=2; j<mcols; j++)
		{
			assertEquals(t3.cols(j).sum().getD(), t3a.cols(j).sum().getD(), 1e-4);
		}
		
		Table tt = t1.transpose();
		assertEquals(t1.ncols(), tt.nrows());
		assertEquals(t1.nrows(), tt.ncols());
		DoubleVecTest.same(t1, tt.transpose());

		long[] indices = LongStream.range(0, nrows).filter(i -> i % 2 == 0).toArray();
		Table t8 = t1.rows(indices);
		assertEquals(indices.length, t8.nrows());
		
		for (int j=0; j<mcols; j++)
		{
			for (int i=0; i<indices.length; i++)
			{
				assertEquals(t1.getD(indices[i], j), t8.getD(i, j), 1e-7);
			}
		}
	
		Table t9 = t1.rowBind(t1, t1);
		for (int j=0; j<mcols; j++)
		{
			for (int i=0; i<nrows; i++)
			{
				assertEquals(t1.getD(i, j), t9.getD(i, j), 1e-7);
				assertEquals(t1.getD(i, j), t9.getD(nrows + i, j), 1e-7);
				assertEquals(t1.getD(i, j), t9.getD(nrows*2 + i, j), 1e-7);
			}
		}
		
		DoubleVecTest.same(t1.sortRows(), t1.rows(t1.orderedIndices()));
	}
}
