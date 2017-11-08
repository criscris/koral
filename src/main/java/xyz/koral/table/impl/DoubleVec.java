package xyz.koral.table.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.function.DoubleBinaryOperator;
import java.util.function.DoubleUnaryOperator;
import java.util.function.UnaryOperator;
import java.util.stream.DoubleStream;
import java.util.stream.LongStream;
import java.util.stream.Stream;

import xyz.koral.KoralError;
import xyz.koral.table.Table;

/**
 * a one column vector of double entries. maximum size is only limited by available memory (roughly 8 byte * nrows).
 * grows dynamically. as large as specified or index + 1 of the largest set(index,value) call
 *
 */
public class DoubleVec extends DoubleTable
{
	private static final long serialVersionUID = 8709509377184974877L;
	
	String name;
	boolean small;
	
	public DoubleVec()
	{
		data = new GrowingDoubleArray();
		small = true;
	}
	
	public DoubleVec(DoubleStream stream)
	{
		this();
		stream.forEachOrdered(v -> 
		{
			long rowIndex = data.size();
			checkSize(rowIndex);
			data.set(rowIndex, v);
		});
	}
	
	public DoubleVec(long nrows)
	{
		if (nrows < GrowingBigDoubleArray.DEFAULTCHUNKSIZE)
		{
			data =  new GrowingDoubleArray((int) nrows);
			small = true;
		}
		else
		{
			data =  new GrowingBigDoubleArray(nrows);
			small = false;
		}
		if (nrows > 0) data.set(nrows - 1, 0);
	}
	
	private DoubleVec(DoubleArray data, boolean small, String name)
	{
		this.data = data;
		this.name = name;
		this.small = small;
	}
	
	public Table copy() 
	{
		return new DoubleVec(data.copy(), small, name);
	}

	public long nrows() 
	{
		return data.size();
	}

	public long ncols() 
	{
		return 1;
	}
	
	public String getColName(long colIndex) 
	{
		if (colIndex != 0) throw new KoralError("Invalid column index: " + colIndex + ". Expected: 0.");
		return name;
	}
	
	public long getColIndex(String colName)
	{
		if (!colName.equals(name)) throw new KoralError("Invalid column name: " + colName + ". Expected: " + name);
		return 0;
	}

	public Table setColName_m(long colIndex, String colId) 
	{
		this.name = colId;
		return this;
	}
	
	public double getD(long rowIndex, long colIndex) 
	{
		return data.get(rowIndex);
	}

	public DoubleStream streamD(long colIndex) 
	{
		checkColIndex(colIndex);
		return data.stream();
	}
	
	public Stream<double[]> streamRowsD() 
	{
		return data.stream().mapToObj(v -> new double[] { v });
	}
	
	private final void checkSize(long requestedRowIndex)
	{
		if (small && requestedRowIndex >= GrowingBigDoubleArray.DEFAULTCHUNKSIZE)
		{
			GrowingDoubleArray dataOld = (GrowingDoubleArray) data;
			GrowingBigDoubleArray dataNew =  new GrowingBigDoubleArray();
			dataOld.trim();
			dataNew.set(0, dataOld.data);
			small = false;
		}
	}
	
	public Table set_m(long rowIndex, long colIndex, double value) 
	{
		checkSize(rowIndex);
		data.set(rowIndex, value);
		return this;
	}

	public Table applyD(DoubleUnaryOperator op) 
	{
		return new DoubleVec(data.copyAndApply(op), small, name);
	}
	
	private final void checkColIndicesParam(long... colIndices)
	{
		if (colIndices == null) return;
		if (colIndices.length > 1) throw new KoralError(
				"DoubleVec is only one column. Number of columns specified: " + colIndices.length);
		if (colIndices.length == 1 && colIndices[0] != 0) throw new KoralError(
				"DoubleVec is only one column. Specified column index: " + colIndices[0]);
	}

	public Table applyD(DoubleUnaryOperator op, long... colIndices) 
	{
		checkColIndicesParam(colIndices);
		return applyD(op);
	}

	public Table applyD_m(DoubleUnaryOperator op, long... colIndices) 
	{
		checkColIndicesParam(colIndices);
		applyD_m(op);
		return this;
	}

	public Table applyD_m(DoubleBinaryOperator op, Table operand2) 
	{
		checkDimensions(operand2);
		for (long i=0; i<data.size(); i++) data.set(i, op.applyAsDouble(data.get(i), operand2.getD(i % operand2.nrows())));
		return this;
	}


	public Table reduceRowsD(DoubleBinaryOperator op) 
	{
		if (nrows() == 0) throw new KoralError("reduceRowsD() needs at least one row.");
		double result = streamD().reduce(op).getAsDouble();
		return new DoubleVec(1).set_m(0, result).setColNames_m(name);
	}
	
	Collection<List<Long>> distinctRows(long colIndex)
	{
		throw new KoralError("aggregateByCols() not allowed for one-column data.");
	}
	
	Collection<List<Long>> distinctRows(long[] colIndices)
	{
		throw new KoralError("aggregateByCols() not allowed for one-column data.");
	}

	public Table aggregateByCols(UnaryOperator<Table> reducer, long... byColIndices) 
	{
		throw new KoralError("aggregateByCols() not allowed for one-column data.");
	}

	public Table transpose() 
	{
		DoubleMat t = new DoubleMat(1, nrows());
		for (long i=0; i<nrows(); i++) t.set_m(0,  i, getD(i));
		return t;
	}

	public Table rows(long... rowIndices) 
	{
		return rows(LongStream.of(rowIndices));
	}
	
	public Table rows(LongStream rowIndices) 
	{
		return Table.numeric(rowIndices.mapToDouble(i -> data.get(i))).setColNames_m(name);
	}

	public Table cols(long... colIndices) 
	{
		checkColIndicesParam(colIndices);
		return copy(); // can only select first column
	}
	
	public Table rowBind(Table... others) 
	{
		if (others == null || others.length == 0) return copy();
		for (Table o : others) if (o.ncols() != 1) throw new KoralError("rowBind(): number of columns mismatch:  1 != " + o.ncols());
		
		List<Table> all = new ArrayList<>();
		all.add(this);
		all.addAll(Arrays.asList(others));
		return Table.numeric(all.stream().flatMapToDouble(t -> t.streamD())).setColNames_m(name);
	}

	public Table sortRows(long... byColIndices) 
	{
		checkColIndicesParam(byColIndices);
		return Table.numeric(
				LongStream.range(0, nrows())
				.boxed()
				.sorted((i1, i2) -> Double.compare(data.get(i1), data.get(i2)))
				.mapToDouble(i -> data.get(i)));
	}
}
