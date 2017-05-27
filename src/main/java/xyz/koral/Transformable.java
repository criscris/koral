package xyz.koral;

import java.util.function.DoubleBinaryOperator;
import java.util.function.DoubleUnaryOperator;

/**
 * Methods perform an entry-wise operation. Table keeps dimensions (nrows x ncols).
 * colIndices specifies the columns that shall be transformed; all other columns of the table remain unchanged.
 * Methods without colIndices parameter perform operation on all columns.
 *
 */
public interface Transformable 
{
	Table applyD(DoubleUnaryOperator op);
	Table applyD(DoubleUnaryOperator op, long... colIndices);
	Table applyD(DoubleBinaryOperator op, Table operand2);
	Table applyD_m(DoubleUnaryOperator op);
	Table applyD_m(DoubleUnaryOperator op, long... colIndices);
	Table applyD_m(DoubleBinaryOperator op, Table operand2);
	
	default Table add(double value) { return applyD(a -> a + value); }
	default Table add(double value, long... colIndices) { return applyD(a -> a + value, colIndices); }
	default Table add(Table values) { return applyD((v1, v2) -> v1 + v2, values); }
	default Table add_m(double value) { return applyD_m(a -> a + value); }
	default Table add_m(double value, long... colIndices) { return applyD(a -> a + value, colIndices); }
	default Table add_m(Table values) { return applyD_m((v1, v2) -> v1 + v2, values); }
	
	default Table sub(double value) { return applyD(a -> a - value); }
	default Table sub(double value, long... colIndices) { return applyD(a -> a - value, colIndices); }
	default Table sub(Table values) { return applyD((v1, v2) -> v1 - v2, values); }
	default Table sub_m(double value) { return applyD_m(a -> a - value); }
	default Table sub_m(double value, long... colIndices) { return applyD(a -> a - value, colIndices); }
	default Table sub_m(Table values) { return applyD_m((v1, v2) -> v1 - v2, values); }
	
	default Table mul(double value) { return applyD(a -> a * value); }
	default Table mul(double value, long... colIndices) { return applyD(a -> a * value, colIndices); }
	default Table mul(Table values) { return applyD((v1, v2) -> v1 * v2, values); }
	default Table mul_m(double value) { return applyD_m(a -> a * value); }
	default Table mul_m(double value, long... colIndices) { return applyD(a -> a * value, colIndices); }
	default Table mul_m(Table values) { return applyD_m((v1, v2) -> v1 * v2, values); }
	
	default Table divideBy(double value) { return applyD(a -> a / value); }
	default Table divideBy(double value, long... colIndices) { return applyD(a -> a / value, colIndices); }
	default Table divideBy(Table values) { return applyD((v1, v2) -> v1 / v2, values); }
	default Table divideBy_m(double value) { return applyD_m(a -> a / value); }
	default Table divideBy_m(double value, long... colIndices) { return applyD(a -> a / value, colIndices); }
	default Table divideBy_m(Table values) { return applyD_m((v1, v2) -> v1 / v2, values); }
	
	default Table round() { return applyD(a -> Math.round(a)); }
	default Table round(long... colIndices) { return applyD(a -> Math.round(a), colIndices); }
	default Table round_m() { return applyD_m(a -> Math.round(a)); }
	default Table round_m(long... colIndices) { return applyD(a -> Math.round(a), colIndices); }

	default Table log10() { return applyD(a -> Math.log10(a)); }
	default Table log10(long... colIndices) { return applyD(a -> Math.log10(a), colIndices); }
	default Table log10_m() { return applyD_m(a -> Math.log10(a)); }
	default Table log10_m(long... colIndices) { return applyD(a -> Math.log10(a), colIndices); }

	default Table sqrt() { return applyD(a -> Math.sqrt(a)); }
	default Table sqrt(long... colIndices) { return applyD(a -> Math.sqrt(a), colIndices); }
	default Table sqrt_m() { return applyD_m(a -> Math.sqrt(a)); }
	default Table sqrt_m(long... colIndices) { return applyD(a -> Math.sqrt(a), colIndices); }

	default Table abs() { return applyD(a -> Math.abs(a)); }
	default Table abs(long... colIndices) { return applyD(a -> Math.abs(a), colIndices); }
	default Table abs_m() { return applyD_m(a -> Math.abs(a)); }
	default Table abs_m(long... colIndices) { return applyD(a -> Math.abs(a), colIndices); }

	default Table pow(double exp) { return applyD(a -> Math.pow(a, exp)); }
	default Table pow(double exp, long... colIndices) { return applyD(a -> Math.pow(a, exp), colIndices); }
	default Table pow_m(double exp) { return applyD_m(a -> Math.pow(a, exp)); }
	default Table pow_m(double exp, long... colIndices) { return applyD(a -> Math.pow(a, exp), colIndices); }
	
	default Table powBase(double base) { return applyD(a -> Math.pow(base, a)); }
	default Table powBase(double base, long... colIndices) { return applyD(a -> Math.pow(base, a), colIndices); }
	default Table powBase_m(double base) { return applyD_m(a -> Math.pow(base, a)); }
	default Table powBase_m(double base, long... colIndices) { return applyD(a -> Math.pow(base, a), colIndices); }
}
