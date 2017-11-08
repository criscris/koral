package xyz.koral.table.impl;

import java.io.Serializable;
import java.util.function.DoubleUnaryOperator;
import java.util.stream.DoubleStream;

public interface DoubleArray extends Serializable
{
	long size();
	double get(long index);
	void add(double value);
	void add(double... values);
	void set(long index, double value);
	void set(long index, double... values);
	void apply(DoubleUnaryOperator op);
	void apply(DoubleUnaryOperator op, long offset, long count);
	void trim();
	DoubleStream stream();
	DoubleStream stream(long offset, long count);
	DoubleArray copy();
	DoubleArray copyAndApply(DoubleUnaryOperator op);
}
