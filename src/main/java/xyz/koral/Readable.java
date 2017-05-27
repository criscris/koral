package xyz.koral;

import java.util.stream.DoubleStream;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import java.util.stream.Stream;

/**
 * getter methods attempt to convert data to the requested type and may throw a KoralError when unsuccessful
 */
public interface Readable 
{
	/**
	 * @return number of rows
	 */
	long nrows();
	
	/**
	 * @return number of columns
	 */
	long ncols();
	
	/**
	 * 
	 * only save to call when nrows() <= Integer.MAX_VALUE
	 */
	default int nrowsI()
	{
		return (int) nrows();
	}
	
	/**
	 * only save to call when ncols() <= Integer.MAX_VALUE
	 */
	default int ncolsI()
	{
		return (int) ncols();
	}
	
	Object get(long rowIndex, long colIndex);
	String getS(long rowIndex, long colIndex);
	double getD(long rowIndex, long colIndex);
	int getI(long rowIndex, long colIndex);
	long getL(long rowIndex, long colIndex);
	
	
	default Object get()
	{
		return get(0L, 0L);
	}
	
	default String getS()
	{
		return getS(0L, 0L);
	}
	
	default double getD()
	{
		return getD(0L, 0L);
	}
	
	default int getI()
	{
		return getI(0L, 0L);
	}
	
	default long getL()
	{
		return getL(0L, 0L);
	}
	
	default Object get(long rowIndex)
	{
		return get(rowIndex, 0);
	}
	
	default String getS(long rowIndex)
	{
		return getS(rowIndex, 0);
	}
	
	default double getD(long rowIndex)
	{
		return getD(rowIndex, 0);
	}
	
	default int getI(long rowIndex)
	{
		return getI(rowIndex, 0);
	}
	
	default long getL(long rowIndex)
	{
		return getL(rowIndex, 0);
	}
	
	Stream<String> streamS(long colIndex);
	DoubleStream streamD(long colIndex);
	
	
	LongStream streamL(long colIndex);
	IntStream streamI(long colIndex);
	
	default Stream<String> streamS()
	{
		return streamS(0L);
	}
	
	default DoubleStream streamD()
	{
		return streamD(0L);
	}
	
	default LongStream streamL()
	{
		return streamL(0L);
	}
	
	default IntStream streamI()
	{
		return streamI(0L);
	}
	
	default String[] toArrayS(long colIndex)
	{
		return streamS(colIndex).toArray(String[]::new);
	}
	
	default double[] toArrayD(long colIndex)
	{
		return streamD(colIndex).toArray();
	}
	
	default int[] toArrayI(long colIndex)
	{
		return streamI(colIndex).toArray();
	}
	
	default long[] toArrayL(long colIndex)
	{
		return streamL(colIndex).toArray();
	}
	
	default String[] toArrayS()
	{
		return toArrayS(0L);
	}
	
	default double[] toArrayD()
	{
		return toArrayD(0L);
	}
	
	default int[] toArrayI()
	{
		return toArrayI(0);
	}
	
	default long[] toArrayL()
	{
		return toArrayL(0);
	}
	
	Stream<Object[]> streamRows();
	Stream<String[]> streamRowsS();
	Stream<double[]> streamRowsD();

	default Stream<int[]> streamRowsI() 
	{
		return streamRowsD().map(d -> DoubleStream.of(d).mapToInt(v -> (int) v).toArray());
	}

	default Stream<long[]> streamRowsL() 
	{
		return streamRowsD().map(d -> DoubleStream.of(d).mapToLong(v -> (long) v).toArray());
	}
	
	/**
	 * @return column name
	 */
	String getColName(long colIndex);
	long getColIndex(String colName);
	
	Table copy();
	
	boolean isNumeric();
}
