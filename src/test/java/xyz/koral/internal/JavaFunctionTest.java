package xyz.koral.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import xyz.koral.IO;
import xyz.koral.compute.config.DataFormat;
import xyz.koral.compute.config.DataSource;
import xyz.koral.compute.config.Param;
import xyz.koral.table.Table;


public class JavaFunctionTest
{
	File baseDir;
	String jsonlFile1;
	String csvFile1;
	
	@Before
	public void init() throws IOException
	{
		baseDir = Files.createTempDirectory("koral_javaFuncTest").toFile();
		
		jsonlFile1 = "test1.jsonl";
		IO.writeJSONStream(Stream.of(
				new A("0", new int[] { 0, 1}), 
				new A("1", new int[] { 2, 3})), IO.ostream(new File(baseDir, jsonlFile1)));
		
		csvFile1 = "test1.csv";
		Table x = Table.colBind(
				Table.numeric(IntStream.range(1,  5)),
				Table.numeric(IntStream.range(1,  5)).applyD(v -> v*v)).setColNames_m("x", "y");
		IO.writeCSV(x.toCSV(), IO.ostream(new File(baseDir, csvFile1)));
	}
		
	@After
	public void close()
	{
		IO.delete(baseDir);
	}
	
	@Test
	public void testStream() 
	{
		Table expected = TestC.func1(IO.readJSONStream(IO.istream(new File(baseDir, jsonlFile1)), A.class));	
		String outFile = "testOut.csv";
		for (int l=1; l<=3; l++)
		{
			JavaFunction f = new JavaFunction();
			f.init(baseDir, outFile, 
					new DataSource("xyz.koral.internal.TestC::func" + l, "java").addParam("a", Param.create(DataFormat.jsonl,  jsonlFile1)));
			f.run();
			Table result = Table.csvToNumeric(IO.readCSV(IO.istream(new File(baseDir, outFile))));
			assertEquals_numeric(expected, result);
		}
	}
	
	@Test
	public void testTable() 
	{
		Table expected = TestC.func4(Table.csvToNumeric(IO.readCSV(IO.istream(new File(baseDir, csvFile1)))));	
		String outFile = "testOut2.csv";
		
		JavaFunction f = new JavaFunction();
		f.init(baseDir, outFile, 
				new DataSource("xyz.koral.internal.TestC::func4", "java").addParam("a", Param.create(DataFormat.csv, csvFile1)));
		f.run();
		Table result = Table.csvToNumeric(IO.readCSV(IO.istream(new File(baseDir, outFile))));
		assertEquals_numeric(expected, result);
	}
	
	@Test
	public void testSimpleTypes1() 
	{
		A expected = TestC.func5();
		String outFile = "testOut3.json";
		JavaFunction f = new JavaFunction();
		f.init(baseDir, outFile, 
				new DataSource("xyz.koral.internal.TestC::func5", "java"));
		f.run();
		A result = IO.readJSON(IO.istream(new File(baseDir, outFile)), A.class);
		assertTrue(expected.equals(result));
	}
	
	@Test
	public void testSimpleTypes2() 
	{
		A expected = TestC.func6("first value",  new int[] { 1, 2, 3, 4 });
		String outFile = "testOut4.json";
		JavaFunction f = new JavaFunction();
		f.init(baseDir, outFile, 
				new DataSource("xyz.koral.internal.TestC::func6", "java")
				.addParam("val1", Param.createJson("first value"))
				.addParam("arr", Param.createJson(new int[] { 1, 2, 3, 4 })));
		f.run();
		A result = IO.readJSON(IO.istream(new File(baseDir, outFile)), A.class);
		assertTrue(expected.equals(result));
	}
	
	@Test
	public void testSimpleMixed() 
	{
		Table expected = TestC.func7(1, IO.readJSONStream(IO.istream(new File(baseDir, jsonlFile1)), A.class), 77);	
		String outFile = "testOut5.csv";
		JavaFunction f = new JavaFunction();
		f.init(baseDir, outFile, 
				new DataSource("xyz.koral.internal.TestC::func7", "java")
				.addParam("a", Param.create(DataFormat.jsonl, jsonlFile1))
				.addParam("skip",  Param.createJson(1))
				.addParam("add",Param.createJson(77)));
		f.run();
		Table result = Table.csvToNumeric(IO.readCSV(IO.istream(new File(baseDir, outFile))));	
		assertEquals_numeric(expected, result);
	}
	
	@Test
	public void testReturnStream() 
	{
		List<A> expected = TestC.func8(IO.readJSONStream(IO.istream(new File(baseDir, jsonlFile1)), A.class)).collect(Collectors.toList());	
		String outFile = "testOut6.jsonl";
		JavaFunction f = new JavaFunction();
		f.init(baseDir, outFile, 
				new DataSource("xyz.koral.internal.TestC::func8", "java")
				.addParam("a", Param.create(DataFormat.jsonl, jsonlFile1)));
		f.run();
		List<A> result = IO.readJSONStream(IO.istream(new File(baseDir, outFile)), A.class).collect(Collectors.toList());
		assertEquals(expected, result);	
	}

	
	static void assertEquals_numeric(Table expected, Table actual)
	{
		assertEquals(expected.nrows(), actual.nrows());
		assertEquals(expected.ncols(), actual.ncols());
		
		for (long x=0; x<expected.ncols(); x++)
		{
			for (long y=0; y<expected.nrows(); y++)
			{
				assertEquals(expected.getD(y, x), actual.getD(y, x), 1e-6);
			}
		}
	}
}

class A
{
	public String val1;
	public int[] arr;
	
	public A(String val1, int[] arr) 
	{
		this.val1 = val1;
		this.arr = arr;
	}


	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		A other = (A) obj;
		if (!Arrays.equals(arr, other.arr))
			return false;
		if (val1 == null) {
			if (other.val1 != null)
				return false;
		} else if (!val1.equals(other.val1))
			return false;
		return true;
	}
}

class TestC
{
	public static Table func1(Stream<A> a)
	{
		return Table.text(a.map(x -> x.val1)).setColNames_m("val1");
	}
	
	public static Table func2(List<A> a)
	{
		return Table.text(a.stream().map(x -> x.val1)).setColNames_m("val1");
	}
	
	public static Table func3(Collection<A> a)
	{
		return Table.text(a.stream().map(x -> x.val1)).setColNames_m("val1");
	}
	
	public static Table func4(Table a)
	{
		return a;
	}
	
	public static A func5()
	{
		return new A("1", new int[] {1, 2, 3});
	}
	
	public static A func6(String val1, int[] arr)
	{
		return new A(val1, arr);
	}
	
	public static Table func7(int skip, Stream<A> a, int add)
	{
		return Table.numeric(a.skip(skip).mapToDouble(x -> new Double(x.val1) + add)).setColNames_m("val1");
	}
	
	public static Stream<A> func8(Stream<A> a)
	{
		return a.map(arg -> 
		{
			arg.val1 += "100";
			return arg;
		});
	}
}