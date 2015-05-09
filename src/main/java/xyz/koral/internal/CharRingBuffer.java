package xyz.koral.internal;

public class CharRingBuffer
{
	char[] buf;
	int pointer = 0;
	int length = 0;
	
	public CharRingBuffer(int maxLength)
	{
		buf = new char[maxLength];
	}
	
	public final void add(char c)
	{
		buf[pointer] = c;
		length = Math.min(buf.length, length + 1);
		pointer = (pointer + 1) % buf.length;
	}
	
	public final void add(String text)
	{
		for (int i=0; i<text.length(); i++) add(text.charAt(i));
	}
	
	public final char get(int index)
	{
		return buf[(buf.length + pointer - length + index) % buf.length];
	}
	
	public final boolean startsWith(char[] sequence)
	{
		if (length < sequence.length) return false;
		for (int i=sequence.length-1; i>=0; i--)
		{
			if (sequence[i] != get(i)) return false;
		}
		return true;
	}
}
