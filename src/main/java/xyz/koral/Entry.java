package xyz.koral;

public interface Entry 
{
	long index();
	long pitchIndex();
	int strideSize();
	
	default String getS()
	{
		return getS(0);
	}
	String getS(int strideIndex);
	String[] getStrideS();
	
	default float getF()
	{
		return getF(0);
	}
	float getF(int strideIndex);
	float[] getStrideF();
	
	default double getD()
	{
		return getD(0);
	}
	double getD(int strideIndex);
	double[] getStrideD();
	
	default int getI()
	{
		return getI(0);
	}
	int getI(int strideIndex);
	int[] getStrideI();
	
	default long getL()
	{
		return getL(0);
	}
	long getL(int strideIndex);
	long[] getStrideL();
	
//	default <T> T get(Class<T> clazz)
//	{
//		return get(clazz, 0);
//	}
//	<T> T get(Class<T> clazz, int strideIndex);
//	<T> T[] getStride(Class<T> clazz);
}
