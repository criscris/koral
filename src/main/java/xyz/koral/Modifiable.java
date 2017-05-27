package xyz.koral;

/**
 * setter methods attempt to convert data to the storage type and may throw a KoralError when unsuccessful
 */
public interface Modifiable 
{	
	Table setColName_m(long colIndex, String colId);

	default Table setColNames_m(long firstColIndex, String... colIds) 
	{
		if (colIds.length == 0) throw new KoralError("No column name specified.");
		Table t = null;
		for (int i=0; i<colIds.length; i++)
		{
			t = setColName_m(firstColIndex + i, colIds[i]);
		}
		return t;
	}
	
	default Table setColNames_m(String... colIds) 
	{
		return setColNames_m(0, colIds);
	}
	
	Table set_m(long rowIndex, long colIndex, Object value);
	Table set_m(long rowIndex, long colIndex, String value);
	Table set_m(long rowIndex, long colIndex, double value);
	Table set_m(long rowIndex, long colIndex, int value);
	Table set_m(long rowIndex, long colIndex, long value);
	
	default Table set_m(long rowIndex, Object value)
	{
		return set_m(rowIndex, 0, value);
	}
	
	default Table set_m(long rowIndex, String value)
	{
		return set_m(rowIndex, 0, value);
	}
	
	default Table set_m(long rowIndex, double value)
	{
		return set_m(rowIndex, 0, value);
	}
	
	default Table set_m(long rowIndex, int value)
	{
		return set_m(rowIndex, 0, value);
	}
	
	default Table set_m(long rowIndex, long value)
	{
		return set_m(rowIndex, 0, value);
	}
}
