package xyz.koral;

import java.util.function.DoubleBinaryOperator;
import java.util.function.UnaryOperator;

/**
 * column-wise reduction operations.
 * reducing (including min, max, sum, mean, sd, se): (nrows, ncols) -> (1, ncols).
 * aggregating: (nrows, ncols) -> (k, ncols) with 1<=k<=nrows.
 *
 */
public interface Summable extends Readable, Transformable
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
}
