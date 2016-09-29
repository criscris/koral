package xyz.koral.internal;

import java.util.ArrayList;
import java.util.Collections;

public class DenseStringVector implements DenseVector
{
	ArrayList<String> data;
	int size = 0;
	long estStringMemorySize = 0;
	
	public DenseStringVector()
	{
		this(DenseVector.DEFAULT_CAPACITY);
	}
	
	final void addToMemorySize(String value)
	{
		if (value != null) estStringMemorySize += value.length()*2 + 38;
	}
	
	final void removeFromMemorySize(String value)
	{
		if (value != null) estStringMemorySize -= value.length()*2 + 38;
	}

	public DenseStringVector(int initialCapacity)
	{
		data = new ArrayList<>(initialCapacity);
	}
	
	public final void add(String value) 
	{
		data.add(value);
		addToMemorySize(value);
	}

	public final void add(float value) 
	{
		add(Float.toString(value));
	}
	
	public final void add(double value)
	{
		add(Double.toString(value));
	}

	public final void add(int value) 
	{
		add(Integer.toString(value));
	}

	public final void add(long value) 
	{
		add(Long.toString(value));
	}
	
	public final String getS(int index) 
	{
		return data.get(index);
	}

	public final float getF(int index) 
	{
		return new Float(data.get(index));
	}

	public final double getD(int index) 
	{
		return new Double(data.get(index));
	}

	public final int getI(int index) 
	{
		Double d = getD(index);
		return d == null ? null : d.intValue();
	}

	public final long getL(int index) 
	{
		return new Long(data.get(index));
	}
	
	public final void set(int index, String value) 
	{
		while (index >= data.size())
		{
			data.add(null);
		}
		
		addToMemorySize(value);
		String old = data.set(index, value);
		removeFromMemorySize(old);
	}

	public final void set(int index, float value) 
	{
		set(index, Float.toString(value));
	}
	
	public final void set(int index, double value)
	{
		set(index, Double.toString(value));
	}

	public final void set(int index, int value) 
	{
		set(index, Integer.toString(value));
	}

	public final void set(int index, long value) 
	{
		set(index, Long.toString(value));
	}
	
	public final void trim()
	{
		data.trimToSize();
	}
	
	public final int binarySearch(String key) 
	{
		return Collections.binarySearch(data, key);
	}

	public final int binarySearch(float key) 
	{
		return binarySearch(Float.valueOf(key));
	}

	public final int binarySearch(double key) 
	{
		return binarySearch(Double.valueOf(key));
	}
	
	public final int binarySearch(int key) 
	{
		return binarySearch(Integer.valueOf(key));
	}

	public final int binarySearch(long key) 
	{
		return binarySearch(Long.valueOf(key));
	}
	
	public final int binarySearch(String key, int startIndex, int endIndexEx)
	{
		int index = Collections.binarySearch(data.subList(startIndex, endIndexEx), key);
		index += index >= 0 ? startIndex : -startIndex;
		return index;
	}
	
	public final int binarySearch(float key, int startIndex, int endIndexEx)
	{
		return binarySearch(Float.valueOf(key), startIndex, endIndexEx);
	}
	public final int binarySearch(double key, int startIndex, int endIndexEx)
	{
		return binarySearch(Double.valueOf(key), startIndex, endIndexEx);
	}
	
	public final int binarySearch(int key, int startIndex, int endIndexEx)
	{
		return binarySearch(Integer.valueOf(key), startIndex, endIndexEx);
	}
	
	public final int binarySearch(long key, int startIndex, int endIndexEx)
	{
		return binarySearch(Long.valueOf(key), startIndex, endIndexEx);
	}
	
	public final int size()
	{
		return data.size();
	}
	
	public String typeLiteral()
	{
		return "string";
	}
	
	public long memorySize()
	{
		return data.size() * 4 + 16 + estStringMemorySize;
	}
}