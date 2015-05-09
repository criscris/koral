package xyz.koral.internal;

import java.util.Arrays;

public class DenseCharVector implements DenseVector
{
	char[] data;
	int size = 0;
	
	public DenseCharVector()
	{
		this(DenseVector.DEFAULT_CAPACITY);
	}

	public DenseCharVector(int initialCapacity)
	{
		data = new char[initialCapacity];
	}
	
	public final void add(double value)
	{
		add((char) value);
	}
	
	public final void add(String value) 
	{
		checkResize(size + value.length());
		for (int i=0; i<value.length(); i++)
		{
			data[size] = value.charAt(i);
			size++;
		}
	}
	
	public final void add(char value)
	{
		checkResize(size + 1);
		data[size] = value;
		size++;
	}

	public final void add(float value) 
	{
		add((char) value);
	}

	public final void add(int value) 
	{
		add((char) value);
	}

	public final void add(long value) 
	{
		add((char) value);
	}
	
	public final String getS(int index) 
	{
		return Character.toString(data[index]);
	}

	public final float getF(int index) 
	{
		return data[index];
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
		checkResize(index + value.length());
		for (int i=0; i<value.length(); i++)
		{
			data[index + i] = value.charAt(i);
		}
		size = Math.max(size, index + value.length());
	}
	
	public final void set(int index, char value) 
	{
		checkResize(index + 1);
		data[index] = value;
		size = Math.max(size, index + 1);
	}

	public final void set(int index, float value) 
	{
		set(index, (char) value);
	}
	
	public final void set(int index, double value)
	{
		set(index, (char) value);
	}

	public final void set(int index, int value) 
	{
		set(index, (char) value);
	}

	public final void set(int index, long value) 
	{
		set(index, (char) value);
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
		return binarySearch(key.charAt(0));
	}
	
	public final int binarySearch(char key) 
	{
		return Arrays.binarySearch(data, 0, size, key);
	}

	public final int binarySearch(float key) 
	{
		return binarySearch((char) key);
	}

	public final int binarySearch(double key) 
	{
		return binarySearch((char) key);
	}
	
	public final int binarySearch(int key) 
	{
		return binarySearch((char) key);
	}

	public final int binarySearch(long key) 
	{
		return binarySearch((int) key);
	}
	
	public final int binarySearch(String key, int startIndex, int endIndexEx)
	{
		return binarySearch(key.charAt(0), startIndex, endIndexEx);
	}
	
	public final int binarySearch(char key, int startIndex, int endIndexEx)
	{
		return Arrays.binarySearch(data, startIndex, endIndexEx, key);
	}
	
	public final int binarySearch(float key, int startIndex, int endIndexEx)
	{
		return binarySearch((char) key, startIndex, endIndexEx);
	}
	public final int binarySearch(double key, int startIndex, int endIndexEx)
	{
		return binarySearch((char) key, startIndex, endIndexEx);
	}
	
	public final int binarySearch(int key, int startIndex, int endIndexEx)
	{
		return binarySearch((char) key, startIndex, endIndexEx);
	}
	
	public final int binarySearch(long key, int startIndex, int endIndexEx)
	{
		return binarySearch((int) key, startIndex, endIndexEx);
	}

	public final int size()
	{
		return size;
	}

	public String toString()
	{
		return new String(data, 0, size);
	}

	public Class<?> type() 
	{
		return Character.class;
	}
	
	public long memorySize()
	{
		return data.length * 2 + 16;
	}
}