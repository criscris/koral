package xyz.koral.table;

import java.io.Serializable;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import java.util.stream.Stream;

import xyz.koral.IO;
import xyz.koral.KoralError;
import xyz.koral.table.impl.AbstractTable;
import xyz.koral.table.impl.Columns;
import xyz.koral.table.impl.DoubleMat;
import xyz.koral.table.impl.DoubleVec;
import xyz.koral.table.impl.Mat;


/**
 * Methods ending with _m modify the original table and return itself.
 * zero-based indices.
 * 
 * Methods:
	Readable
		long nrows();
		long ncols();
		int nrowsI();
		int ncolsI();
		Object get(long rowIndex, long colIndex);
		String getS(long rowIndex, long colIndex);
		double getD(long rowIndex, long colIndex);
		int getI(long rowIndex, long colIndex);
		long getL(long rowIndex, long colIndex);
		Object get();
		String getS();
		double getD();
		int getI();
		long getL();
		Object get(long rowIndex);
		String getS(long rowIndex);
		double getD(long rowIndex);
		int getI(long rowIndex);
		long getL(long rowIndex);
		Stream<String> streamS(long colIndex);
		DoubleStream streamD(long colIndex);
		LongStream streamL(long colIndex);
		IntStream streamI(long colIndex);
		Stream<String> streamS();
		DoubleStream streamD();
		LongStream streamL();
		IntStream streamI();
		String[] toArrayS(long colIndex);
		double[] toArrayD(long colIndex);
		int[] toArrayI(long colIndex);
		long[] toArrayL(long colIndex);
		String[] toArrayS();
		double[] toArrayD();
		int[] toArrayI();
		long[] toArrayL();
		Stream<Object[]> streamRows();
		Stream<String[]> streamRowsS();
		Stream<double[]> streamRowsD();
		Stream<int[]> streamRowsI();
		Stream<long[]> streamRowsL();
		String getColName(long colIndex);
		Table copy();
		
	Modifiable
		Table setColName_m(long colIndex, String colId);
		Table setColNames_m(long firstColIndex, String... colIds);
		Table setColNames_m(String... colIds);
		Table set_m(long rowIndex, long colIndex, Object value);
		Table set_m(long rowIndex, long colIndex, String value);
		Table set_m(long rowIndex, long colIndex, double value);
		Table set_m(long rowIndex, long colIndex, int value);
		Table set_m(long rowIndex, long colIndex, long value);
		Table set_m(long rowIndex, Object value);
		Table set_m(long rowIndex, String value);
		Table set_m(long rowIndex, double value);
		Table set_m(long rowIndex, int value);
		Table set_m(long rowIndex, long value);
		
	Transformable
		Table applyD(DoubleUnaryOperator op);
		Table applyD(DoubleUnaryOperator op, long... colIndices);
		Table applyD(DoubleBinaryOperator op, Table operand2);
		Table applyD_m(DoubleUnaryOperator op);
		Table applyD_m(DoubleUnaryOperator op, long... colIndices);
		Table applyD_m(DoubleBinaryOperator op, Table operand2);
		Table add(double value);
		Table add(double value, long... colIndices);
		Table add(Table values);
		Table add_m(double value);
		Table add_m(double value, long... colIndices);
		Table add_m(Table values);
		Table sub(double value);
		Table sub(double value, long... colIndices);
		Table sub(Table values);
		Table sub_m(double value);
		Table sub_m(double value, long... colIndices);
		Table sub_m(Table values);
		Table mul(double value);
		Table mul(double value, long... colIndices);
		Table mul(Table values);
		Table mul_m(double value);
		Table mul_m(double value, long... colIndices);
		Table mul_m(Table values);
		Table divideBy(double value);
		Table divideBy(double value, long... colIndices);
		Table divideBy(Table values);
		Table divideBy_m(double value);
		Table divideBy_m(double value, long... colIndices);
		Table divideBy_m(Table values);
		Table round()
		Table round(long... colIndices);
		Table round_m();
		Table round_m(long... colIndices);
		Table log10();
		Table log10(long... colIndices);
		Table log10_m();
		Table log10_m(long... colIndices);
		Table sqrt();
		Table sqrt(long... colIndices);
		Table sqrt_m();
		Table sqrt_m(long... colIndices);
		Table abs();
		Table abs(long... colIndices);
		Table abs_m();
		Table abs_m(long... colIndices);
		Table pow(double exp);
		Table pow(double exp, long... colIndices);
		Table pow_m(double exp);
		Table pow_m(double exp, long... colIndices);
		Table powBase(double base);
		Table powBase(double base, long... colIndices);
		Table powBase_m(double base);
		Table powBase_m(double base, long... colIndices);
	
	Summable
		Table reduceRowsD(DoubleBinaryOperator op);
		Table aggregateByCols(UnaryOperator<Table> reducer, long... byColIndices);
		Table min();
		Table max();
		Table sum();
		Table mean();
		Table sd(SDType type);
		Table se(SDType type);
		
	Indexable
		Table whichS(Predicate<String> op, long colIndex);
		Table whichD(DoublePredicate op, long colIndex);
		Table whichI(IntPredicate op, long colIndex);
		Table whichL(LongPredicate op, long colIndex);
		Table whichS(Predicate<String> op);
		Table whichD(DoublePredicate op);
		Table whichI(IntPredicate op);
		Table whichL(LongPredicate op);
		Table orderedIndices(long colIndex);
		Table orderedIndicesDesc(long colIndex);
		Table orderedIndices();
		Table orderedIndicesDesc();
		
	Shapeable
		Table transpose();
		Table rows(long... rowIndices);
		Table rows(LongStream rowIndices);
		Table rows(Table rowIndices);
		Table cols(long... colIndices);
		Table cols(LongStream colIndices);
		Table cols(Table colIndices);
		Table rowBind(Table... others);
		Table sortRows(long... byColIndices);
		Table sortRows();
			
				
  Implementations:
	DoubleVec: one column, double type. nrows may grow.
	DoubleMat: 2D. double type. fixed size.
	Mat: 2D. any type. nrows or ncols may grow.
	Columns: 2D. any type. fixed size. Is a column-wise composition of multiple Table types.
 *
 * 
 * 
 */
public interface Table extends Readable, Modifiable, Transformable, Summable, Indexable, Shapeable, Serializable
{
	public static Table seq(double from, double to, double by)
	{
		/// use  BigDecimal to prevent precision errors e.g. 0.2*3==0.6000000000001
		BigDecimal from_ = new BigDecimal(from);
		BigDecimal to_ = new BigDecimal(to);
		BigDecimal by_ = new BigDecimal(by);
		
		BigDecimal di = to_.subtract(from_);
		BigDecimal n_ = di.divide(by_, 25, RoundingMode.HALF_UP).add(new BigDecimal(1));
		int n = (int) n_.doubleValue();
		double[] d = new double[n];
		for (int i=0; i<n; i++)
		{
			d[i] = by_.multiply(new BigDecimal((double) i)).add(from_).doubleValue();
		}
		return numeric(DoubleStream.of(d));
	}
	
	public static Table seq(long from, long to)
	{
		int n = (int) (to - from + 1);
		long[] l = new long[n];
		for (int i=0; i<n; i++)
		{
			l[i] = from + i;
		}
		return numeric(LongStream.of(l));
	}
	
	static Table numeric(DoubleStream stream)
	{
		return new DoubleVec(stream);
	}
	
	static Table numeric(IntStream stream)
	{
		return numeric(stream.asDoubleStream());
	}
	
	static Table numeric(LongStream stream)
	{
		return numeric(stream.asDoubleStream());
	}
	
	static Table numeric(Stream<double[]> rows)
	{
		return rows.collect(numericCollector());
	}
	
	static Collector<double[], ?, Table> numericCollector()
	{
		return Collector.of(
			() -> 
			{ 
				List<Table> cols = new ArrayList<>();
				return cols;
			}, 
			(cols, v) -> 
			{
				if (cols.size() == 0)
				{
					for (int i=0; i<v.length; i++) cols.add(new DoubleVec());
				}
				else if (v.length != cols.size()) 
					throw new KoralError("Supplied row with " + v.length + " columns, required: " + cols.size() + ".");
				
				for (int i=0; i<v.length; i++)
				{
					Table col = cols.get(i);
					col.set_m(col.nrows(), v[i]);
				}
			}, 
			(a1, a2) -> 
			{
				if (a2.size() == 0) return a1;
				if (a1.size() == 0) return a2;
				
 				
				for (int i=0; i<a1.size(); i++)
				{
					Table a = a1.get(i);
					Table b = a2.get(i);
					for (long j=0; j<b.nrows(); j++) a.set_m(a.nrows(), b.getD(j));
				}
				return a1;
			}, a ->
			{
				if (a.size() == 1) return a.get(0);
				return new Columns(a);
			});
	}
	
	static Table numeric(long nrows)
	{
		return new DoubleVec(nrows);
	}
	
	static Table numeric(long nrows, long ncols)
	{
		return new DoubleMat(nrows, ncols);
	}
	
	static Table text(Stream<String> stream)
	{
		return new Mat(stream.map(s -> s));
	}
	
	static Collector<String[], ?, Table> textCollector()
	{
		return Collector.of(
			() -> 
			{ 
				List<List<Object>> cols = new ArrayList<>();
				return cols;
			}, 
			(cols, v) -> 
			{
				if (cols.size() == 0)
				{
					for (int i=0; i<v.length; i++) cols.add(new ArrayList<>());
				}
				else if (v.length != cols.size()) 
					throw new KoralError("Supplied row with " + v.length + " columns, required: " + cols.size() + ".");
				
				for (int i=0; i<v.length; i++)
				{
					List<Object> col = cols.get(i);
					col.add(v[i]);
				}
			}, 
			(a1, a2) -> 
			{
				for (int i=0; i<a1.size(); i++)
				{
					a1.get(i).addAll(a2.get(i));
				}
				return a1;
			}, a ->
			{
				return new Mat(a);
			});
	}
	
	static Table text(long nrows)
	{
		return text(nrows, 1);
	}
	
	static Table text(long nrows, long ncols)
	{
		if (nrows >= Integer.MAX_VALUE - 5 || ncols >= Integer.MAX_VALUE - 5) 
			throw new KoralError("Invalid size request: (" + nrows + "," + ncols + ")");
		return new Mat((int) nrows, (int) ncols);
	}
	
	static Collector<Object[], ?, Table> dataCollector()
	{
		return Collector.of(
			() -> 
			{ 
				List<Table> cols = new ArrayList<>();
				return cols;
			}, 
			(cols, v) -> 
			{
				if (cols.size() == 0)
				{
					for (int i=0; i<v.length; i++)
					{
						Object val = v[i];
						cols.add(val instanceof Number ? new DoubleVec() : new Mat(0, 1));
					}
				}
				else if (v.length != cols.size()) 
					throw new KoralError("Supplied row with " + v.length + " columns, required: " + cols.size() + ".");
				
				for (int i=0; i<v.length; i++)
				{
					Table col = cols.get(i);
					col.set_m(col.nrows(), v[i]);
				}
			}, 
			(a1, a2) -> 
			{
				List<Table> cols_ = new ArrayList<>();
				for (int i=0; i<a1.size(); i++)
				{
					cols_.add(a1.get(i).rowBind(a2.get(i)));
				}
				return cols_;
			}, a ->
			{
				if (a.size() == 1) return a.get(0);
				return new Columns(a);
			});
	}

	static Table csvToNumeric(Stream<List<String>> rows)
	{
		return AbstractTable.header(rows, 
				r -> numeric(r.map(row -> row.stream().mapToDouble(e -> IO.stringToNumber(e)).toArray())));
	}
	
	static Table csvToText(Stream<List<String>> rows)
	{
		return AbstractTable.header(rows, 
				r -> r.map(row -> row.toArray(new String[0])).collect(textCollector()));
	}
	
	static Table csvToData(Stream<List<String>> rows)
	{
		class Types
		{
			boolean[] isNumber;
		}
		Types t = new Types();
		return AbstractTable.header(rows, 
				r -> r
				.map(row -> 
				{
					if (t.isNumber == null)
					{
						t.isNumber = new boolean[row.size()];
						for (int i=0; i<row.size(); i++)
						{
							try
							{
								new Double(row.get(i));
								t.isNumber[i] = true;
							}
							catch (Exception ex)
							{
								
							}
						}
					}
					
					if (row.size() != t.isNumber.length) 
						throw new KoralError("Number of csv row elements don't match. expected=" + t.isNumber.length + ". actual=" + row.size());
					Object[] o = new Object[row.size()];
					for (int i=0; i<o.length; i++)
					{
						o[i] = t.isNumber[i] ? IO.stringToNumber(row.get(i)) : row.get(i);
					}
					return o;
				}).collect(dataCollector()));
	}
	
	static Table colBind(List<Table> tables)
	{
		if (tables == null || tables.size() == 0) throw new KoralError("No table specified.");
		return new Columns(tables.stream().map(a -> a.copy()).collect(Collectors.toList()));
	}
	
	static Table colBind(Table... tables)
	{
		if (tables == null || tables.length == 0) throw new KoralError("No table specified.");
		return colBind(Arrays.asList(tables));
	}
	
	static Table colBind_m(List<Table> tables)
	{
		if (tables == null || tables.size() == 0) throw new KoralError("No table specified.");
		return new Columns(new ArrayList<>(tables));
	}
	
	static Table colBind_m(Table... tables)
	{
		if (tables == null || tables.length == 0) throw new KoralError("No table specified.");
		return new Columns(Arrays.asList(tables));
	}
	
	static Table rowBind(List<Table> tables)
	{
		if (tables == null || tables.size() == 0) throw new KoralError("No table specified.");
		if (tables.size() == 1) return tables.get(0).copy();
		return tables.get(0).rowBind(tables.subList(1, tables.size()).toArray(new Table[0]));
	}
	
	default Table sortRows(long... byColIndices) 
	{
		if (byColIndices == null || byColIndices.length == 0) 
			throw new KoralError("No columns specified to sort for.");
		Table result = this;
		for (int i=byColIndices.length-1; i>=0; i--)
		{
			result = result.rows(result.orderedIndices(byColIndices[i]));
		}
		return result;
	}
	
	default Stream<List<String>> toCSV()
	{
		List<String> header = 
		LongStream.range(0, ncols())
		.mapToObj(i -> { 
			String name = getColName(i);
			return name == null ? "col" + i : name; 
		})
		.collect(Collectors.toList());
		return Stream.concat(Stream.of(header), streamRowsS().map(array -> Arrays.asList(array)));
	}
	
	default String toCSVString()
	{
		StringWriter w = new StringWriter();
		IO.writeCSV(toCSV(), w);
		return w.toString();
	}
}
