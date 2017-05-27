package xyz.koral.internal;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.DoubleBinaryOperator;
import java.util.function.DoubleUnaryOperator;
import java.util.stream.Collectors;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import java.util.stream.Stream;

import xyz.koral.IO;
import xyz.koral.KoralError;
import xyz.koral.Table;

/**
 * Two dimensional dense data structure. may grow in both dimensions.
 *
 */
public class Mat extends AbstractTable
{
	private static final long serialVersionUID = -8483996827168461745L;
	
	List<List<Object>> data; // list of columns
	int nrows; // == data.get(0..ncols-1).size()
	Identifiers colNames;

	public Mat(int nrows, int ncols)
	{
		this.nrows = nrows;
		data = new ArrayList<>(ncols);
		for (int i=0; i<ncols; i++)
		{
			List<Object> col = new ArrayList<>(nrows);
			for (int j=0; j<nrows; j++) col.add(null);
			data.add(col);
		}
		colNames = new Identifiers();
	}
	
	public Mat(Stream<Object> column)
	{
		data = new ArrayList<>(1);
		data.add(column.collect(Collectors.toList()));
		nrows = data.get(0).size();
		colNames = new Identifiers();
	}
	
	public Mat(List<List<Object>> columns)
	{
		this(columns, new Identifiers());
	}
	
	private Mat(int nrows, int ncols, Identifiers colNames)
	{
		this(nrows, ncols);
		this.colNames = colNames;
	}
	
	private Mat(List<List<Object>> data, Identifiers colNames)
	{
		this.nrows = data.size() == 0 ? 0 : data.get(0).size();
		this.data = data;
		this.colNames = colNames;
	}
	
	/**
	 * entries are not cloned.
	 */
	public Table copy() 
	{
		List<List<Object>> c = new ArrayList<>(data.size());
		for (List<Object> col : data) c.add(new ArrayList<>(col));	
		return new Mat(c, new Identifiers(colNames));
	}
	
	public final long nrows() 
	{
		return nrows;
	}

	public final long ncols() 
	{
		return data.size();
	}

	public Object get(long rowIndex, long colIndex) 
	{
		return data.get((int) colIndex).get((int) rowIndex);
	}

	public String getS(long rowIndex, long colIndex) 
	{
		return get(rowIndex, colIndex).toString();
	}

	public double getD(long rowIndex, long colIndex) 
	{
		return IO.stringToNumber(getS(rowIndex, colIndex));
	}

	public int getI(long rowIndex, long colIndex) 
	{
		return (int) getD(rowIndex, colIndex);
	}

	public long getL(long rowIndex, long colIndex) 
	{
		return (long) getD(rowIndex, colIndex);
	}

	public Stream<String> streamS(long colIndex) 
	{
		checkColIndex(colIndex);
		return data.get((int) colIndex).stream().map(o -> o.toString());
	}

	public DoubleStream streamD(long colIndex)
	{
		return streamS(colIndex).mapToDouble(s -> IO.stringToNumber(s));
	}

	public LongStream streamL(long colIndex) 
	{
		return streamD(colIndex).mapToLong(v -> (long) v);
	}

	public IntStream streamI(long colIndex) 
	{
		return streamD(colIndex).mapToInt(v -> (int) v);
	}
	
	public Stream<Object[]> streamRows() 
	{
		return IntStream.range(0, nrows).mapToObj(i -> {
			Object[] row = new Object[data.size()];
			for (int j=0; j<data.size(); j++) row[j] = data.get(j).get(i);
			return row;
		});
	}

	public Stream<String[]> streamRowsS() 
	{
		return IntStream.range(0, nrows).mapToObj(i -> {
			String[] row = new String[data.size()];
			for (int j=0; j<data.size(); j++) row[j] = data.get(j).get(i).toString();
			return row;
		});
	}

	public Stream<double[]> streamRowsD() 
	{
		return streamRowsS().map(row -> Stream.of(row).mapToDouble(s -> IO.stringToNumber(s)).toArray());
	}

	public String getColName(long colIndex) 
	{
		checkColIndex(colIndex);
		return colNames.get(colIndex);
	}
	
	public long getColIndex(String colName)
	{
		return colNames.getIndex(colName);
	}

	public Table setColName_m(long colIndex, String colId) 
	{
		checkColIndex(colIndex);
		colNames.set(colIndex, colId);
		return this;
	}

	
	private final void checkSize(long rowIndex, long colIndex)
	{
		if (rowIndex < 0 || colIndex < 0 || rowIndex >= Integer.MAX_VALUE - 5 || colIndex >= Integer.MAX_VALUE - 5)
			throw new KoralError("Invalid index specified: (" + rowIndex + "," + colIndex + ")");
		if (rowIndex < nrows && colIndex < ncols()) return;
		
		if (rowIndex >= nrows)
		{
			int rowsToAdd = (int) (rowIndex + 1 - nrows);
			for (List<Object> col : data)
			{
				for (int i=0; i<rowsToAdd; i++) col.add(null);
			}
			nrows += rowsToAdd;
		}
		if (colIndex >= ncols())
		{
			int colsToAdd = (int) (colIndex + 1 - ncols());
			for (int i=0; i<colsToAdd; i++)
			{
				List<Object> col = new ArrayList<>(nrows);
				for (int j=0; j<nrows; j++) col.add(null);
				data.add(col);
			}
		}
	}

	public Table set_m(long rowIndex, long colIndex, Object value) 
	{
		checkSize(rowIndex, colIndex);
		data.get((int) colIndex).set((int) rowIndex, value);
		return this;
	}

	public Table set_m(long rowIndex, long colIndex, String value) 
	{
		return set_m(rowIndex, colIndex, (Object) value);
	}

	public Table set_m(long rowIndex, long colIndex, double value) 
	{
		return set_m(rowIndex, colIndex, (Double) value);
	}

	public Table set_m(long rowIndex, long colIndex, int value) 
	{
		return set_m(rowIndex, colIndex, (Integer) value);
	}

	public Table set_m(long rowIndex, long colIndex, long value) 
	{
		return set_m(rowIndex, colIndex, (Long) value);
	}

	public Table applyD(DoubleUnaryOperator op) 
	{
		throw new KoralError("applyD() not allowed non-numeric data.");
	}

	public Table applyD(DoubleUnaryOperator op, long... colIndices) 
	{
		throw new KoralError("applyD() not allowed non-numeric data.");
	}

	public Table applyD(DoubleBinaryOperator op, Table operand2) 
	{
		throw new KoralError("applyD() not allowed non-numeric data.");
	}

	public Table applyD_m(DoubleUnaryOperator op) 
	{
		throw new KoralError("applyD_m() not allowed non-numeric data.");
	}

	public Table applyD_m(DoubleUnaryOperator op, long... colIndices) 
	{
		throw new KoralError("applyD_m() not allowed non-numeric data.");
	}

	public Table applyD_m(DoubleBinaryOperator op, Table operand2) 
	{
		throw new KoralError("applyD_m() not allowed non-numeric data.");
	}

	public Table reduceRowsD(DoubleBinaryOperator op) 
	{
		throw new KoralError("reduceRowsD() not allowed non-numeric data.");
	}
	
	Collection<List<Long>> distinctRows(long colIndex)
	{
		Map<Object, List<Long>> groups = new LinkedHashMap<>();
		List<Object> col = data.get((int) colIndex);
		for (int i=0; i<nrows; i++)
		{
			Object val = col.get(i);
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
	 * requires entries to implement Comparable (String and number objects have it).
	 * 
	 * what to do with null entries?
	 */
	@SuppressWarnings("unchecked")
	public Table orderedIndices(long colIndex) 
	{
		checkColIndex(colIndex);
		List<Object> col = data.get((int) colIndex);
		return orderedIndices((i1, i2) -> ((Comparable<Object>) col.get((int) (long) i1)).compareTo(col.get((int) (long) i2)));
	}
	
	@SuppressWarnings("unchecked")
	public Table orderedIndicesDesc(long colIndex) 
	{
		checkColIndex(colIndex);
		List<Object> col = data.get((int) colIndex);
		return orderedIndices((i1, i2) -> ((Comparable<Object>) col.get((int) (long) i2)).compareTo(col.get((int) (long) i1)));
	}


	public Table transpose() 
	{
		Mat t = new Mat(data.size(), nrows);
		for (int j=0; j<data.size(); j++)
		{
			List<Object> col = data.get(j);
			for (int i=0; i<nrows; i++)
			{
				Object o = col.get(i);
				t.data.get(i).set(j, o);
			}
		}
		return t;
	}

	public Table rows(long... rowIndices)
	{
		Mat m = new Mat(rowIndices.length, data.size(), new Identifiers(colNames));
		for (int j=0; j<data.size(); j++)
		{
			List<Object> col = data.get(j);
			List<Object> col_ = m.data.get(j);
			for (int i=0; i<rowIndices.length; i++)
			{
				col_.set(i, col.get((int) rowIndices[i]));
			}
		}
		return m;
	}

	public Table cols(long... colIndices) 
	{
		checkColIndices(colIndices);
		List<List<Object>> c = new ArrayList<>(colIndices.length);
		for (long colIndex : colIndices) 
		{
			c.add(new ArrayList<>(data.get((int) colIndex)));	
		}
		Mat m = new Mat(c, new Identifiers());
		
		Set<String> colNameSet = new HashSet<>();
		for (int i=0; i<colIndices.length; i++) 
		{
			String colName = getColName(colIndices[i]);
			if (colName != null && colNameSet.add(colName)) m.setColName_m(i, colName);
		}
		
		return m;
	}

	public Table rowBind(Table... others) 
	{
		if (others == null || others.length == 0) return copy();
		for (Table o : others) 
			if (o.ncols() != ncols()) 
				throw new KoralError("rowBind(): number of columns mismatch: " + ncols() + " != " + o.ncols());
		
		List<Table> all = new ArrayList<>();
		all.add(this);
		all.addAll(Arrays.asList(others));
		
		long totalRows = all.stream().mapToLong(table -> table.nrows()).sum();
		if (totalRows >= Integer.MAX_VALUE - 5) throw new KoralError("Mat cannot store " + totalRows + " rows.");
		
		Mat m = new Mat((int) totalRows, data.size(), new Identifiers(colNames));
		for (int j=0; j<data.size(); j++)
		{
			List<Object> col = m.data.get(j);
			int rowOffset = 0;
			for (Table table : all)
			{
				for (int i=0; i<table.nrows(); i++)
				{
					col.set(rowOffset, table.get(i, j));
					rowOffset++;
				}
			}
		}
		return m;
	}
	
	public boolean isNumeric()
	{
		return false;
	}
}
