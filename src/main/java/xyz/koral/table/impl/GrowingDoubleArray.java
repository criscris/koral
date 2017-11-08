package xyz.koral.table.impl;

import java.util.Arrays;
import java.util.function.DoubleUnaryOperator;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;

import xyz.koral.KoralError;

public class GrowingDoubleArray implements DoubleArray
{
	private static final long serialVersionUID = 2434530095920074476L;

	public static final int DEFAULTCAPACITY = 10;
	
	double[] data;
	int size = 0;
	
	public GrowingDoubleArray()
	{
		this(DEFAULTCAPACITY);
	}

	public GrowingDoubleArray(int initialCapacity)
	{
		data = new double[initialCapacity];
		size = 0;
	}
	
	public GrowingDoubleArray(double[] values)
	{
		data = values;
		size = values.length;
	}
	
	public final long size()
	{
		return size;
	}
	
	public DoubleArray copy()
	{
		trim();
		return new GrowingDoubleArray(Arrays.copyOf(data, size));
	}
	
	public DoubleArray copyAndApply(DoubleUnaryOperator op)
	{
		GrowingDoubleArray c = new GrowingDoubleArray(size);
		c.size = size;
		for (int i=0; i<size; i++) c.data[i] = op.applyAsDouble(data[i]);
		return c;
	}
	
	public final void add(double value)
	{
		checkResize(size + 1);
		data[size] = value;
		size++;
	}
	
	public final void add(double... values) 
	{
		if (values == null) return;
		checkResize(size + values.length);
		for (int i=0; i<values.length; i++) data[size + i] = values[i];
		size += values.length;
	}
	
	public final double get(long index) 
	{
		return data[(int) index];
	}
	
	public final void set(long index, double value)
	{
		int index_ = (int) index;
		checkResize(index_ + 1);
		data[index_] = value;
		size = Math.max(size, index_ + 1);
	}
	
	public final void set(long index, double... values) 
	{
		if (values == null) return;
		int index_ = (int) index;
		checkResize(index_ + values.length);
		for (int i=0; i<values.length; i++) data[index_ + i] = values[i];
		size = Math.max(size, index_ + values.length);
	}
	
	private final void checkResize(int neededSize)
	{
		if (neededSize > data.length)
		{
			int newCapacity = Math.max(neededSize, data.length + (data.length >> 1)); // grow by 1.5
			data = Arrays.copyOf(data, newCapacity);
		}
	}
	
	public void apply(DoubleUnaryOperator op)
	{
		for (int i=0; i<size; i++) data[i] = op.applyAsDouble(data[i]);
	}
	
	public void apply(DoubleUnaryOperator op, long offset, long count)
	{
		int lastEx = (int) (offset + count);
		for (int i=(int) offset; i<lastEx; i++) data[i] = op.applyAsDouble(data[i]);
	}
	
	public void trim()
	{
		if (data.length > size)
		{
			data = Arrays.copyOf(data, size);
		}
	}

	public DoubleStream stream() 
	{
		trim();
		return DoubleStream.of(data);
	}
	
	public DoubleStream stream(long offset, long count)
	{
		if (offset == 0 && count == size) return stream();
		if (offset + count > size) throw new KoralError("Out of bounds: " + (offset + count) + " > " + size);
		return IntStream.range((int) offset, (int) (offset + count)).mapToDouble(i -> data[i]);
	}
}
