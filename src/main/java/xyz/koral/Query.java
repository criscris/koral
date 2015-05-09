package xyz.koral;

public abstract class Query 
{
	Occur occur;
	
	public enum Occur
	{
		MUST,
		SHOULD,
		NOT
	}
	
	public Query()
	{
		this(Occur.MUST);
	}
	
	public Query(Occur occur)
	{
		this.occur = occur;
	}
	
	public Occur getOccur()
	{
		return occur;
	}
}
