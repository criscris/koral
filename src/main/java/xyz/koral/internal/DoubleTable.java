package xyz.koral.internal;

import java.util.function.DoubleBinaryOperator;
import java.util.function.DoubleUnaryOperator;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import java.util.stream.Stream;

import xyz.koral.IO;
import xyz.koral.KoralError;
import xyz.koral.Table;

public abstract class DoubleTable extends AbstractTable 
{
	private static final long serialVersionUID = -7997433253995406376L;
	
	DoubleArray data; // row-wise storage
	
	public Object get(long rowIndex, long colIndex) 
	{
		return getD(rowIndex, colIndex);
	}

	public String getS(long rowIndex, long colIndex) 
	{
		return IO.numberToString(getD(rowIndex, colIndex));
	}

	public int getI(long rowIndex, long colIndex) 
	{
		return (int) getD(rowIndex, colIndex);
	}

	public long getL(long rowIndex, long colIndex) 
	{
		return (long) getD(rowIndex, colIndex);
	}
	
	public Stream<String> streamS(long colIndex) 
	{
		return streamD(colIndex).mapToObj(v -> IO.numberToString(v));
	}
	

	public LongStream streamL(long colIndex) 
	{
		return streamD(colIndex).mapToLong(v -> (long) v);
	}

	public IntStream streamI(long colIndex) 
	{
		return streamD(colIndex).mapToInt(v -> (int) v);
	}
	
	public Stream<String[]> streamRowsS()
	{
		return streamRowsD().map(d -> DoubleStream.of(d).mapToObj(v -> IO.numberToString(v)).toArray(String[]::new));
	}
	
	public Stream<Object[]> streamRows() 
	{
		return streamRowsD().map(d -> DoubleStream.of(d).mapToObj(v -> v).toArray());
	}
	
	public Table set_m(long rowIndex, long colIndex, Object value) 
	{
		if (value instanceof Double) return set_m(rowIndex, colIndex, (double) (Double) value);
		if (value instanceof Long) return set_m(rowIndex, colIndex, (double) (Long) value);
		if (value instanceof Integer) return set_m(rowIndex, colIndex, (double) (Integer) value);
		throw new KoralError("set_m(long rowIndex, long colIndex, Object value): value must be a number.");
	}
	
	public Table set_m(long rowIndex, long colIndex, String value) 
	{
		set_m(rowIndex, colIndex, IO.stringToNumber(value));
		return this;
	}

	public Table set_m(long rowIndex, long colIndex, int value) 
	{
		set_m(rowIndex, colIndex, (double) value);
		return this;
	}

	public Table set_m(long rowIndex, long colIndex, long value)
	{
		set_m(rowIndex, colIndex, (double) value);
		return this;
	} 
	
	public Table applyD(DoubleBinaryOperator op, Table operand2) 
	{
		checkDimensions(operand2);
		return copy().applyD_m(op, operand2);
	}

	public Table applyD_m(DoubleUnaryOperator op) 
	{
		data.apply(op);
		return this;
	}
	
	public Table orderedIndices(long colIndex) 
	{
		checkColIndex(colIndex);
		long offset = colIndex*nrows();
		return orderedIndices((i1, i2) -> Double.compare(data.get(offset + i1), data.get(offset + i2)));
	}

	public Table orderedIndicesDesc(long colIndex) 
	{
		checkColIndex(colIndex);
		long offset = colIndex*nrows();
		return orderedIndices((i1, i2) -> Double.compare(data.get(offset + i2), data.get(offset + i1)));
	}
	
	public boolean isNumeric()
	{
		return true;
	}
}
