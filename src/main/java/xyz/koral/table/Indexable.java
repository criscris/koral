package xyz.koral.table;

import java.util.function.DoublePredicate;
import java.util.function.IntPredicate;
import java.util.function.LongPredicate;
import java.util.function.Predicate;

/**
 * When no column is specified, column 0 is used.
 *
 */
public interface Indexable 
{
	Table whichS(Predicate<String> op, long colIndex);
	Table whichD(DoublePredicate op, long colIndex);
	Table whichI(IntPredicate op, long colIndex);
	Table whichL(LongPredicate op, long colIndex);
	
	default Table whichS(Predicate<String> op)
	{
		return whichS(op, 0);
	}
	
	default Table whichD(DoublePredicate op)
	{
		return whichD(op, 0);
	}
	
	default Table whichI(IntPredicate op)
	{
		return whichI(op, 0);
	}
	
	default Table whichL(LongPredicate op)
	{
		return whichL(op, 0);
	}
	
	Table orderedIndices(long colIndex);
	Table orderedIndicesDesc(long colIndex);
	
	default Table orderedIndices()
	{
		return orderedIndices(0);
	}
	
	default Table orderedIndicesDesc()
	{
		return orderedIndicesDesc(0);
	}
}
