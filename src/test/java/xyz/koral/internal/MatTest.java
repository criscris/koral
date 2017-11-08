package xyz.koral.internal;

import static org.junit.Assert.assertEquals;

import java.util.List;
import java.util.Random;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import java.util.stream.LongStream;

import org.junit.Test;

import xyz.koral.table.Table;
import xyz.koral.table.impl.Mat;

public class MatTest {

	@Test
	public void test() 
	{
		BiFunction<Long, Long, Table> table = (nrows, ncols) -> new Mat((int) (long) nrows, (int) (long) ncols);
		mat(table, 100, 10, 123);
		mat(table, 10, 101, 123);
		mat(table, 511, 1222, 123);
		mat(table, 10000, 5, 123);
	}
	
	static String string(Random rnd)
	{
		char[] c = new char[10];
		for (int i=0; i<c.length; i++) c[i] = (char) rnd.nextInt(60000);
		return new String(c);
	}
	
	static void fill(Table t, int nrows, int mcols, Random rnd)
	{
		for (int j=0; j<mcols; j++)
		{
			for (int i=0; i<nrows; i++)
			{
				t.set_m(i, j, string(rnd));
			}
		}
	}
	
	static void same(Table t1, Table t2)
	{
		assertEquals(t1.nrows(), t2.nrows());
		assertEquals(t1.ncols(), t2.ncols());
		for (long j=0; j<t1.ncols(); j++)
		{
			for (long i=0; i<t1.nrows(); i++)
			{
				assertEquals(t1.get(i, j), t2.get(i, j));
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
		same(t1, t2);
		
		int col = (int) Math.max(0, mcols - 2);
		String[] v = t1.streamS(col).toArray(n -> new String[n]);
		for (int i=0; i<nrows; i++)
		{
			assertEquals(t1.getS(i, col), v[i]);
		}
		
		List<String[]> rows = t1.streamRowsS().collect(Collectors.toList());
		for (int i=0; i<nrows; i++)
		{
			String[] row = rows.get(i);
			for (int j=0; j<mcols; j++)
			{
				assertEquals(t1.getS(i, j), row[j]);
			}
		}
		
		assertEquals(null, t1.getColName(col));
		t1.setColNames_m(col, "abc");
		assertEquals("abc", t1.getColName(col));

		Table tt = t1.transpose();
		assertEquals(t1.ncols(), tt.nrows());
		assertEquals(t1.nrows(), tt.ncols());
		same(t1, tt.transpose());
		
		long[] indices = LongStream.range(0, nrows).filter(i -> i % 2 == 0).toArray();
		Table t8 = t1.rows(indices);
		assertEquals(indices.length, t8.nrows());
		
		for (int j=0; j<mcols; j++)
		{
			for (int i=0; i<indices.length; i++)
			{
				assertEquals(t1.getS(indices[i], j), t8.getS(i, j));
			}
		}
	
		Table t9 = t1.rowBind(t1, t1);
		for (int j=0; j<mcols; j++)
		{
			for (int i=0; i<nrows; i++)
			{
				assertEquals(t1.get(i, j), t9.get(i, j));
				assertEquals(t1.get(i, j), t9.get(nrows + i, j));
				assertEquals(t1.get(i, j), t9.get(nrows*2 + i, j));
			}
		}
		
		same(t1.sortRows(), t1.rows(t1.orderedIndices()));
	}
}
