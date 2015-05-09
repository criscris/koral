package xyz.koral.internal;

import xyz.koral.Entry;

public class VectorEntry implements Entry
{
	long index;
	final long pitchIndex;
	final int strideSize;
	final int strideStartIndex;
	final DenseVector data;
	
	public VectorEntry(long index, long pitchIndex, int strideSize, int strideStartIndex, DenseVector data) 
	{
		this.index = index;
		this.pitchIndex = pitchIndex;
		this.strideSize = strideSize;
		this.strideStartIndex = strideStartIndex;
		this.data = data;
	}

	public long index() 
	{
		return index;
	}
	
	public void setIndex(long index)
	{
		this.index = index;
	}

	public long pitchIndex() 
	{
		return pitchIndex;
	}

	public int strideSize() 
	{
		return strideSize;
	}

	public String getS(int strideIndex) 
	{
		return data.getS(strideStartIndex + strideIndex);
	}

	public String[] getStrideS() 
	{
		return data.getS(strideStartIndex, strideSize);
	}

	public float getF(int strideIndex) 
	{
		return data.getF(strideStartIndex + strideIndex);
	}

	public float[] getStrideF() 
	{
		return data.getF(strideStartIndex, strideSize);
	}

	public double getD(int strideIndex) 
	{
		return data.getD(strideStartIndex + strideIndex);
	}

	public double[] getStrideD() 
	{
		return data.getD(strideStartIndex, strideSize);
	}

	public int getI(int strideIndex) 
	{
		return data.getI(strideStartIndex + strideIndex);
	}

	public int[] getStrideI() 
	{
		return data.getI(strideStartIndex, strideSize);
	}

	public long getL(int strideIndex) 
	{
		return data.getL(strideStartIndex + strideIndex);
	}

	public long[] getStrideL() 
	{
		return data.getL(strideStartIndex, strideSize);
	}
}
