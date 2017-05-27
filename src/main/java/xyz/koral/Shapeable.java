package xyz.koral;

import java.util.stream.LongStream;
import java.util.stream.Stream;

public interface Shapeable extends Readable 
{
	Table transpose();
	
	Table rows(long... rowIndices);
	
	default Table rows(LongStream rowIndices)
	{
		return rows(rowIndices.toArray());
	}
	
	default Table rows(Table rowIndices)
	{
		return rows(rowIndices.streamL());
	}
	
	default Table cols(String... colIDs)
	{
		return cols(Stream.of(colIDs).mapToLong(id -> getColIndex(id)));
	}
	
	Table cols(long... colIndices);
	
	default Table cols(LongStream colIndices)
	{
		return cols(colIndices.toArray());
	}
	
	default Table cols(Table colIndices)
	{
		return cols(colIndices.streamL());
	}

	Table rowBind(Table... others);
	
	Table sortRows(long... byColIndices);
	
	default Table sortRows()
	{
		return sortRows(0L);
	}
}
