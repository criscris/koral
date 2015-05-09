package xyz.koral.internal;

import java.util.LinkedHashSet;
import java.util.Set;

public class LRUQueue
{
	Set<Unloadable> queue = new LinkedHashSet<>(); // ordered set
	
	long currentTotalMemory = 0;
	final long maxMemory;
	
	public LRUQueue(long maxMemory)
	{
		this.maxMemory = maxMemory;
	}
	
	public int size()
	{
		return queue.size();
	}
	
	public long sizeInBytes()
	{
		return currentTotalMemory;
	}
	
	public boolean add(Unloadable unloadable)
	{
		if (queue.contains(unloadable))
		{
			queue.remove(unloadable);
			queue.add(unloadable);
			return false;
		}
		else
		{
			currentTotalMemory += unloadable.memorySize();

			while (queue.size() > 0 && currentTotalMemory > maxMemory)
			{
				Unloadable u = queue.iterator().next();
				queue.remove(u);
				currentTotalMemory -= u.memorySize();
				u.unload();
			}
			
			queue.add(unloadable);
			return true;
		}
	}
}