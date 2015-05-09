package xyz.koral.internal;

import java.util.Arrays;

public class DenseIntegerVector implements DenseVector
{
	int[] data;
	int size = 0;
	
	public DenseIntegerVector()
	{
		this(DenseVector.DEFAULT_CAPACITY);
	}

	public DenseIntegerVector(int initialCapacity)
	{
		data = new int[initialCapacity];
	}
	
	public final void add(double value)
	{
		add((int) value);
	}
	
	public final void add(String value) 
	{
		add(new Integer(value));
	}

	public final void add(float value) 
	{
		add((int) value);
	}

	public final void add(int value) 
	{
		checkResize(size + 1);
		data[size] = value;
		size++;
	}

	public final void add(long value) 
	{
		add((int) value);
	}
	
	public final String getS(int index) 
	{
		return Integer.toString(data[index]);
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
		return data[index];
	}

	public final long getL(int index) 
	{
		return data[index];
	}
	
	public final void set(int index, String value) 
	{
		set(index, new Integer(value));
	}

	public final void set(int index, float value) 
	{
		set(index, (int) value);
	}
	
	public final void set(int index, double value)
	{
		set(index, (int) value);
	}

	public final void set(int index, int value) 
	{
		checkResize(index + 1);
		data[index] = value;
		size = Math.max(size, index + 1);
	}
	
	public final void addTo(int index, int value) 
	{
		checkResize(index + 1);
		data[index] += value;
		size = Math.max(size, index + 1);
	}

	public final void set(int index, long value) 
	{
		set(index, (int) value);
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
		return binarySearch(new Integer(key));
	}

	public final int binarySearch(float key) 
	{
		return binarySearch((int) key);
	}

	public final int binarySearch(double key) 
	{
		return binarySearch((int) key);
	}
	
	public final int binarySearch(int key) 
	{
		return Arrays.binarySearch(data, 0, size, key);
	}

	public final int binarySearch(long key) 
	{
		return binarySearch((int) key);
	}
	
	public final int binarySearch(String key, int startIndex, int endIndexEx)
	{
		return binarySearch(new Integer(key), startIndex, endIndexEx);
	}
	
	public final int binarySearch(float key, int startIndex, int endIndexEx)
	{
		return binarySearch((int) key, startIndex, endIndexEx);
	}
	public final int binarySearch(double key, int startIndex, int endIndexEx)
	{
		return binarySearch((int) key, startIndex, endIndexEx);
	}
	
	public final int binarySearch(int key, int startIndex, int endIndexEx)
	{
		return Arrays.binarySearch(data, startIndex, endIndexEx, key);
	}
	
	public final int binarySearch(long key, int startIndex, int endIndexEx)
	{
		return binarySearch((int) key, startIndex, endIndexEx);
	}

	public final int size()
	{
		return size;
	}
	
	public Class<?> type() 
	{
		return Integer.class;
	}
	
	public long memorySize()
	{
		return data.length * 4 + 16;
	}
}
