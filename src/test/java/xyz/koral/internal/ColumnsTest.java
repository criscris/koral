package xyz.koral.internal;

import java.util.function.BiFunction;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.Test;

import xyz.koral.table.Table;
import xyz.koral.table.impl.Columns;
import xyz.koral.table.impl.DoubleVec;

public class ColumnsTest {

	@Test
	public void test() 
	{
		BiFunction<Long, Long, Table> table = (nrows, ncols) ->  new Columns(
				IntStream.range(0, (int) (long) ncols).boxed().map(i -> new DoubleVec(nrows)).collect(Collectors.toList()));
		
		DoubleMatTest.mat(table, 100, 10, 123);
		DoubleMatTest.mat(table, 1000, 11, 124);
		DoubleMatTest.mat(table, 10000, 12, 123);
		DoubleMatTest.mat(table, 89765, 4, 123);
		DoubleMatTest.mat(table, 189765, 44, 123);
	}

}
