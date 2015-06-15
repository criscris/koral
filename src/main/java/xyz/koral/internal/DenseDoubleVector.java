package xyz.koral.internal;

import java.util.Arrays;

public class DenseDoubleVector implements DenseVector
{
	double[] data;
	int size = 0;
	
	public DenseDoubleVector()
	{
		this(DenseVector.DEFAULT_CAPACITY);
	}

	public DenseDoubleVector(int initialCapacity)
	{
		data = new double[initialCapacity];
	}
	
	public final void add(double value)
	{
		checkResize(size + 1);
		data[size] = value;
		size++;
	}
	
	public final void add(String value) 
	{
		double d = value == null ? Double.NaN : (value.length() == 0 ? 0.0 : new Double(value));
		add(d);
	}

	public final void add(float value) 
	{
		add((double) value);
	}

	public final void add(int value) 
	{
		add((double) value);
	}

	public final void add(long value) 
	{
		add((double) value);
	}
	
	public final void addTo(int index, double value) 
	{
		checkResize(index + 1);
		data[index] += value;
		size = Math.max(size, index + 1);
	}
	
	public final String getS(int index) 
	{
		return Double.toString(data[index]);
	}

	public final float getF(int index) 
	{
		return (float) data[index];
	}

	public final double getD(int index) 
	{
		return data[index];
	}

	public final int getI(int index) 
	{
		return (int) data[index];
	}

	public final long getL(int index) 
	{
		return (long) data[index];
	}

	public final void set(int index, String value) 
	{
		set(index, new Double(value));
	}

	public final void set(int index, float value) 
	{
		set(index, (double) value);
	}
	
	public final void set(int index, double value)
	{
		checkResize(index + 1);
		data[index] = value;
		size = Math.max(size, index + 1);
	}

	public final void set(int index, int value) 
	{
		set(index, (double) value);
	}

	public final void set(int index, long value) 
	{
		set(index, (double) value);
	}
	
	private final void checkResize(int neededSize)
	{
		if (neededSize > data.length)
		{
			int newCapacity = Math.max(neededSize, data.length + (data.length >> 1)); // grow by 1.5
			data = Arrays.copyOf(data, newCapacity);
		}
	}
	
	public void trim()
	{
		if (data.length > size)
		{
			data = Arrays.copyOf(data, size);
		}
	}
	
	public final int binarySearch(String key) 
	{
		return binarySearch(new Double(key));
	}

	public final int binarySearch(float key) 
	{
		return binarySearch((double) key);
	}

	public final int binarySearch(double key) 
	{
		return Arrays.binarySearch(data, 0, size, key);
	}
	
	public final int binarySearch(int key) 
	{
		return binarySearch((double) key);
	}

	public final int binarySearch(long key) 
	{
		return binarySearch((double) key);
	}
	
	public final int binarySearch(String key, int startIndex, int endIndexEx)
	{
		return binarySearch(new Double(key), startIndex, endIndexEx);
	}
	
	public final int binarySearch(float key, int startIndex, int endIndexEx)
	{
		return binarySearch((double) key, startIndex, endIndexEx);
	}
	
	public final int binarySearch(double key, int startIndex, int endIndexEx)
	{
		return Arrays.binarySearch(data, startIndex, endIndexEx, key);
	}
	
	public final int binarySearch(int key, int startIndex, int endIndexEx)
	{
		return binarySearch((double) key, startIndex, endIndexEx);
	}
	
	public final int binarySearch(long key, int startIndex, int endIndexEx)
	{
		return binarySearch((double) key, startIndex, endIndexEx);
	}

	
	public final int size()
	{
		return size;
	}
	
	public Class<?> type() 
	{
		return Double.class;
	}
	
	public long memorySize()
	{
		return data.length * 8 + 16;
	}
}
