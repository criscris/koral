package xyz.koral.internal;

import java.util.ArrayList;
import java.util.List;
import java.util.function.DoubleUnaryOperator;
import java.util.stream.DoubleStream;

import xyz.koral.KoralError;

public class GrowingBigDoubleArray implements DoubleArray
{
	private static final long serialVersionUID = 1589201822392682341L;

	public static final int DEFAULTCHUNKSIZE = 236557360; // 42 array increases, starting with initialCapacity=10 and 1.5 times increase size = size + (size >> 1)
	
	final int initialChunkCapacity;
	final int chunkSize;
	List<DoubleArray> chunks = new ArrayList<>();
	long size;
	
	public GrowingBigDoubleArray()
	{
		this(0, GrowingDoubleArray.DEFAULTCAPACITY, DEFAULTCHUNKSIZE);
	}
	
	public GrowingBigDoubleArray(long size)
	{
		this(size, GrowingDoubleArray.DEFAULTCAPACITY, DEFAULTCHUNKSIZE);
	}
	
	GrowingBigDoubleArray(long size, int initialChunkCapacity, int chunkSize)
	{
		this.initialChunkCapacity = initialChunkCapacity;
		this.chunkSize = chunkSize;
		if (size > 0) set(size, 0.0);
	}
	
	public DoubleArray copy()
	{
		GrowingBigDoubleArray c = new GrowingBigDoubleArray(0, initialChunkCapacity, chunkSize);
		c.size = size;
		for (DoubleArray chunk : chunks) c.chunks.add(chunk.copy());
		return c;
	}
	
	public DoubleArray copyAndApply(DoubleUnaryOperator op)
	{
		GrowingBigDoubleArray c = new GrowingBigDoubleArray(0, initialChunkCapacity, chunkSize);
		c.size = size;
		for (DoubleArray chunk : chunks) c.chunks.add(chunk.copyAndApply(op));
		return c;
	}
	
	public final long size()
	{
		return size;
	}
	
	public final double get(long index)
	{
		if (index >= size) throw new KoralError("ArrayIndexOutOfBoundsException index=" + index + " size=" + size);
		return chunks.get((int) (index / chunkSize)).get((int) (index % chunkSize));
	}
	
    private final void ensureCapacity(long index) 
    {
    	if (index < size) return;
		int neededChunks = (int) (index / chunkSize) + 1;
		for (int i=chunks.size(); i<neededChunks; i++) 
		{
			if (i > 0)  
			{
				DoubleArray prev = chunks.get(i - 1);
				if (prev.size() < chunkSize)
				{
					// previous chunk needs to be filled before new chunk added
					prev.set(chunkSize - 1, 0.0);
				}
				else if (prev.size() == chunkSize)
				{
					prev.trim();
				}
			}
			chunks.add(new GrowingDoubleArray(initialChunkCapacity));
		}
    }
    
	public final void add(double value)
	{
		ensureCapacity(size);
		chunks.get(chunks.size() - 1).add(value);
		size++;
	}
	
	public final void add(double... values)
	{
		if (values == null) return;
		ensureCapacity(size - 1 + values.length);
		for (int i=0; i<values.length; i++)
		{
			chunks.get((int) (size / chunkSize)).add(values[i]);
			size++;
		}
	}

	public final void set(long index, double value)
	{
		ensureCapacity(index);
		chunks.get((int) (index / chunkSize)).set((int) (index % chunkSize), value);
		size = Math.max(size, index + 1);
	}
	
	public void set(long index, double... values) 
	{
		if (values == null) return;
		ensureCapacity(index - 1 + values.length);
		for (int i=0; i<values.length; i++)
		{
			long j = index + i;
			chunks.get((int) (j / chunkSize)).set((int) (j % chunkSize), values[i]);
		}
		size = Math.max(size, index + values.length);
	}
	
	public void apply(DoubleUnaryOperator op)
	{
		for (DoubleArray d : chunks)
		{
			d.apply(op);
		}
	}
	
	public void apply(DoubleUnaryOperator op, long offset, long count)
	{
		chunkOffsetsCount(offset, count)
		.forEach(c -> chunks.get((int) c[0]).apply(op, c[1], c[2]));
	}
	
	public void trim()
	{
		if (chunks.size() == 0) return;
		chunks.get(chunks.size() - 1).trim();
	}

	public DoubleStream stream() 
	{
		return chunks.stream().flatMapToDouble(chunk -> chunk.stream());
	}
	
	/**
	 * determines which local ranges of which chunks are within the specified global range 
	 * @return [chunkIndex, offsetWithinChunk, countWithinChunk]+
	 */
	final List<long[]> chunkOffsetsCount(long offset, long count)
	{
		List<long[]> c = new ArrayList<>();
		long lastIndexInc = offset + count - 1;
		int firstChunk = (int) (offset / chunkSize);
		int lastChunkInc = (int) (lastIndexInc / chunkSize);
		
		long firstIndex = offset % chunkSize;
		if (firstChunk == lastChunkInc)
		{
			c.add(new long[] { firstChunk, firstIndex, count });
			return c;
		}
		
		// "half" start chunk
		c.add(new long[] { firstChunk, firstIndex, chunkSize - firstIndex });
		
		// full middle chunks if exist
		for (int i=firstChunk + 1; i<lastChunkInc; i++)
		{
			c.add(new long[] { i, 0, chunkSize });
		}
		
		// "half" end chunk
		long within = (offset + count) % chunkSize;
		c.add(new long[] { lastChunkInc, 0,  within == 0 ? chunkSize : within}); // due to modulo, zero should actually be chunkSize
		
		return c;
	}
	
	public DoubleStream stream(long offset, long count)
	{
		if (offset + count > size) throw new KoralError("Out of bounds: " + (offset + count) + " > " + size);
		return chunkOffsetsCount(offset, count).stream().flatMapToDouble(c -> chunks.get((int) c[0]).stream(c[1], c[2]));
	}
}
