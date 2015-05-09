package xyz.koral;

import java.util.function.Consumer;

public interface WritableArray extends Array, Consumer<Entry>
{
	void add(String... values);
	void add(float... values);
	void add(double... values);
	void add(int... values);
	void add(long... values);
	
	void set(long index, long pitch, String... values);
	void set(long index, long pitch, float... values);
	void set(long index, long pitch, double... values);
	void set(long index, long pitch, int... values);
	void set(long index, long pitch, long... values);
}
