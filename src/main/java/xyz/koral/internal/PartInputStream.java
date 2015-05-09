package xyz.koral.internal;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

public class PartInputStream extends FilterInputStream
{
	long bytesLeftToRead;
	
	public PartInputStream(InputStream in, long offset, long maxBytes) throws IOException 
	{
		super(in);
		long skipped = 0;
		while (skipped < offset)
		{
			skipped += in.skip(offset - skipped);
		}
		bytesLeftToRead = maxBytes;
	}

	public int read() throws IOException 
	{
		return bytesLeftToRead-- <= 0 ? -1 : super.read();
	}

	public int read(byte[] b, int off, int len) throws IOException 
	{
		len = (int) Math.min(len, bytesLeftToRead);
		if (len <= 0) return -1;
		int read = super.read(b, off, len);
		bytesLeftToRead -= read;
		return read;
	}
}
