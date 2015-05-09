package xyz.koral.internal;

public class ArrayStream 
{
	public final static char colSep = '|';
	public final static char pitchSep = '`';
	public final static char strideSep = '~';
	public final static char colPositionSep = '\\'; // java needs escaping
	public final static char pitchPositionSep = '^';
	public XmlEscaper escaper = new XmlEscaper();
}
