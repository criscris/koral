package xyz.koral;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.List;

import xyz.koral.internal.ArrayStream;
import xyz.koral.internal.ArrayStreamWriter;
import xyz.koral.internal.KoralError;
import xyz.koral.internal.KoralIOImpl;
import xyz.koral.internal.XmlDocument;

public interface KoralIO 
{
	static KoralIO instance()
	{
		return new KoralIOImpl();
	}
	
	default Koral load(File ... files) {
		return load(true, files);
	}
	Koral load(boolean initSearchIndex, File ... files);
	<T> void save(Iterable<T> objects, Class<T> clazz, QID baseID, OutputStream os);
	default void save(Koral koral, OutputStream os) {
		save(koral.arrays(), os);
	}
	default void save(List<Array> arrays, OutputStream os) {
		save(arrays, null, os);
	}
	default void save(List<Array> arrays, long[] indices, OutputStream os) {
		if (arrays.size() == 0) throw new KoralError("cannot save KoralResource: no array.");
		
		QID base = arrays.get(0).qid().getNamespaceQID();
		for (int i=1; i<arrays.size(); i++)
		{
			QID base1 = arrays.get(i).qid().getNamespaceQID();
			int levels = base.noOfSameLevels(base1);
			base = base.base(levels);
		}
		
		class Intender
		{
			String intend = "    ";
			int intendLevel = 0;
			
			void up()
			{
				intendLevel++;
			}
			
			void down()
			{
				intendLevel--;
			}
			
			String get()
			{
				StringBuilder sb = new StringBuilder();
				for (int i=0; i<intendLevel; i++) sb.append(intend);
				return sb.toString();
			}
		}
		Intender intender = new Intender();
		
		
		BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os, XmlDocument.cs));
		try
		{
			writer.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
			writer.write("<koral id=\"" + base + "\" version=\"0.2\" xmlns=\"http://koral.xyz/schema\">\n");
		
			for (int i=0; i<arrays.size(); i++)
			{
				Array a = arrays.get(i);
				QID aQID = a.qid().split(base);
				String namespace = aQID.getNamespace();
				
				if (namespace.length() > 0)
				{
					intender.up();
					writer.write(intender.get());
					writer.write("<koral id=\"" + namespace + "\">\n");
					
				}
				
				intender.up();
				writer.write(intender.get());
				writer.write("<array ");
				writer.write("id=\"" + aQID.getID() + "\" ");
				
				writer.write("type=\"" + a.typeLiteral() + "\" ");
				if (! (a.strideSize() == 1 && a.strideName(0).length() == 0)) 
				{
					String s = a.strideName(0);
					for (int j=1; j<a.strideSize(); j++)
					{
						s += ArrayStream.strideSep + a.strideName(j);
					}
					writer.write("stride=\"" + s + "\" ");
				}
					
				if (a.maxPitchSize() > 1) writer.write("maxPitch=\"" + a.maxPitchSize() + "\" ");
				
				if (indices == null)
				{
					writer.write("count=\"" + a.size() + "\"");
					writer.write(">");
					a.forEach(new ArrayStreamWriter(writer, true));
				}
				else
				{
					writer.write("count=\"" + indices.length + "\"");
					writer.write(">");
					ArrayStreamWriter w = new ArrayStreamWriter(writer, true);
					for (int j=0; j<indices.length; j++)
					{
						List<Entry> entries = a.getPitch(indices[j]);
						if (entries == null) continue;
						int index = j;
						for (Entry e : entries)
						{
							// change index
							w.accept(new Entry()
							{
								public long index() { return index; }
								public long pitchIndex() { return e.pitchIndex(); }
								public int strideSize() { return e.strideSize(); }
								public String getS(int strideIndex) { return e.getS(strideIndex); }
								public String[] getStrideS() { return e.getStrideS(); }
								public float getF(int strideIndex) { return e.getF(); }
								public float[] getStrideF() { return getStrideF(); }
								public double getD(int strideIndex) { return e.getD(strideIndex); }
								public double[] getStrideD() { return e.getStrideD(); }
								public int getI(int strideIndex) { return e.getI(strideIndex); }
								public int[] getStrideI() { return e.getStrideI(); }
								public long getL(int strideIndex) { return e.getL(strideIndex); }
								public long[] getStrideL() { return e.getStrideL(); }
							});
						}
					}
				}
				
				writer.write("</array>\n");
				intender.down();
				
				if (namespace.length() > 0)
				{
					writer.write(intender.get());
					writer.write("</koral>\n");
					intender.down();
				}
			}
			
			writer.write("</koral>");
			writer.close();
		}
		catch (IOException ex)
		{
			throw new KoralError(ex);
		}
	}
	void saveAsCsv(Koral koral, OutputStream os);
}


