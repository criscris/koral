package xyz.koral;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import xyz.koral.internal.ArrayInfo;
import xyz.koral.internal.DenseVector;
import xyz.koral.internal.DenseVectorFactory;
import xyz.koral.internal.KoralError;
import xyz.koral.internal.VectorEntry;

public class InMemorySparseArray implements WritableArray
{
	ArrayInfo meta;
	
	DenseVector cols; // cols.size() == no of main elements
	DenseVector pitchOffsets; // pitchOffsets.size() == cols.size()
	DenseVector pitches; // pitches.size() == data.size() / stride
	DenseVector values;
	
	
	public InMemorySparseArray(String namespace, String id, Class<?> dataType)
	{
		this(namespace, id, dataType, 1, 1);
	}
	
	public InMemorySparseArray(String namespace, String id, Class<?> dataType, int stride, long maxPitch)
	{
		this(new QID(namespace, id), dataType, stride, maxPitch);
	}
	
	public InMemorySparseArray(QID qid, Class<?> dataType, int stride, long maxPitch)
	{
		this(qid, dataType, defaultStrideNames(stride), maxPitch);
	}
	
	private static String[] defaultStrideNames(int stride)
	{
		String[] strideNames = new String[stride];
		for (int i=0; i<strideNames.length; i++) strideNames[i] = "";
		return strideNames;
	}
	
	public InMemorySparseArray(QID qid, Class<?> dataType, String[] strideNames, long maxPitch)
	{
		this(new ArrayInfo(qid, maxPitch, strideNames, dataType));
	}
	
	public InMemorySparseArray(ArrayInfo meta)
	{
		this.meta = meta;
		cols = DenseVectorFactory.create(Integer.class);
		pitchOffsets = DenseVectorFactory.create(Integer.class);
		pitches = DenseVectorFactory.create(Integer.class);
		values = DenseVectorFactory.create(meta.dataType);
	}
	
	final int lastCol()
	{
		return cols.size() == 0 ? -1 : cols.getI(cols.size() - 1);
	}
	
	final int lastPitch()
	{
		return pitches.size() == 0 ? -1 : pitches.getI(pitches.size() - 1);
	}
	
	final int lastStride()
	{
		return values.size() == 0 ? -1 : (values.size()-1) % meta.stride;
	}
	
	final void increaseBy(int noOfDataEntries)
	{
		if (noOfDataEntries == 0) return;
		if (noOfDataEntries % meta.stride != 0) throw new KoralError("values.length() == " + noOfDataEntries + " is not a multiple of stride=" + meta.stride);
		int strides = noOfDataEntries / meta.stride;
		for (int i=0; i<strides; i++)
		{
			int newPitch = (int) ((lastPitch() + 1) % meta.maxPitch);
			pitches.add(newPitch);
			if (newPitch == 0) // needs new col
			{
				int newCol = lastCol() + 1;
				cols.add(newCol);
				pitchOffsets.add(pitches.size() - 1);
			}
		}
	}
	
	final void increaseTo(int col, int pitch, int noOfDataEntries)
	{
		if (noOfDataEntries == 0) return;
		if (noOfDataEntries % meta.stride != 0) throw new KoralError("values.length() == " + noOfDataEntries + " is not a multiple of stride=" + meta.stride);
		if (pitch < 0 || pitch >= meta.maxPitch) throw new KoralError("pitch index == " + pitch + " is out of bounds [0, " + meta.maxPitch + "[");
		
		int lastCol = lastCol();
		if (col < lastCol) throw new IllegalArgumentException("new col == " + col + " cannot be smaller than last == " + lastCol);
		
		int lastPitch = lastPitch();
		if (lastCol == col)
		{
			if (pitch <= lastPitch) throw new IllegalArgumentException("new pitch == " + pitch + " cannot be smaller or the same than last == " + lastPitch);
		}
		else
		{
			cols.add(col);
			pitchOffsets.add(pitches.size());
		}
		pitches.add(pitch);
		
		increaseBy(noOfDataEntries - meta.stride);
	}
	
	final int pitchSize(int col)
	{
		int colIndex = cols.binarySearch(col);
		if (colIndex < 0) return 0;
		
		int pitchesStart = pitchOffset(colIndex);
		int pitchesEndEx = pitchOffset(colIndex + 1);
		return pitchesEndEx - pitchesStart;
	}
	
	final int pitchOffset(int colIndex)
	{
		return colIndex < pitchOffsets.size() ? pitchOffsets.getI(colIndex) : pitches.size();
	}
	
	final int getStrideStartIndex(int col, int pitch)
	{
		int colIndex = cols.binarySearch(col);
		if (colIndex < 0) return -1;
		
		int pitchesStart = pitchOffsets.getI(colIndex);
		int pitchesEndEx = colIndex < pitchOffsets.size() - 1 ? pitchOffsets.getI(colIndex + 1) : pitches.size(); 
		
		int pitchIndex = pitches.binarySearch(pitch, pitchesStart, pitchesEndEx);
		return pitchIndex * meta.stride;
	}

	/**
	 * TODO: contains potentially unnecessary conversion to string
	 */
	public void accept(Entry t) 
	{
		set(t.index(), t.pitchIndex(), t.getStrideS());
	}

	public QID qid() 
	{
		return meta.qid;
	}

	public long size() 
	{
		return lastCol() + 1;
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
	
	public Class<?> type() 
	{
		return meta.dataType;
	}

	public String typeLiteral() 
	{
		return values.typeLiteral();
	}

	public boolean hasEntry(long index, long pitch) 
	{
		int strideStartIndex = getStrideStartIndex((int) index, (int) pitch);
		return strideStartIndex >= 0;
	}

	public Entry get(long index, long pitch) 
	{
		return new VectorEntry(index, pitch, meta.stride, getStrideStartIndex((int) index, (int) pitch), values);
	}
	
	public List<Entry> getPitch(long index)
	{
		List<Entry> entries = new ArrayList<>();
		
		int ps = pitchSize((int) index);
		for (int i=0; i<ps; i++)
		{
			entries.add(get(index, i));
		}
		
		return entries;
	}

	public Array filter(String query) 
	{
		throw new KoralError("Not implemented: InMemorySparseArray.filter");
	}

	public Array subArray(long fromIndex, long toExIndex) 
	{
		throw new KoralError("Not implemented: InMemorySparseArray.subArray");
	}


	public void add(String... values) 
	{
		increaseBy(values.length);
		for (String v : values)
		{
			this.values.add(v);
		}
	}

	public void add(float... values) 
	{
		increaseBy(values.length);
		for (float v : values)
		{
			this.values.add(v);
		}
	}

	public void add(double... values) 
	{
		increaseBy(values.length);
		for (double v : values)
		{
			this.values.add(v);
		}
	}

	public void add(int... values) 
	{
		increaseBy(values.length);
		for (int v : values)
		{
			this.values.add(v);
		}
	}

	public void add(long... values) 
	{
		increaseBy(values.length);
		for (long v : values)
		{
			this.values.add(v);
		}
	}

	public void set(long index, long pitch, String... values) 
	{
		increaseTo((int) index, (int) pitch, values.length);
		for (String v : values)
		{
			this.values.add(v);
		}
	}

	public void set(long index, long pitch, float... values) 
	{
		increaseTo((int) index, (int) pitch, values.length);
		for (float v : values)
		{
			this.values.add(v);
		}
	}

	public void set(long index, long pitch, double... values) 
	{
		increaseTo((int) index, (int) pitch, values.length);
		for (double v : values)
		{
			this.values.add(v);
		}
	}

	public void set(long index, long pitch, int... values) 
	{
		increaseTo((int) index, (int) pitch, values.length);
		for (int v : values)
		{
			this.values.add(v);
		}
	}

	public void set(long index, long pitch, long... values) 
	{
		increaseTo((int) index, (int) pitch, values.length);
		for (long v : values)
		{
			this.values.add(v);
		}
	}
	
	public Iterator<Entry> iterator() 
	{
		return new Iterator<Entry>()
		{
			int currentIndex = 0; // index in cols or pitchOffsets vector
			int currentPitchIndex = 0; // index in pitches vector
			
			public boolean hasNext() 
			{
				return currentPitchIndex < pitches.size();
			}

			public Entry next() 
			{
				if (currentPitchIndex >= pitches.size()) throw new NoSuchElementException();
				Entry entry = new VectorEntry(cols.getI(currentIndex), pitches.getI(currentPitchIndex), meta.stride, currentPitchIndex * meta.stride, values);
				
				currentPitchIndex++;
				int nextPitch = pitchOffset(currentIndex + 1);
				if (currentPitchIndex >= nextPitch)
				{
					currentIndex++; 
				}
				
				return entry;
			}
		};
	}
	
	public long memorySize()
	{
		return values.memorySize() + cols.memorySize() + pitchOffsets.memorySize() + pitches.memorySize() + 24;
	} 
	
	public long noOfSetValues()
	{
		return values.size();
	}
}


