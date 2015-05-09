package xyz.koral.internal;

import xyz.koral.QID;

public class ArrayInfo
{
	public final QID qid;
	
	public final long maxPitch;
	
	public final int stride;
	public final String[] strideNames;
	
	public final Class<?> dataType;
	
	public ArrayInfo(QID qid, long maxPitch, String[] strideNames, Class<?> dataType) 
	{
		if (strideNames.length < 1) throw new KoralError("stride=" + strideNames.length + ". needs to be >= 1.");
		this.qid = qid;
		this.maxPitch = maxPitch;
		this.stride = strideNames.length;
		this.strideNames = strideNames;
		this.dataType = dataType;
	}
}
