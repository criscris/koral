package xyz.koral.internal;

import java.util.Arrays;

public class DenseLongVector implements DenseVector
{
	long[] data;
	int size = 0;
	
	public DenseLongVector()
	{
		this(DenseVector.DEFAULT_CAPACITY);
	}

	public DenseLongVector(int initialCapacity)
	{
		data = new long[initialCapacity];
	}
	
	public final void add(double value)
	{
		add((long) value);
	}
	
	public final void add(String value) 
	{
		add(new Long(value));
	}

	public final void add(float value) 
	{
		add((long) value);
	}

	public final void add(int value) 
	{
		add((long) value);
	}

	public final void add(long value) 
	{
		checkResize(size + 1);
		data[size] = value;
		size++;
	}
	
	public final String getS(int index) 
	{
		return Long.toString(data[index]);
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
		return data[index];
	}
	
	public final void set(int index, String value) 
	{
		set(index, new Long(value));
	}

	public final void set(int index, float value) 
	{
		set(index, (long) value);
	}
	
	public final void set(int index, double value)
	{
		set(index, (long) value);
	}

	public final void set(int index, int value) 
	{
		set(index, (long) value);
	}
	
	public final void addTo(int index, long value) 
	{
		checkResize(index + 1);
		data[index] += value;
		size = Math.max(size, index + 1);
	}

	public final void set(int index, long value) 
	{
		checkResize(index + 1);
		data[index] = value;
		size = Math.max(size, index + 1);
	}
	
	private final void checkResize(int neededSize)
	{
		if (neededSize > data.length)
		{
			int newCapacity = Math.max(neededSize, data.length + (data.length >> 1)); // grow by 1.5
			data = Arrays.copyOf(data, newCapacity);
		}
	}
	
	public final void trim()
	{
		if (data.length > size)
		{
			data = Arrays.copyOf(data, size);
		}
	}
	
	public final int binarySearch(String key) 
	{
		return binarySearch(new Long(key));
	}

	public final int binarySearch(float key) 
	{
		return binarySearch((long) key);
	}

	public final int binarySearch(double key) 
	{
		return binarySearch((long) key);
	}
	
	public final int binarySearch(int key) 
	{
		return binarySearch((long) key);
	}

	public final int binarySearch(long key) 
	{
		return Arrays.binarySearch(data, 0, size, key);
	}
	
	public final int binarySearch(String key, int startIndex, int endIndexEx)
	{
		return binarySearch(new Long(key), startIndex, endIndexEx);
	}
	
	public final int binarySearch(float key, int startIndex, int endIndexEx)
	{
		return binarySearch((long) key, startIndex, endIndexEx);
	}
	public final int binarySearch(double key, int startIndex, int endIndexEx)
	{
		return binarySearch((long) key, startIndex, endIndexEx);
	}
	
	public final int binarySearch(int key, int startIndex, int endIndexEx)
	{
		return binarySearch((long) key, startIndex, endIndexEx);
	}
	
	public final int binarySearch(long key, int startIndex, int endIndexEx)
	{
		return Arrays.binarySearch(data, startIndex, endIndexEx, key);
	}

	public final int size()
	{
		return size;
	}
	
	public Class<?> type() 
	{
		return Long.class;
	}
	
	public long memorySize()
	{
		return data.length * 8 + 16;
	}
}
