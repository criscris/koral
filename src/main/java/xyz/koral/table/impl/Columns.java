package xyz.koral.table.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.function.DoubleBinaryOperator;
import java.util.function.DoublePredicate;
import java.util.function.DoubleUnaryOperator;
import java.util.function.IntPredicate;
import java.util.function.LongPredicate;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import java.util.stream.Stream;

import xyz.koral.KoralError;
import xyz.koral.table.Table;

/**
 * container for multiple tables that are aligned column-wise. tables itself may have any number of columns.
 * underlying tables may not be modified in shape other than through methods of this container.
 * 
 * doesn't grow
 */
public class Columns extends AbstractTable
{
	private static final long serialVersionUID = 2508679634579490498L;
	
	List<Table> tables;
	int[] tableColOffsets; // length == tables.size()
	int[] columIndexToTable; // length == ncols
	
	public Columns(List<Table> tables)
	{
		if (tables.size() == 0) throw new KoralError("Zero columns.");
		long ncols = tables.stream().mapToLong(t -> t.ncols()).sum();
		if (ncols >= Integer.MAX_VALUE - 5) throw new KoralError("Too many columns: " + ncols);
		
		long nrows = tables.get(0).nrows();
		for (int j=1; j<tables.size(); j++) if (tables.get(j).nrows() != nrows) throw new KoralError("nrows mismatch: " + nrows + " != " + tables.get(j).nrows());
		
		// any tables that are Columns implementations itself need to be unraveled
		this.tables = tables.stream()
		.flatMap(table -> table instanceof Columns ? ((Columns) table).tables.stream() : Stream.of(table))
		.collect(Collectors.toList());
		
		tableColOffsets = new int[this.tables.size()];
		columIndexToTable = new int[(int) ncols];
		int colOffset = 0;
		for (int i=0; i<this.tables.size(); i++)
		{
			tableColOffsets[i] = colOffset;
			int cols = (int) this.tables.get(i).ncols();
			for (int j=0; j<cols; j++)
			{
				columIndexToTable[colOffset] = i;
				colOffset++;
			}
		}
	}
	
	public Table copy() 
	{
		List<Table> tables_ = new ArrayList<>(tables.size());
		for (Table table : tables) tables_.add(table.copy());
		return new Columns(tables_);
	}

	public long nrows() 
	{
		return tables.get(0).nrows();
	}

	public long ncols() 
	{
		return columIndexToTable.length;
	}
	
	private final int ti(long colIndex)
	{
		if (colIndex >= columIndexToTable.length || colIndex < 0) 
			throw new KoralError("Column index out of bounds: " + colIndex + ". Table size (" + nrows() + "," + ncols() + ").");
		return columIndexToTable[(int) colIndex];
	}
	
	public Object get(long rowIndex, long colIndex) 
	{
		int ti = ti(colIndex);
		return tables.get(ti).get(rowIndex, colIndex - tableColOffsets[ti]);
	}

	public String getS(long rowIndex, long colIndex) 
	{
		int ti = ti(colIndex);
		return tables.get(ti).getS(rowIndex, colIndex - tableColOffsets[ti]);
	}

	public double getD(long rowIndex, long colIndex) 
	{
		int ti = ti(colIndex);
		return tables.get(ti).getD(rowIndex, colIndex - tableColOffsets[ti]);
	}

	public int getI(long rowIndex, long colIndex) 
	{
		int ti = ti(colIndex);
		return tables.get(ti).getI(rowIndex, colIndex - tableColOffsets[ti]);
	}

	public long getL(long rowIndex, long colIndex)
	{
		int ti = ti(colIndex);
		return tables.get(ti).getL(rowIndex, colIndex - tableColOffsets[ti]);
	}

	public Stream<String> streamS(long colIndex) 
	{
		int ti = ti(colIndex);
		return tables.get(ti).streamS(colIndex - tableColOffsets[ti]);
	}

	public DoubleStream streamD(long colIndex) 
	{
		int ti = ti(colIndex);
		return tables.get(ti).streamD(colIndex - tableColOffsets[ti]);
	}

	public LongStream streamL(long colIndex) 
	{
		int ti = ti(colIndex);
		return tables.get(ti).streamL(colIndex - tableColOffsets[ti]);
	}

	public IntStream streamI(long colIndex) 
	{
		int ti = ti(colIndex);
		return tables.get(ti).streamI(colIndex - tableColOffsets[ti]);
	}

	public Stream<String[]> streamRowsS() 
	{
		return LongStream.range(0, nrows()).mapToObj(i -> 
		{
			String[] row = new String[(int) ncols()];
			for (int j=0; j<ncols(); j++) row[j] = getS(i, j);
			return row;
		});
	}

	public Stream<double[]> streamRowsD() 
	{
		return LongStream.range(0, nrows()).mapToObj(i -> 
		{
			double[] row = new double[(int) ncols()];
			for (int j=0; j<ncols(); j++) row[j] = getD(i, j);
			return row;
		});
	}
	
	public Stream<Object[]> streamRows() 
	{
		return LongStream.range(0, nrows()).mapToObj(i -> 
		{
			Object[] row = new Object[(int) ncols()];
			for (int j=0; j<ncols(); j++) row[j] = get(i, j);
			return row;
		});
	}

	public String getColName(long colIndex) 
	{
		int ti = ti(colIndex);
		return tables.get(ti).getColName(colIndex - tableColOffsets[ti]);
	}
	
	public long getColIndex(String colName)
	{
		for (int i=0; i<tables.size(); i++)
		{
			try
			{
				long index = tables.get(i).getColIndex(colName);
				return tableColOffsets[i] + index;
			}
			catch (KoralError ex)
			{

			}
		}
		
		throw new KoralError("Invalid identifier: " + colName);
	}

	public Table setColName_m(long colIndex, String colId) 
	{
		int ti = ti(colIndex);
		tables.get(ti).setColName_m(colIndex - tableColOffsets[ti], colId);
		return this;
	}
	
	void checkRowIndex(long rowIndex)
	{
		if (rowIndex >= nrows() || rowIndex < 0) throw new KoralError(
				"Row index out of bounds: " + rowIndex + ". Table size=(" + nrows() + "," + ncols() + ").");
	}

	public Table set_m(long rowIndex, long colIndex, Object value) 
	{
		int ti = ti(colIndex);
		checkRowIndex(rowIndex);
		tables.get(ti).set_m(rowIndex, colIndex - tableColOffsets[ti], value);
		return this;
	}

	public Table set_m(long rowIndex, long colIndex, String value) 
	{
		int ti = ti(colIndex);
		checkRowIndex(rowIndex);
		tables.get(ti).set_m(rowIndex, colIndex - tableColOffsets[ti], value);
		return this;
	}

	public Table set_m(long rowIndex, long colIndex, double value) 
	{
		int ti = ti(colIndex);
		checkRowIndex(rowIndex);
		tables.get(ti).set_m(rowIndex, colIndex - tableColOffsets[ti], value);
		return this;
	}

	public Table set_m(long rowIndex, long colIndex, int value) 
	{
		int ti = ti(colIndex);
		checkRowIndex(rowIndex);
		tables.get(ti).set_m(rowIndex, colIndex - tableColOffsets[ti], value);
		return this;
	}

	public Table set_m(long rowIndex, long colIndex, long value) 
	{
		int ti = ti(colIndex);
		checkRowIndex(rowIndex);
		tables.get(ti).set_m(rowIndex, colIndex - tableColOffsets[ti], value);
		return this;
	}

	public Table applyD(DoubleUnaryOperator op) 
	{
		return new Columns(tables.stream().map(table -> table.applyD(op)).collect(Collectors.toList()));
	}

	public Table applyD(DoubleUnaryOperator op, long... colIndices) 
	{
		return copy().applyD_m(op, colIndices);
	}
	
	void checkDimensions(Table other)
	{
		if (ncols() != other.ncols()) throw new KoralError(
				"Dimension mismatch: (" + nrows() + "," + ncols() + ") != (" + other.nrows() + "," + other.ncols() + ")");
	}

	public Table applyD(DoubleBinaryOperator op, Table operand2) 
	{
		checkDimensions(operand2);
		List<Table> tables_ = new ArrayList<>();
		for (int i=0; i<tables.size(); i++)
		{
			tables_.add(tables.get(i).applyD(op, 
					operand2.cols(
							LongStream.range(tableColOffsets[i], 
							tableColOffsets[i] + tables.get(i).ncols()))));
		}
		return new Columns(tables_);
	}

	public Table applyD_m(DoubleUnaryOperator op) 
	{
		tables.stream().forEach(table -> table.applyD_m(op));
		return this;
	}

	public Table applyD_m(DoubleUnaryOperator op, long... colIndices) 
	{
		for (long colIndex : colIndices)
		{
			int ti = ti(colIndex);
			tables.get(ti).applyD_m(op, colIndex - tableColOffsets[ti]);
		}
		return this;
	}

	public Table applyD_m(DoubleBinaryOperator op, Table operand2) 
	{
		checkDimensions(operand2);
		for (int i=0; i<tables.size(); i++)
		{
			tables.get(i).applyD_m(op, 
					operand2.cols(
							LongStream.range(tableColOffsets[i], 
							tableColOffsets[i] + tables.get(i).ncols())));
		}
		return this;
	}

	public Table reduceRowsD(DoubleBinaryOperator op) 
	{
		return new Columns(tables.stream().map(table -> table.reduceRowsD(op)).collect(Collectors.toList()));
	}

	public Table whichS(Predicate<String> op, long colIndex) 
	{
		int ti = ti(colIndex);
		return tables.get(ti).whichS(op, colIndex - tableColOffsets[ti]);
	}

	public Table whichD(DoublePredicate op, long colIndex) 
	{
		int ti = ti(colIndex);
		return tables.get(ti).whichD(op, colIndex - tableColOffsets[ti]);
	}

	public Table whichI(IntPredicate op, long colIndex) 
	{
		int ti = ti(colIndex);
		return tables.get(ti).whichI(op, colIndex - tableColOffsets[ti]);
	}

	public Table whichL(LongPredicate op, long colIndex) 
	{
		int ti = ti(colIndex);
		return tables.get(ti).whichL(op, colIndex - tableColOffsets[ti]);
	}

	public Table orderedIndices(long colIndex) 
	{
		int ti = ti(colIndex);
		return tables.get(ti).orderedIndices(colIndex - tableColOffsets[ti]);
	}

	public Table orderedIndicesDesc(long colIndex) 
	{
		int ti = ti(colIndex);
		return tables.get(ti).orderedIndicesDesc(colIndex - tableColOffsets[ti]);
	}

	public Table transpose() 
	{
		Table t = tables.get(0).transpose();
		if (tables.size() > 1)
		{
			t = t.rowBind(tables.subList(1, tables.size()).stream().map(table -> table.transpose()).toArray(n -> new Table[n]));
		}
		return t;
	}

	public Table rows(long... rowIndices) 
	{
		return new Columns(tables.stream().map(table -> table.rows(rowIndices)).collect(Collectors.toList()));
	}

	
	public Table cols(long... colIndices) 
	{
		if (colIndices == null || colIndices.length == 0) throw new KoralError("No columns specified.");
		
		List<Table> tables_ = new ArrayList<>();
		int lastTi = -1;
		List<Long> localCols = new ArrayList<>();
		
		for (long colIndex : colIndices)
		{
			int ti = ti(colIndex);
			if (ti != lastTi)
			{
				if (localCols.size() > 0)
				{
					tables_.add(tables.get(lastTi).cols(localCols.stream().mapToLong(l -> l)));
					localCols = new ArrayList<>();
				}
				lastTi = ti;
			}
			localCols.add(colIndex - tableColOffsets[ti]);
		}
		if (localCols.size() > 0) tables_.add(tables.get(lastTi).cols(localCols.stream().mapToLong(l -> l)));
		
		// if only one table remain, no need for Columns container
		return tables_.size() == 1 ? tables_.get(0) : new Columns(tables_);
	}

	public Table rowBind(Table... others) 
	{
		if (others == null || others.length == 0) return copy();
		for (Table o : others) 
			if (o.ncols() != ncols()) 
				throw new KoralError("rowBind(): number of columns mismatch: " + ncols() + " != " + o.ncols());
	
		List<Table> tables_ = new ArrayList<>();
		for (int i=0; i<tables.size(); i++)
		{
			// from all tables extract the columns needed for rowbinding to the current table
			long[] cols = LongStream.range(tableColOffsets[i], tableColOffsets[i] + tables.get(i).ncols()).toArray();
			Table[] tcols = Stream.of(others).map(t -> t.cols(cols)).toArray(Table[]::new);
			tables_.add(tables.get(i).rowBind(tcols));
			
		}
		return new Columns(tables_);
	}
	
	public boolean isNumeric()
	{
		for (Table t : tables) if (!t.isNumeric()) return false;
		return true;
	}
}
