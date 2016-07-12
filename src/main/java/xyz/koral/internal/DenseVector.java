package xyz.koral.internal;

public interface DenseVector 
{
	static final int DEFAULT_CAPACITY = 10;
	
	void add(String value);
	void add(float value);
	void add(double value);
	void add(int value);
	void add(long value);
	
	String getS(int index);
	float getF(int index);
	double getD(int index);
	int getI(int index);
	long getL(int index);
	
	void set(int index, String value);
	void set(int index, float value);
	void set(int index, double value);
	void set(int index, int value);
	void set(int index, long value);
	
	int size();
	void trim();
	
	int binarySearch(String key);
	int binarySearch(float key);
	int binarySearch(double key);
	int binarySearch(int key);
	int binarySearch(long key);
	
	int binarySearch(String key, int startIndex, int endIndexEx);
	int binarySearch(float key, int startIndex, int endIndexEx);
	int binarySearch(double key, int startIndex, int endIndexEx);
	int binarySearch(int key, int startIndex, int endIndexEx);
	int binarySearch(long key, int startIndex, int endIndexEx);
	
	
	default String typeLiteral()
	{
		return "numeric";
	}
	
	default Class<?> type() {
		return String.class;
	}
	
	long memorySize();
	
	default String[] getS(int index, int size) 
	{
		String[] result = new String[size];
		for (int i=0; i<size; i++)
		{
			result[i] = getS(index + i);
		}
		return result;
	}

	default float[] getF(int index, int size) 
	{
		float[] result = new float[size];
		for (int i=0; i<size; i++)
		{
			result[i] = getF(index + i);
		}
		return result;
	}

	
	default double[] getD(int index, int size) 
	{
		double[] result = new double[size];
		for (int i=0; i<size; i++)
		{
			result[i] = getD(index + i);
		}
		return result;
	}

	default int[] getI(int index, int size) 
	{
		int[] result = new int[size];
		for (int i=0; i<size; i++)
		{
			result[i] = getI(index + i);
		}
		return result;
	}

	default long[] getL(int index, int size) 
	{
		long[] result = new long[size];
		for (int i=0; i<size; i++)
		{
			result[i] = getL(index + i);
		}
		return result;
	}
}
