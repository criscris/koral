package xyz.koral.internal;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import xyz.koral.Entry;

public class PitchIterator implements StreamIterable<List<Entry>>
{
	Iterable<Entry> iterable;
	
	public PitchIterator(Iterable<Entry> iterable)
	{
		this.iterable = iterable;
	}

	public Iterator<List<Entry>> iterator() 
	{
		Iterator<Entry> iter = iterable.iterator();
		
		return new Iterator<List<Entry>>() 
		{
			Entry lastElement = null;
			List<Entry> entries = null;
			
			public boolean hasNext() 
			{
				if (entries == null) 
				{
					entries = new ArrayList<Entry>();
					if (lastElement != null) 
					{
						entries.add(lastElement);
						lastElement = null;
					}
					while (iter.hasNext())
					{
						lastElement = iter.next();
						if (entries.size() > 0 && lastElement.index() > entries.get(0).index()) break;
						entries.add(lastElement);
						lastElement = null;
					}
				}
				
				return entries.size() != 0;
			}

			public List<Entry> next() 
			{
				if (!hasNext()) return null;
				List<Entry> result = entries;
				entries = null;
				return result;
			}
		};
	}
}
