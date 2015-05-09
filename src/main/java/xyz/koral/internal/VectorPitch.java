package xyz.koral.internal;

import xyz.koral.Pitch;

public class VectorPitch implements Pitch
{
	long index;
	long pitchSize;
	int strideSize;
	
	int pitchStartIndex;
	DenseVector data;

	public VectorPitch(long index, long pitchSize, int strideSize, int pitchStartIndex, DenseVector data) 
	{
		this.index = index;
		this.pitchSize = pitchSize;
		this.strideSize = strideSize;
		this.pitchStartIndex = pitchStartIndex;
		this.data = data;
	}

	public long index() 
	{
		return index;
	}

	public long pitchSize() 
	{
		return pitchSize;
	}

	public int strideSize() 
	{
		return strideSize;
	}

	public String getS(long pitchIndex, int strideIndex) 
	{
		return data.getS((int) (pitchStartIndex + pitchIndex*strideSize + strideIndex));
	}

	public String[] getStrideS(long pitchIndex) 
	{
		return data.getS((int) (pitchStartIndex + pitchIndex*strideSize ), strideSize);
	}

	public float getF(long pitchIndex, int strideIndex) 
	{
		return data.getF((int) (pitchStartIndex + pitchIndex*strideSize + strideIndex));
	}

	public float[] getStrideF(long pitchIndex) 
	{
		return data.getF((int) (pitchStartIndex + pitchIndex*strideSize ), strideSize);
	}

	public double getD(long pitchIndex, int strideIndex) 
	{
		return data.getF((int) (pitchStartIndex + pitchIndex*strideSize + strideIndex));
	}

	public double[] getStrideD(long pitchIndex) 
	{
		return data.getD((int) (pitchStartIndex + pitchIndex*strideSize ), strideSize);
	}

	public long getL(long pitchIndex, int strideIndex) 
	{
		return data.getL((int) (pitchStartIndex + pitchIndex*strideSize + strideIndex));
	}

	public long[] getStrideL(long pitchIndex) 
	{
		return data.getL((int) (pitchStartIndex + pitchIndex*strideSize ), strideSize);
	}

	public int getI(long pitchIndex, int strideIndex) 
	{
		return data.getI((int) (pitchStartIndex + pitchIndex*strideSize + strideIndex));
	}

	public int[] getStrideI(long pitchIndex) 
	{
		return data.getI((int) (pitchStartIndex + pitchIndex*strideSize ), strideSize);
	}

}
