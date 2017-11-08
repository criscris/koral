package xyz.koral;

import static org.junit.Assert.assertEquals;

import java.io.StringReader;
import java.io.StringWriter;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.junit.Test;

import xyz.koral.table.Table;

public class TableTest 
{
	
	@Test
	public void test1() 
	{
		Table t = Table.csvToData(IO.readCSV(new StringReader(
				  "A,B,C\n"
				+ "1,X,1\n"
				+ "1,X,2\n"
				+ "1,\"X\",3\n")));
		assertEquals(3, t.ncols());
		assertEquals(3, t.nrows());
		
		Table ta = t.aggregateByCols(a -> a.sum(), 1);
		assertEquals(3, ta.ncols());
		assertEquals(1, ta.nrows());
		assertEquals(t.cols(0).sum().getD(), ta.getD(0, 0), 1e-7);
		assertEquals(t.cols(2).sum().getD(), ta.getD(0, 2), 1e-7);
	}
	
	@Test
	public void test2() 
	{
		int n = 10;
		Table x = IntStream.range(0, n).boxed().map(i -> new double[] { i }).collect(Table.numericCollector()).setColNames_m("x");
		Table s = Stream.generate(() -> "abc").limit(n).map(t -> new String[] { t }).collect(Table.textCollector()).setColNames_m("s");
		Table c = Table.colBind_m(x, s);
		assertEquals(n, c.nrows());
		Table a = c.aggregateByCols(d -> d.max(), 1);
		assertEquals(1, a.nrows());
		assertEquals(n - 1, a.getD(0), 1e-7);
	}
	
	@Test
	public void testParallelRead() 
	{
		int nrows = 1000000;
		int ncols = 12;
		//Random rnd = new Random(2134L);

		Stream<List<String>> rows = IntStream.range(0, nrows + 1) // header
		.parallel()
		.mapToObj(i -> 
			IntStream.range(0,  ncols)
			.mapToObj(v -> "" + (v + i))
			.collect(Collectors.toList()));
		
		Table x = Table.csvToNumeric(rows);
		assertEquals(nrows, x.nrows());
		assertEquals(ncols, x.ncols());
	}
	

	static void log(Table table)
	{
		StringWriter w = new StringWriter();
		IO.writeCSV(table.toCSV(), w);
		System.out.println(w.toString());
	}
}
