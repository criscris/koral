package xyz.koral;

import java.io.File;
import java.io.OutputStream;
import java.util.List;

import xyz.koral.internal.KoralIOImpl;

public interface KoralIO 
{
	static KoralIO instance()
	{
		return new KoralIOImpl();
	}
	
	Koral load(File ... files);
	Koral load(boolean initSearchIndex, File ... files);
	<T> void save(Iterable<T> objects, Class<T> clazz, QID baseID, OutputStream os);
	void save(Koral koral, OutputStream os);
	void save(List<Array> arrays, OutputStream os);
	void save(List<Array> arrays, long[] indices, OutputStream os);
	void saveAsCsv(Koral koral, OutputStream os);
}


