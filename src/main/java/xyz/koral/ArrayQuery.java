package xyz.koral;


public class ArrayQuery extends Query
{
	QID arrayID;
	String textQuery;
	double min;
	double maxEx;
	
	public ArrayQuery(Occur occur, QID arrayID, String textQuery)
	{
		super(occur);
		init(arrayID, textQuery, Double.NaN, Double.NaN);
	}
	
	public ArrayQuery(QID arrayID, String textQuery)
	{
		init(arrayID, textQuery, Double.NaN, Double.NaN);
	}
	
	public ArrayQuery(Occur occur, QID arrayID, double min, double maxEx)
	{
		super(occur);
		init(arrayID, null, min, maxEx);
	}

	
	public ArrayQuery(QID arrayID, double min, double maxEx)
	{
		init(arrayID, null, min, maxEx);
	}
	
	void init(QID arrayID, String textQuery, double min, double maxEx)
	{
		this.arrayID = arrayID;
		this.textQuery = textQuery;
		this.min = min;
		this.maxEx = maxEx;
	}
	
	public QID getArrayID()
	{
		return arrayID;
	}

	public String getTextQuery() 
	{
		return textQuery;
	}

	public double getMin() 
	{
		return min;
	}

	public double getMaxEx() 
	{
		return maxEx;
	}
}
