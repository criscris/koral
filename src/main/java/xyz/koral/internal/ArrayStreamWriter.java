package xyz.koral.internal;

import java.io.IOException;
import java.io.Writer;
import java.util.function.Consumer;

import xyz.koral.Entry;

public class ArrayStreamWriter implements Consumer<Entry>
{
	Writer writer;
	ArrayStream as = new ArrayStream();
	
	
	boolean addLineReturns;
	long charsWrittenSinceLastLineReturn = 0;
	static final long lineReturnAfter = 10000;
	
	public ArrayStreamWriter(Writer writer)
	{
		this(writer, true);
	}
	
	public ArrayStreamWriter(Writer writer, boolean addLineReturns)
	{
		this.writer = writer;
		this.addLineReturns = addLineReturns;
	}
	
	private final void write(char c) throws IOException
	{
		writer.write(c);
		charsWrittenSinceLastLineReturn++;
	}
	
	private final void write(String s) throws IOException
	{
		writer.write(s);
		charsWrittenSinceLastLineReturn += s.length();
	}

	long lastCol = 0;
	long lastPitch = -1;
	public void accept(Entry t) 
	{
		//System.out.println("entry " + t.index() + " " + t.pitchIndex() + " " + Arrays.toString(t.getStrideS()));
		
		// check if not sequentially forward 
		long col = t.index();
		if (col < lastCol) throw new IllegalArgumentException("new col == " + col + " cannot be smaller than last == " + lastCol);
		long pitch = t.pitchIndex();
		if (col == lastCol && pitch <= lastPitch) throw new IllegalArgumentException("new pitch == " + pitch + " cannot be smaller or the same than last == " + lastPitch);
		
		// write absolute col position iff
		// a) the col to write is skips cols between the last col OR
		// b) it's the very first entry and col index 1
		boolean writeColPos = (col > lastCol + 1) || (lastPitch == -1 && col == 1);
		
		// write absolute pitch position iff
		// a) same col as last and pitch to write skips pitches between last pitch OR
		// b) a new col is started and pitch is not zero
		boolean writePitchPos = (col == lastCol && pitch > lastPitch + 1) || (col > lastCol && pitch > 0);
		
		try
		{
		
			if (writeColPos)
			{
				write(ArrayStream.colPositionSep);
				write(Long.toString(col));
				write(ArrayStream.colPositionSep);
			}
			// if there is a new col but no absolute col position
			else if (col > lastCol)
			{
				write(ArrayStream.colSep);
			}
			
			if (writePitchPos)
			{
				write(ArrayStream.pitchPositionSep);
				write(Long.toString(pitch));
				write(ArrayStream.pitchPositionSep);
			}
			// no absolute col/pitch positions but not first pitch element
			if (!(writeColPos || writePitchPos) && pitch > 0)
			{
				write(ArrayStream.pitchSep);
			}
		
			// write strides
			write(as.escaper.escapeContent(t.getS(0).trim()));
			for (int i=1; i<t.strideSize(); i++)
			{
				write(ArrayStream.strideSep);
				write(as.escaper.escapeContent(t.getS(i).trim()));
			}
			if (addLineReturns && charsWrittenSinceLastLineReturn >= lineReturnAfter)
			{
				write("\n\t");
				charsWrittenSinceLastLineReturn = 0;
			}
		}
		catch (IOException ex)
		{
			throw new KoralError(ex);
		}
		
		lastCol = col;
		lastPitch = pitch;
	}
}
