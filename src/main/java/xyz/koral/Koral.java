package xyz.koral;

import java.util.List;
import java.util.Map;

import xyz.koral.internal.KoralImpl;
import xyz.koral.internal.StreamIterable;

public interface Koral 
{
	static Koral instance()
	{
		return new KoralImpl();
	}
	
	static Koral instance(Array... arrays)
	{
		return new KoralImpl(arrays);
	}
	
	int noOfArrays();
	List<Array> arrays();
	List<String> arrayIDs(String baseNamespace);
	default List<String> arrayIDs(QID baseNamespace) {
		return arrayIDs(baseNamespace.get());
	}
	void add(Array... arrays);
	void add(Koral... korals);
	void replace(Array oldArray, Array newArray);
	Koral indices(long[] indices);
	Koral indices(Query... queries);
	long maxNoOfEntries();
	Koral limit(long fromIndex, long maxNoOfIndices);
	default Array asArray(String qID) {
		return asArray(new QID(qID));
	}
	Array asArray(QID qID);

	<T> T get(String baseNamespace, Class<T> clazz, long index, String... ids);
	default <T> StreamIterable<T> asTable(Class<T> clazz, String... qualifiedIDs) {
		return asTable(null, clazz, qualifiedIDs);
	}
	default <T> StreamIterable<T> asTableFiltered(QID baseNamespace, Class<T> clazz, List<String> ids) {
		return asTableFiltered(baseNamespace.get(), clazz, ids.toArray(new String[0]));
	}
	<T> StreamIterable<T> asTableFiltered(String baseNamespace, Class<T> clazz, String... ids);
	default <T> StreamIterable<T> asTable(QID baseNamespace, Class<T> clazz, List<String> ids) {
		return asTable(baseNamespace.get(), clazz, ids.toArray(new String[0]));
	}
	<T> StreamIterable<T> asTable(String baseNamespace, Class<T> clazz, String... ids);
	StreamIterable<List<List<Entry>>> asTable();
	StreamIterable<List<List<Entry>>> asTable(List<Array> arrays);
	default StreamIterable<Map<String, List<Entry>>> asTable(String baseNamespace, List<String> ids) {
		return asTable(baseNamespace, ids.toArray(new String[0]));
	}
	StreamIterable<Map<String, List<Entry>>> asTable(String baseNamespace, String... ids);
}

