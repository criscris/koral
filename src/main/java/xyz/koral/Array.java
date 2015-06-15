package xyz.koral;

import java.util.List;

import xyz.koral.internal.StreamIterable;

public interface Array extends StreamIterable<Entry>
{
	QID qid();
	
	long size();
	
	
	long pitchSize(long index);
	long maxPitchSize();
	
	int strideSize();
	String strideName(int index);
	
	default boolean hasEntry(long index)
	{
		return hasEntry(index, 0);
	}
	boolean hasEntry(long index, long pitch);
	
	default Entry get(long index)
	{
		return get(index, 0);
	}
	Entry get(long index, long pitch);
	List<Entry> getPitch(long index);
	
	Class<?> type();
	String typeLiteral();
}
