package xyz.koral.table.impl;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import xyz.koral.KoralError;

public class Identifiers implements Serializable
{
	private static final long serialVersionUID = 1L;
	
	Map<Long, String> indexToValue;
	Map<String, Long> valueToIndex;
	
	public Identifiers()
	{
		indexToValue = new HashMap<>();
		valueToIndex = new HashMap<>();
	}
	
	public Identifiers(Identifiers source)
	{
		indexToValue = new HashMap<>(source.indexToValue);
		valueToIndex = new HashMap<>(source.valueToIndex);
	}
	
	public void set(long index, String id)
	{
		if (id != null)
		{
			Long index_ = valueToIndex.get(id);
			if (index_ != null && index_ != index) throw new KoralError("Non-unique identifier:" + id);
			valueToIndex.put(id, index);
		}

		String oldID = indexToValue.get(index);
		if (oldID != null) valueToIndex.remove(oldID);
		
		if (id != null)
		{
			indexToValue.put(index, id);
		}
		else
		{
			indexToValue.remove(index);
		}
		
	}
	
	public String get(long index)
	{
		return indexToValue.get(index);
	}
	
	public long getIndex(String id)
	{
		Long index = valueToIndex.get(id);
		if (index == null) throw new KoralError("Invalid identifier: " + id);
		return index;
	}

	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((indexToValue == null) ? 0 : indexToValue.hashCode());
		result = prime * result + ((valueToIndex == null) ? 0 : valueToIndex.hashCode());
		return result;
	}

	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Identifiers other = (Identifiers) obj;
		if (indexToValue == null) {
			if (other.indexToValue != null)
				return false;
		} else if (!indexToValue.equals(other.indexToValue))
			return false;
		if (valueToIndex == null) {
			if (other.valueToIndex != null)
				return false;
		} else if (!valueToIndex.equals(other.valueToIndex))
			return false;
		return true;
	}
}
