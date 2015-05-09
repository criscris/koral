package xyz.koral.internal;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class UTF8ByteCountReader 
{
	int bufSize = 16384;
	public char[] buf = new char[bufSize];
	
	private boolean lastWasHighSurrogate = false;
	private long byteCount = 0;
	
	public long[] offsets = new long[bufSize];
	boolean computeOffsets = true;
	
	BufferedReader reader;
	
	public UTF8ByteCountReader(InputStream is, boolean computeOffsets)
	{
		this.computeOffsets = computeOffsets;
		reader = new BufferedReader(new InputStreamReader(is, XmlDocument.cs));
	}
	
	/**
	 * 
	 * @param 
	 * @return number of bytes read into buffer
	 */
	public int read() throws IOException
	{
		int readChars = reader.read(buf);
		if (readChars <= 0) return readChars;
		
		if (computeOffsets)
		{
			for (int i=0; i<readChars; i++)
			{	
				char c = buf[i];
				offsets[i] = byteCount;

				if (!lastWasHighSurrogate)
				{
					if (c <= 0x7F) byteCount++;
					else if (c <= 0x7FF) byteCount += 2;
					else if (Character.isHighSurrogate(c)) 
					{
						byteCount += 4;
						lastWasHighSurrogate = true;
					} 
					else byteCount += 3;
				}
				else lastWasHighSurrogate = false;
			}
		}
		return readChars;
	}
	
	public void close() throws IOException
	{
		reader.close();
	}
}
