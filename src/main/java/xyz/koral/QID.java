package xyz.koral;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import xyz.koral.internal.KoralError;

/**
 * a qualified Identifier
 * e.g.
 * a
 * a.b
 * a.b.c
 * ab1244.aK_ab2k
 */
public class QID 
{	
	List<String> levels = new ArrayList<>();
	
	static final Pattern pattern = Pattern.compile("[a-zA-Z][a-zA-Z_0-9]*(\\.[a-zA-Z][a-zA-Z_0-9]*)*");
	static final String sep = ".";
	static final String sepEsc = "\\.";
	public QID(String... parts)
	{
		init(parts);
	}
	
	private void init(String... parts)
	{
		if (parts.length == 0) throw new KoralError("Empty id");
		for (int i=0; i<parts.length; i++)
		{
			if (parts[i] == null) continue;
			Matcher m = pattern.matcher(parts[i]);
			if (!m.matches()) throw new KoralError("Invalid id part: " + parts[i]);
			
			for (String p : parts[i].split(sepEsc))
			{
				levels.add(p);
			}
		}
	}
	
	public QID(QID base, String id)
	{
		if (base != null) levels.addAll(base.levels);
		init(id);
	}
	
	public QID(QID... parts)
	{
		for (QID part : parts)
		{
			if (part == null) continue;
			levels.addAll(part.levels);
		}
		if (parts.length == 0) throw new KoralError("Empty id");
	}
	
	public QID(List<String> parts)
	{
		this(parts.toArray(new String[0]));
	}
	
	public String get()
	{
		StringBuilder sb = new StringBuilder();
		for (int i=0; i<levels.size(); i++)
		{
			sb.append(levels.get(i));
			if (i < levels.size() - 1) sb.append(sep);
		}
		return sb.toString();
	}
	
	public String getNamespace()
	{
		StringBuilder sb = new StringBuilder();
		for (int i=0; i<levels.size()-1; i++)
		{
			sb.append(levels.get(i));
			if (i < levels.size() - 2) sb.append(sep);
		}
		return sb.toString();
	}
	
	public QID getNamespaceQID()
	{
		return base(noOfLevels() - 1);
	}
	
	public String getID()
	{
		return levels.get(noOfLevels() - 1);
	}
	
	public QID split(QID baseQID)
	{
		if (baseQID.noOfLevels() >= noOfLevels()) return null;
		int same = noOfSameLevels(baseQID);
		return new QID(levels.subList(same, levels.size()));
	}
	
	public QID base(int noOfLevels)
	{
		return new QID(levels.subList(0, noOfLevels));
	}
	
	public QID parent()
	{
		if (levels.size() <= 1) return null;
		return base(levels.size() - 1);
	}
	
	public int noOfSameLevels(QID qid)
	{
		int l = 0;
		for (int i=0; i<Math.min(noOfLevels(), qid.noOfLevels()); i++)
		{
			if (!levels.get(i).equals(qid.levels.get(i))) break;
			l++;
		}
		return l;
	}
	
	public int noOfLevels()
	{
		return levels.size();
	}

	public int hashCode() 
	{
		return get().hashCode();
	}

	public boolean equals(Object obj) 
	{
		if (this == obj) return true;
		if (obj == null) return false;
		if (getClass() != obj.getClass()) return false;
		QID other = (QID) obj;
		return get().equals(other.get());
	}

	public String toString() 
	{
		return get();
	}
}
