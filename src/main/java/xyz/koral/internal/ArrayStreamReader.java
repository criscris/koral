package xyz.koral.internal;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.NoSuchElementException;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import xyz.koral.Entry;

public class ArrayStreamReader implements Iterable<Entry>
{
	Reader reader;
	boolean isClosed = false;
	
	static final int bufSize = 200000;
	char[] buf;
	ArrayStream as = new ArrayStream();
	
	public ArrayStreamReader(String text)
	{
		reader = new StringReader(text);
		buf = new char[4096];
		state = new StreamState();
		currentConsumer = new EntryConsumer(state);
	}
	
	public ArrayStreamReader(Reader reader)
	{
		this(reader, 0, bufSize);
	}
	
	public ArrayStreamReader(Reader reader, long firstIndex, long maxBytes)
	{
		this.reader = new BufferedReader(reader);
		buf = new char[(int) Math.min(bufSize, maxBytes)];
		state = new StreamState();
		state.index = firstIndex;
		currentConsumer = new EntryConsumer(state);
	}
	
	enum State
	{
		entry,
		colPosition,
		pitchPosition
	}
	
	class StreamState
	{
		long index = 0;
		long pitchIndex = 0;
		int localBufIndex = 0;
		State charConsumeState;
	}
	StreamState state;

	interface CharConsumer
	{
		void consume(char c);
		void end();
		int length();
	}
	
	class NumberConsumer implements CharConsumer
	{
		DenseCharVector value = new DenseCharVector(20);
		long result;

		public void consume(char c) 
		{
			value.add(c);
		}
		
		public void end()
		{
			result = new Long(value.toString());
		}
		
		public int length()
		{
			return value.size();
		}
	}
	
	class EntryConsumer implements CharConsumer, Entry
	{
		long index;
		long pitchIndex;
		
		DenseCharVector d = new DenseCharVector();
		ArrayList<String> entries = new ArrayList<>();
		
		public EntryConsumer(StreamState state)
		{
			index = state.index;
			pitchIndex = state.pitchIndex;
		}
		
		public void consume(char c) 
		{
			if (d == null) return;
			
			if (c == ArrayStream.strideSep)
			{
				addEntry();
				d = new DenseCharVector();
			}
			else
			{
				d.add(c);
			}
		}
		
		public int length()
		{
			return entries.size() > 0 ? 1 : d.toString().trim().length();
		}
		
		private void addEntry()
		{
			entries.add(as.escaper.unescape(d.toString().trim()));
		}
		
		public void end()
		{
			addEntry();
			d = null;
			entryBuffer.add(this);
		}
		
		public long index() 
		{
			return index;
		}

		public long pitchIndex() 
		{
			return pitchIndex;
		}

		public int strideSize() 
		{
			return entries.size();
		}

		public String getS(int strideIndex) 
		{
			return entries.get(strideIndex);
		}

		public String[] getStrideS() 
		{
			return entries.toArray(new String[0]);
		}

		public double getD(int strideIndex) 
		{
			String v = entries.get(strideIndex);
			return v.length() == 0 ? 0 : new Double(v);
		}

		public double[] getStrideD() 
		{
			double[] stride = new double[entries.size()];
			for (int i=0; i<entries.size(); i++) stride[i] = getD(i);
			return stride;
		}

		public float getF(int strideIndex) 
		{
			String v = entries.get(strideIndex);
			return v.length() == 0 ? 0 : new Float(v);
		}

		public float[] getStrideF() 
		{
			float[] stride = new float[entries.size()];
			for (int i=0; i<entries.size(); i++) stride[i] = getF(i);
			return stride;
		}

		public int getI(int strideIndex) 
		{
			String v = entries.get(strideIndex);
			return v.length() == 0 ? 0 : new Integer(v);
		}

		public int[] getStrideI() 
		{
			int[] stride = new int[entries.size()];
			for (int i=0; i<entries.size(); i++) stride[i] = getI(i);
			return stride;
		}

		public long getL(int strideIndex) 
		{
			String v = entries.get(strideIndex);
			return v.length() == 0 ? 0 : new Long(v);
		}

		public long[] getStrideL() 
		{
			long[] stride = new long[entries.size()];
			for (int i=0; i<entries.size(); i++) stride[i] = getL(i);
			return stride;
		}
	}
	LinkedList<Entry> entryBuffer = new LinkedList<Entry>();
	CharConsumer previousConsumer = null;
	CharConsumer currentConsumer;
	
	private void readMore()
	{
		try
		{
			while (entryBuffer.size() == 0 && !isClosed)
			{
				int readChars = reader.read(buf);
				
				if (readChars == -1)
				{
					if (currentConsumer != null) currentConsumer.end();
					currentConsumer = null;
					reader.close();
					isClosed = true;
					return; // end of stream, no more entries
				}
				
				for (int i=0; i<readChars; i++)
				{
					char c = buf[i];
					
					switch (c)
					{
					case ArrayStream.colSep:
						currentConsumer.end();
						state.index++;
						state.pitchIndex = 0;
						currentConsumer = new EntryConsumer(state);
						break;
					case ArrayStream.pitchSep:
						currentConsumer.end();
						state.pitchIndex++;
						currentConsumer = new EntryConsumer(state);
						break;
					case ArrayStream.colPositionSep:
						if (currentConsumer instanceof NumberConsumer)
						{
							currentConsumer.end();
							long newIndex = ((NumberConsumer) currentConsumer).result;
							if (newIndex <= state.index) throw new IllegalArgumentException("new col == " + newIndex + " cannot be smaller or the same than last == " + state.index);
							state.index = newIndex;
							state.pitchIndex = 0;
							currentConsumer = new EntryConsumer(state);
						}
						else
						{
							if ((state.index > 0 || state.pitchIndex > 0) || currentConsumer.length() > 0) currentConsumer.end();
							currentConsumer = new NumberConsumer();
						}
						break;
					case ArrayStream.pitchPositionSep: 
						if (currentConsumer instanceof NumberConsumer)
						{
							currentConsumer.end();
							long newPitchIndex = ((NumberConsumer) currentConsumer).result;
							if (newPitchIndex <= state.pitchIndex) throw new IllegalArgumentException("new pitch == " + newPitchIndex + " cannot be smaller or the same than last == " + state.pitchIndex);
							state.pitchIndex = newPitchIndex;
							currentConsumer = new EntryConsumer(state);
						}
						else
						{
							if (state.pitchIndex > 0 || currentConsumer.length() > 0) currentConsumer.end();
							currentConsumer = new NumberConsumer();
						}
						break;
					default: currentConsumer.consume(c);
					}
				}
			}
		
		}
		catch (IOException ex)
		{
			throw new KoralError(ex);
		}
	}

	public Iterator<Entry> iterator() 
	{
		return new Iterator<Entry>() 
		{
			public boolean hasNext() 
			{
				readMore();
				return entryBuffer.size() > 0;
			}

			public Entry next() 
			{
				readMore();
				if (entryBuffer.size() == 0) throw new NoSuchElementException();
				return entryBuffer.removeFirst();
			}
		};
	}
	
    public Stream<Entry> stream()
    {
        return StreamSupport.stream(spliterator(), false);
    }
}

