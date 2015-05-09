package xyz.koral.internal;

import java.util.Iterator;
import java.util.List;

import xyz.koral.Array;
import xyz.koral.Entry;
import xyz.koral.QID;

public class SubArray implements Array
{
	Array source;
	DenseLongVector indices;
	
	public SubArray(Array source, DenseLongVector indices)
	{
		this.source = source;
		this.indices = indices;
	}

	public Iterator<Entry> iterator() 
	{
		int[] currentIndex = new int[1];
		currentIndex[0] = 0;
		
		return source.stream().filter(entry -> 
		{
			if (entry.index() > indices.getL(currentIndex[0]))
			{
				currentIndex[0]++;
			}		
			boolean include = entry.index() == indices.getL(currentIndex[0]);
			if (include) ((VectorEntry) entry).setIndex(currentIndex[0]);
			return include;
		}).iterator();
	}

	public QID qid() 
	{
		return source.qid();
	}
	
	public long size() 
	{
		return indices.size();
	}

	public long maxPitchSize() 
	{
		return source.maxPitchSize();
	}

	public int strideSize() 
	{
		return source.strideSize();
	}

	public String strideName(int index) 
	{
		return source.strideName(index);
	}

	public boolean hasEntry(long index, long pitch) 
	{
		return source.hasEntry(indices.getL((int) index), pitch);
	}

	public Entry get(long index, long pitch) 
	{
		VectorEntry v = (VectorEntry) source.get(indices.getL((int) index), pitch);
		if (v == null) return null;
		v.setIndex(index);
		return v;
	}

	public List<Entry> getPitch(long index) 
	{
		List<Entry> entries = source.getPitch(indices.getL((int) index));
		if (entries == null) return null;
		for (Entry e : entries) ((VectorEntry) e).setIndex(index);
		return entries;
	}

	public Class<?> type() 
	{
		return source.type();
	}

	public String typeLiteral() 
	{
		return source.typeLiteral();
	}

}
