package xyz.koral.table;

import java.util.function.DoubleBinaryOperator;
import java.util.function.UnaryOperator;
import java.util.stream.LongStream;

/**
 * column-wise reduction operations.
 * reducing (including min, max, sum, mean, sd, se): (nrows, ncols) -> (1, ncols).
 * aggregating: (nrows, ncols) -> (k, ncols) with 1<=k<=nrows.
 *
 */
public interface Summable extends Readable, Transformable, Indexable
{
	Table reduceRowsD(DoubleBinaryOperator op);
	Table aggregateByCols(UnaryOperator<Table> reducer, long... byColIndices);
	
	default Table min()
	{
		return reduceRowsD((val1, val2) -> Math.min(val1, val2));
	}
	
	default Table max()
	{
		return reduceRowsD((val1, val2) -> Math.max(val1, val2));
	}
	
	default Table sum()
	{
		return reduceRowsD((val1, val2) -> val1 + val2);
	}
	
	default Table mean() 
	{
		return sum().divideBy_m(nrows());
	}
	
	public enum SDType
	{
		Population,
		RandomSample
	}
	default Table sd(SDType type)
	{
		return sub(mean())
				.pow_m(2)
				.sum()
				.divideBy_m(nrows() - (type == SDType.RandomSample ? 1 : 0))
				.sqrt_m();
	}
	
	default Table se(SDType type)
	{
		return sd(type).divideBy_m(Math.sqrt(nrows()));
	}
	
	default Table median()
	{
		long ni2 =  nrows()/2;
		return Table.numeric(LongStream.range(0, ncols()).mapToDouble(col -> {
			long medianIndex = orderedIndices(col).getL(ni2);
			return getD(medianIndex, col);
		})).transpose()
		.setColNames_m(LongStream.range(0, ncols()).mapToObj(col -> getColName(col)).toArray(n -> new String[n]));
	}
}
