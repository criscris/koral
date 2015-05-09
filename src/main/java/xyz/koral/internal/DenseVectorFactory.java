package xyz.koral.internal;

public class DenseVectorFactory 
{
	public static DenseVector create(Class<?> clazz)
	{
		if (clazz == Double.class) return new DenseDoubleVector();
		if (clazz == Integer.class) return new DenseIntegerVector();
		if (clazz == Float.class) return new DenseDoubleVector();
		if (clazz == Long.class) return new DenseDoubleVector();
		if (clazz == Boolean.class) return new DenseBooleanVector();
		return new DenseStringVector();
	}
}
