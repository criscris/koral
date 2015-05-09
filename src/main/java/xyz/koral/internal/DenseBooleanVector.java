package xyz.koral.internal;


public class DenseBooleanVector implements DenseVector
{
	final int bitsPerEntry = 32;
	DenseIntegerVector data; // stored as 32 boolean bits per integer value
	int size = 0;
	
	public DenseBooleanVector()
	{
		this(DenseVector.DEFAULT_CAPACITY);
	}

	public DenseBooleanVector(int initialCapacity)
	{
		data = new DenseIntegerVector(initialCapacity/bitsPerEntry+1);
	}
	
	public final boolean getB(int index)
	{
		int integerIndex = index/bitsPerEntry;
		int bitIndex = index - integerIndex*bitsPerEntry;
		
		int intValue = data.getI(integerIndex);
		int bitValue = (intValue >>> bitIndex) & 1;
		return bitValue == 1;
	}
	
	public final void add(boolean value)
	{
		set(size, value);
	}
	
	public final void set(int index, boolean value)
	{
		int integerIndex = index/bitsPerEntry;
		int bitIndex = index - integerIndex*bitsPerEntry;
		int val = 0;
		if (integerIndex < data.size)
		{
			int previousVal = data.getI(integerIndex);
			val = value ? previousVal | (1 << bitIndex) : previousVal & ~(1 << bitIndex);
		}
		else if (value)
		{
			val = 1 << bitIndex;
		}
		data.set(integerIndex, val);
		size = Math.max(size, index + 1);
	}
	
	
	public final void add(double value)
	{
		add(value == 1);
	}
	
	public final void add(String value) 
	{
		add(value != null && value.length() > 0);
	}

	public final void add(float value) 
	{
		add(value == 1);
	}

	public final void add(int value) 
	{
		add(value == 1);
	}

	public final void add(long value) 
	{
		add(value == 1);
	}
	
	public final String getS(int index) 
	{
		return getB(index) ? "1" : "0";
	}

	public final float getF(int index) 
	{
		return getB(index) ? 1 : 0;
	}

	public final double getD(int index) 
	{
		return getB(index) ? 1 : 0;
	}

	public final int getI(int index) 
	{
		return  getB(index) ? 1 : 0;
	}

	public final long getL(int index) 
	{
		return getB(index) ? 1 : 0;
	}
	
	public final void set(int index, String value) 
	{
		set(index, value != null && value.length() > 0);
	}

	public final void set(int index, float value) 
	{
		set(index, value == 1);
	}
	
	public final void set(int index, double value)
	{
		set(index, value == 1);
	}

	public final void set(int index, int value) 
	{
		set(index, value == 1);
	}

	public final void set(int index, long value) 
	{
		set(index, value == 1);
	}
	
	public final void trim()
	{
		data.trim();
	}
	
	public final int binarySearch(String key) 
	{
		throw new KoralError("binarySearch not implemented for boolean vetors");
	}

	public final int binarySearch(float key) 
	{
		throw new KoralError("binarySearch not implemented for boolean vetors");
	}

	public final int binarySearch(double key) 
	{
		throw new KoralError("binarySearch not implemented for boolean vetors");
	}
	
	public final int binarySearch(int key) 
	{
		throw new KoralError("binarySearch not implemented for boolean vetors");
	}

	public final int binarySearch(long key) 
	{
		throw new KoralError("binarySearch not implemented for boolean vetors");
	}
	
	public final int binarySearch(String key, int startIndex, int endIndexEx)
	{
		throw new KoralError("binarySearch not implemented for boolean vetors");
	}
	
	public final int binarySearch(float key, int startIndex, int endIndexEx)
	{
		throw new KoralError("binarySearch not implemented for boolean vetors");
	}
	public final int binarySearch(double key, int startIndex, int endIndexEx)
	{
		throw new KoralError("binarySearch not implemented for boolean vetors");
	}
	
	public final int binarySearch(int key, int startIndex, int endIndexEx)
	{
		throw new KoralError("binarySearch not implemented for boolean vetors");
	}
	
	public final int binarySearch(long key, int startIndex, int endIndexEx)
	{
		throw new KoralError("binarySearch not implemented for boolean vetors");
	}

	public final int size()
	{
		return size;
	}
	
	public Class<?> type() 
	{
		return Boolean.class;
	}
	
	public long memorySize()
	{
		return data.memorySize() + 8;
	}
}
