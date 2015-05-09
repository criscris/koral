package xyz.koral;

public interface Pitch 
{
	long index();
	long pitchSize();
	int strideSize();
	
	default String getS()
	{
		return getS(0, 0);
	}
	default String getS(long pitchIndex)
	{
		return getS(pitchIndex, 0);
	}
	String getS(long pitchIndex, int strideIndex);
	String[] getStrideS(long pitchIndex);
	
	default float getF()
	{
		return getF(0, 0);
	}
	default float getF(long pitchIndex)
	{
		return getF(pitchIndex, 0);
	}
	float getF(long pitchIndex, int strideIndex);
	float[] getStrideF(long pitchIndex);
	
	default double getD()
	{
		return getD(0, 0);
	}
	default double getD(long pitchIndex)
	{
		return getD(pitchIndex, 0);
	}
	double getD(long pitchIndex, int strideIndex);
	double[] getStrideD(long pitchIndex);
	
	default long getL()
	{
		return getL(0, 0);
	}
	default long getL(long pitchIndex)
	{
		return getL(pitchIndex, 0);
	}
	long getL(long pitchIndex, int strideIndex);
	long[] getStrideL(long pitchIndex);
	
	default int getI()
	{
		return getI(0, 0);
	}
	default int getI(long pitchIndex)
	{
		return getI(pitchIndex, 0);
	}
	int getI(long pitchIndex, int strideIndex);
	int[] getStrideI(long pitchIndex);
}
