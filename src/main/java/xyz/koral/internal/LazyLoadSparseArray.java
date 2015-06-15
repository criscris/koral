package xyz.koral.internal;

import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import xyz.koral.Array;
import xyz.koral.Entry;
import xyz.koral.InMemorySparseArray;
import xyz.koral.QID;

public class LazyLoadSparseArray implements Array
{
	ArrayInfo meta;
	String typeInfo;
	Source source;
	long count;
	
	DenseLongVector startIndices;
	ArrayPart[] parts;
	LRUQueue queue;
	
	class ArrayPart implements Unloadable
	{
		InMemorySparseArray loadedArray;
		long firstIndex;
		long lastIndexEx;
		long byteOffset;
		long noOfBytes;
		
		public ArrayPart(long firstIndex, long lastIndexEx, long byteOffset, long noOfBytes) 
		{
			this.firstIndex = firstIndex;
			this.lastIndexEx = lastIndexEx;
			this.byteOffset = byteOffset;
			this.noOfBytes = noOfBytes;
		}

		public long memorySize() 
		{
			return loadedArray != null ? loadedArray.memorySize() : 0;
		}
		
		public void load()
		{
			if (loadedArray != null) return;
			
			loadedArray = new InMemorySparseArray(meta);
			new ArrayStreamReader(new InputStreamReader(source.createInputStream(byteOffset, noOfBytes), XmlDocument.cs), 
					firstIndex, noOfBytes).forEach(entry -> 
			{
				if (entry.index() < lastIndexEx) loadedArray.accept(entry);
			});
		}

		public void unload() 
		{
			loadedArray = null;
		}		
	}
	
	public LazyLoadSparseArray(ArrayInfo meta, String typeInfo, Source source, DenseLongVector startIndices, DenseLongVector byteOffsets, LRUQueue queue)
	{
		this.meta = meta;
		this.typeInfo = typeInfo;
		this.source = source;
		this.count = startIndices.getL(startIndices.size - 1);
		this.startIndices = startIndices;
		
		this.queue = queue;
		parts = new ArrayPart[startIndices.size() - 1];
		for (int i=0; i<parts.length; i++)
		{
			parts[i] = new ArrayPart(startIndices.getL(i), startIndices.getL(i+1), byteOffsets.getL(i), byteOffsets.getL(i+1) - byteOffsets.getL(i));
		}
		System.out.println(meta.qid + ": " + parts.length + " array parts.");
	}
	
	
	public QID qid() 
	{
		return meta.qid;
	}

	public long maxPitchSize() 
	{
		return meta.maxPitch;
	}

	public int strideSize() 
	{
		return meta.stride;
	}

	public String strideName(int index) 
	{
		return meta.strideNames[index];
	}
	
	public long size() 
	{
		return count != 0 ? count : startIndices.getL(startIndices.size() - 1);
	}
	
	public void setSize(long count)
	{
		this.count = count;
	}
	
	public Class<?> type()
	{
		return meta.dataType;
	}

	public String typeLiteral() 
	{
		return typeInfo;
	}
	
	ArrayPart loadArrayPart(int partIndex)
	{
		ArrayPart part = parts[partIndex];
		part.load();
		queue.add(part);
		return part;
	}
	
	ArrayPart getArrayPart(long index)
	{
		if (index < startIndices.getL(0) || index >= count) return null;
		int i = startIndices.binarySearch(index);
		if (i < 0) i = -(i+2);
		if (i < 0) return null;
		return loadArrayPart(Math.min(startIndices.size() - 2, i));
	}
	

	public boolean hasEntry(long index, long pitch) 
	{
		ArrayPart p = getArrayPart(index);
		if (p == null) return false;
		return p.loadedArray.hasEntry(index, pitch);
	}

	
	public Entry get(long index, long pitch) 
	{
		ArrayPart p = getArrayPart(index);
		if (p == null) return null;
		return p.loadedArray.get(index, pitch);
	}
	
	public List<Entry> getPitch(long index)
	{
		ArrayPart p = getArrayPart(index);
		if (p == null) return new ArrayList<Entry>();
		return  p.loadedArray.getPitch(index);
	}
	
	public long pitchSize(long index) 
	{
		ArrayPart p = getArrayPart(index);
		if (p == null) return -1;
		return  p.loadedArray.pitchSize(index);
	}


	public Iterator<Entry> iterator() 
	{
		return new Iterator<Entry>()
		{
			int partIndex = -1;
			Iterator<Entry> iter = null;
			
			public boolean hasNext() 
			{
				if (iter != null)
				{
					if (iter.hasNext()) return true;
				}
				
				while (partIndex + 1 < parts.length)
				{
					partIndex++;
					iter = loadArrayPart(partIndex).loadedArray.iterator();
					if (iter.hasNext()) return true;
				}
				
				iter = null;
				return false;
			}

			public Entry next() 
			{
				if (!hasNext()) return null;
				return iter.next();
			}
		};
	}

	public Source getSource() 
	{
		return source;
	}
}
