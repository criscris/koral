package xyz.koral.table.impl;

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
import java.util.stream.DoubleStream;
import java.util.stream.LongStream;
import java.util.stream.Stream;

import xyz.koral.KoralError;
import xyz.koral.table.Table;

/**
 * fixed-size 2D dimensional double data. size is only limited by available memory (roughly 8 byte * ncols * nrows) 
 * row-wise storage
 */
public class DoubleMat extends DoubleTable
{
	private static final long serialVersionUID = -1903654725177227461L;
	
	final long nrows;
	final long ncols;
	Identifiers colNames;
	
	public DoubleMat(long nrows, long ncols)
	{
		this.nrows = nrows;
		this.ncols = ncols;
		long size = nrows * ncols;
		if (size <= 1000000000L)
		{
			data =  new GrowingDoubleArray((int) size);
		}
		else
		{
			data =  new GrowingBigDoubleArray(size);
		}
		colNames = new Identifiers();
	}
	
	private DoubleMat(long nrows, long ncols, Identifiers colNames)
	{
		this(nrows, ncols);
		this.colNames = colNames;
	}
	
	private DoubleMat(long nrows, long ncols, DoubleArray data, Identifiers colNames)
	{
		this.nrows = nrows;
		this.ncols = ncols;
		this.data = data;
		this.colNames = colNames;
	}
	
	public Table copy() 
	{
		return new DoubleMat(nrows, ncols, data.copy(), new Identifiers(colNames));
	}

	public long nrows() 
	{
		return nrows;
	}

	public long ncols() 
	{
		return ncols;
	}
	
	private final void checkBounds(long rowIndex, long colIndex)
	{
		if (rowIndex >= nrows || colIndex >= ncols || rowIndex < 0 || colIndex < 0)
			throw new KoralError("Table index out of bounds at (" 
		+ rowIndex + "," + colIndex + "). Table size=(" + nrows + "," + ncols + ").");
	}
	
	private final long index(long rowIndex, long colIndex)
	{
		return colIndex * nrows + rowIndex;
	}

	public double getD(long rowIndex, long colIndex) 
	{
		checkBounds(rowIndex, colIndex);
		return data.get(index(rowIndex, colIndex));
	}

	public DoubleStream streamD(long colIndex) 
	{
		checkColIndex(colIndex);
		return data.stream(colIndex * nrows, nrows);
	}

	public Stream<double[]> streamRowsD() 
	{
		if (ncols > Integer.MAX_VALUE - 5) throw new KoralError("not possible for table size=(" + nrows + "," + ncols + ").");
		return LongStream.range(0, nrows).mapToObj(i -> {
			double[] row = new double[(int) ncols];
			for (int j=0; j<ncols; j++) row[j] = data.get(index(i, j));
			return row;
		});
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

	public Table set_m(long rowIndex, long colIndex, double value) 
	{
		checkBounds(rowIndex, colIndex);
		data.set(index(rowIndex, colIndex), value);
		return this;
	}

	public Table applyD(DoubleUnaryOperator op) 
	{
		return new DoubleMat(nrows, ncols, data.copyAndApply(op), new Identifiers(colNames));
	}
	
	private final void checkColIndicesUnique(long... colIndices)
	{
		Arrays.sort(colIndices);
		for (int i=1; i<colIndices.length; i++)
		{
			if (colIndices[i] == colIndices[i-1])
				throw new KoralError("colIndices not unique: " + colIndices[i]);
		}
	}

	public Table applyD(DoubleUnaryOperator op, long... colIndices) 
	{
		checkColIndices(colIndices);
		checkColIndicesUnique(colIndices);
		return copy().applyD(op, colIndices);
	}

	public Table applyD_m(DoubleUnaryOperator op, long... colIndices) 
	{
		checkColIndices(colIndices);
		checkColIndicesUnique(colIndices);
		for (long colIndex : colIndices)
		{
			data.apply(op, colIndex * nrows, nrows);
		}
		return this;
	}

	public Table applyD_m(DoubleBinaryOperator op, Table operand2) 
	{
		checkDimensions(operand2);
		long index = 0;
		for (long j=0; j<ncols; j++)
		{
			for (long i=0; i<nrows; i++)
			{
				data.set(index, op.applyAsDouble(data.get(index), operand2.getD(i % operand2.nrows(), j)));
				index++;
			}
		}
		return this;
	}

	public Table reduceRowsD(DoubleBinaryOperator op) 
	{
		if (nrows == 0) throw new KoralError("reduceRowsD() needs at least one row.");
		if (nrows == 1) return copy();
		DoubleMat result = new DoubleMat(1, ncols, new Identifiers(colNames));
		for (long i=0; i<ncols; i++)
		{
			result.set_m(0, i, streamD(i).reduce(op).getAsDouble());
		}
		return result;
	}
	
	Collection<List<Long>> distinctRows(long colIndex)
	{
		Map<Double, List<Long>> groups = new LinkedHashMap<>();
		long offset = colIndex * nrows;
		for (long i=0; i<nrows; i++)
		{
			double val = data.get(offset + i);
			List<Long> group = groups.get(val);
			if (group == null)
			{
				group = new ArrayList<>();
				groups.put(val, group);
			}
			group.add(i);
		}
		return groups.values();
	}
	
	Collection<List<Long>> distinctRows(long[] colIndices)
	{
		Map<String, List<Long>> groups = new LinkedHashMap<>();
		long[] rowIndex = new long[1];
		cols(colIndices)
		.streamRowsD()
		.map(row -> Arrays.toString(row))
		.forEach(row -> 
		{
			List<Long> group = groups.get(row);
			if (group == null)
			{
				group = new ArrayList<>();
				groups.put(row, group);
			}
			group.add(rowIndex[0]);
			rowIndex[0]++;
		});
		return groups.values();
	}


	public Table transpose() 
	{
		DoubleMat t = new DoubleMat(ncols, nrows);
		for (long j=0; j<ncols; j++)
		{
			for (long i=0; i<nrows; i++)
			{
				t.set_m(j, i, getD(i, j));
			}
		}
		return t;
	}

	public Table rows(long... rowIndices) 
	{
		DoubleMat t = new DoubleMat(rowIndices.length, ncols, new Identifiers(colNames));
		for (long j=0; j<ncols; j++)
		{
			long offset = j * nrows;
			t.data.set(j * rowIndices.length, 
					LongStream.of(rowIndices)
					.mapToDouble(i -> data.get(offset + i))
					.toArray());
		}
		return t;
	}

	public Table cols(long... colIndices) 
	{
		DoubleMat t = new DoubleMat(nrows, colIndices.length);
		Set<String> colNameSet = new HashSet<>();
		for (int j=0; j<colIndices.length; j++)
		{
			long colIndex = colIndices[j];
			t.data.set(nrows*j, streamD(colIndex).toArray());
			String colName = getColName(colIndex);
			if (colName != null && colNameSet.add(colName)) t.setColName_m(j, colName);
		}
		return t;
	}

	public Table rowBind(Table... others) 
	{
		if (others == null || others.length == 0) return copy();
		for (Table o : others) 
			if (o.ncols() != ncols) 
				throw new KoralError("rowBind(): number of columns mismatch: " + ncols + " != " + o.ncols());
		
		List<Table> all = new ArrayList<>();
		all.add(this);
		all.addAll(Arrays.asList(others));
		
		long totalRows = all.stream().mapToLong(table -> table.nrows()).sum();
		
		DoubleMat t = new DoubleMat(totalRows, ncols, new Identifiers(colNames));
		for (long j=0; j<ncols; j++)
		{
			long offset = j * totalRows;
			for (Table table : all)
			{
				t.data.set(offset, table.streamD(j).toArray());
				offset += table.nrows();
			}
		}
		return t;
	}
}
