package xyz.koral.internal;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.DoublePredicate;
import java.util.function.Function;
import java.util.function.IntPredicate;
import java.util.function.LongPredicate;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import java.util.stream.LongStream;
import java.util.stream.Stream;

import xyz.koral.KoralError;
import xyz.koral.Table;

public abstract class AbstractTable implements Table
{
	private static final long serialVersionUID = 6343202228016799112L;

	public static Table header(Stream<List<String>> rows, Function<Stream<List<String>>, Table> body)
	{
		class Header
		{
			List<String> firstRow = null;
		}
		Header h = new Header();
		return body.apply(
				rows
				.filter(row -> {
					if (h.firstRow != null) return true;
					h.firstRow = row;
					return false;
				}))
			.setColNames_m(h.firstRow.toArray(new String[0]));
	}
	
	void checkDimensions(Table other)
	{
		if (ncols() != other.ncols()) throw new KoralError(
				"Dimension mismatch: " + nrows() + "x" + ncols() + " != " + other.nrows() + "x" + other.ncols());
	}
	
	void checkColIndex(long colIndex)
	{
		if (colIndex >= ncols() || colIndex < 0) throw new KoralError(
				"Column index out of bounds: " + colIndex + ". Table size=(" + nrows() + "," + ncols() + ").");
	}
	
	void checkColIndices(long... colIndices)
	{
		if (colIndices == null) throw new KoralError("No column index specified.");
		for (long colIndex : colIndices)
		{
			checkColIndex(colIndex);
		}
	}
	
	/**
	 * may be overridden for better performance
	 */
	Collection<List<Long>> distinctRows(long colIndex)
	{
		Map<Object, List<Long>> groups = new LinkedHashMap<>();
		for (long i=0; i<nrows(); i++)
		{
			Object val = get(i, colIndex);
			List<Long> group = groups.get(val);
			if (group == null)
			{
				group = new ArrayList<>();
				groups.put(val, group);
			}
			group.add((long) i);
		}
		return groups.values();
	}
	
	/**
	 * may be overridden for better performance
	 */
	Collection<List<Long>> distinctRows(long[] colIndices)
	{
		Map<List<Object>, List<Long>> groups = new LinkedHashMap<>();
		long[] rowIndex = new long[1];
		cols(colIndices)
		.streamRows()
		.forEach(row -> 
		{
			List<Object> row_ = Arrays.asList(row);
			List<Long> group = groups.get(row_);
			if (group == null)
			{
				group = new ArrayList<>();
				groups.put(row_, group);
			}
			group.add(rowIndex[0]);
			rowIndex[0]++;
		});
		return groups.values();
	}
	
	public Table aggregateByCols(UnaryOperator<Table> reducer, long... byColIndices) 
	{
		checkColIndices(byColIndices);
		if (byColIndices.length == 0) throw new KoralError("No column index specified.");
		Set<Long> byColIndicesSet = LongStream.of(byColIndices).mapToObj(i -> i).collect(Collectors.toSet());
		if (byColIndicesSet.size() != byColIndices.length) throw new KoralError("Non-unique column indices.");
		if (byColIndices.length == ncols()) throw new KoralError("No column remains for reduction.");
		
		Collection<List<Long>> groups = byColIndices.length == 1 ? distinctRows(byColIndices[0]) : distinctRows(byColIndices);
		Table groupCols = cols(byColIndices).rows(groups.stream().mapToLong(g -> g.get(0)).toArray());
		
		long[] aggColIndices = LongStream.range(0, ncols()).filter(i -> !byColIndicesSet.contains(i)).toArray();
		
		Table reduceCols = cols(aggColIndices);
		Table reducedCols = 
		Table.rowBind( 
		groups
		.stream()
		.map(group -> reducer.apply(reduceCols.rows(group.stream().mapToLong(i -> i).toArray())))
		.collect(Collectors.toList()));

		// inverse column selection to restore original column order
		long[] cols = new long[ncolsI()];
		for (int i=0; i<byColIndices.length; i++) cols[(int) byColIndices[i]] = i;
		for (int i=0; i<aggColIndices.length; i++) cols[(int) aggColIndices[i]] = byColIndices.length + i;
		return Table.colBind(groupCols, reducedCols).cols(cols);
	}
	
	private Table which(long colIndex, LongPredicate acceptRowEntry) 
	{
		checkColIndex(colIndex);
		return Table.numeric(LongStream.range(0, nrows()).filter(acceptRowEntry));
	}
	
	public Table whichD(DoublePredicate op, long colIndex) 
	{
		return which(colIndex, i -> op.test(getD(i, colIndex)));
	}

	public Table whichS(Predicate<String> op, long colIndex) 
	{
		return which(colIndex, i -> op.test(getS(i, colIndex)));
	}

	public Table whichI(IntPredicate op, long colIndex) 
	{
		return which(colIndex, i -> op.test(getI(i, colIndex)));
	}

	public Table whichL(LongPredicate op, long colIndex) 
	{
		return which(colIndex, i -> op.test(getL(i, colIndex)));
	}
	
	Table orderedIndices(Comparator<Long> comparator)
	{
		return Table.numeric(
				LongStream.range(0, nrows())
				.boxed()
				.sorted(comparator)
				.mapToLong(i -> i));
	}
}
