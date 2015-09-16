package xyz.koral;

import java.util.Arrays;
import java.util.List;

public class TestR
{
	public long index;
	public double a = Double.NaN;
	public double b = Double.NaN;
	public List<String> c;
	public double[] d;
	public List<List<String>> e;
	
	public String toString() 
	{
		StringBuilder sb = new StringBuilder();
		if (c != null)
		{
			for (String s : c) sb.append(" " + s);
		}
		String cStr = sb.toString().trim();
		
		sb = new StringBuilder();
		if (e != null)
		{
			for (int i=0; i<e.size(); i++)
			{
				sb.append("(");
				
				List<String> es = e.get(i);
				for (int j=0; j<es.size(); j++)
				{
					sb.append(es.get(j));
					if (j < es.size() - 1) sb.append(" ");
				}
				
				sb.append(") ");
			}
		}
		String eStr = sb.toString().trim();
		
		return index + " a=" + a + " b=" + b + " c=" + cStr + " d=" + (d != null ? Arrays.toString(d) : "") + " e=" + eStr;
	}
}
