package xyz.koral;


public class CompositeQuery extends Query
{
	Query[] queries;
	
	public CompositeQuery(Occur occur, Query... queries)
	{
		super(occur);
	}
	
	public CompositeQuery(Query... queries)
	{
		this.queries = queries;
	}

	public Query[] getQueries() 
	{
		return queries;
	}
}
