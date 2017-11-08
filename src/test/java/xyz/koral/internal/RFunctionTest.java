package xyz.koral.internal;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.stream.IntStream;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import xyz.koral.IO;
import xyz.koral.R;
import xyz.koral.compute.config.DataFormat;
import xyz.koral.compute.config.DataSource;
import xyz.koral.compute.config.Param;
import xyz.koral.table.Table;

public class RFunctionTest 
{
	File baseDir;
	String csvFile1;
	String scriptFile;
	String scriptFile2;
	
	@Before
	public void init() throws IOException
	{
		baseDir = Files.createTempDirectory("koral_rFuncTest").toFile();
		
		csvFile1 = "test1.csv";
		Table x = Table.colBind(
				Table.numeric(IntStream.range(1,  5)),
				Table.numeric(IntStream.range(1,  5)).applyD(v -> v*v)).setColNames_m("x", "y");
		IO.writeCSV(x.toCSV(), IO.ostream(new File(baseDir, csvFile1)));
		
		scriptFile = "script1.R";
		String r = "tesfu = function(inframe, j) {\n" +
						"inframe + j\n" +
				   "}\n";
		IO.write(r, IO.ostream(new File(baseDir, scriptFile)));
		
		scriptFile2 = "script2.R";
		r = "tesfu = function(jso) {\n" +
						"data.frame(x=jso$x)\n" +
				   "}\n";
		IO.write(r, IO.ostream(new File(baseDir, scriptFile2)));
	}
		
	@After
	public void close()
	{
		IO.delete(baseDir);
	}
	
	@Test
	public void testR() 
	{
		R r = new R();
		r.exec("x = 30");
		r.exec("y = x/3");
		assertEquals(10.0, r.getD("y"), 1e-6);
		
		Table z = Table.colBind(
				Table.numeric(IntStream.range(0,  10)),
				Table.numeric(IntStream.range(0,  10)).applyD(v -> v*v)).setColNames_m("x", "y");
		
		r.set("z", z);
		Table z_ = r.get("z");
		JavaFunctionTest.assertEquals_numeric(z, z_);
		
		z = z.add(1);
		r.exec("z = z + 1");
		z_ = r.get("z");
		JavaFunctionTest.assertEquals_numeric(z, z_);

		r.close();
	}
	
	@Test
	public void testR2()
	{
		R r = new R();
		r.exec("tesfu = function(inframe, j) { inframe + j }");
		r.exec("z = tesfu(8, 2)");
		System.out.println("the result: " + r.getD("z"));
		r.close();
	}
	
	@Test
	public void test1()
	{
		int j = 2;
		Table expected = Table.csvToData(IO.readCSV(IO.istream(new File(baseDir, csvFile1))))
				.add(j);
		
		String outFile = "testOut1.csv";
		RFunction f = new RFunction();
		f.init(baseDir, outFile, new DataSource(scriptFile + "::tesfu", "R")
				.addParam("inframe", Param.create(DataFormat.csv,  csvFile1))
				.addParam("j", Param.createJson(j)));
		f.run();
		
		Table result = Table.csvToData(IO.readCSV(IO.istream(new File(baseDir, outFile))));
		JavaFunctionTest.assertEquals_numeric(expected, result);
	}
	
	@Test
	public void test2()
	{
		String outFile = "testOut2.csv";
		RFunction f = new RFunction();
		f.init(baseDir, outFile, new DataSource(scriptFile2 + "::tesfu", "R")
				.addParam("jso", Param.createJson(new B(43))));
		f.run();
		
		Table result = Table.csvToData(IO.readCSV(IO.istream(new File(baseDir, outFile))));
		System.out.println(result.toCSVString());
	}
}

class B
{
	double x;
	
	public B(double x)
	{
		this.x = x;
	}
}
