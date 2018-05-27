package xyz.koral.compute.config;

public class Param
{
	public DataFormat type;

	public Object val;
	public String uri;
	public String url;
	
	public boolean parallel;
	
	public static Param createJson(Object val)
	{
		Param p = new Param();
		p.type = DataFormat.json;
		p.val = val;
		return p;
	}
	
	public static Param create(DataFormat type, String uri)
	{
		Param p = new Param();
		p.type = type;
		p.uri = uri;
		return p;
	}
}
