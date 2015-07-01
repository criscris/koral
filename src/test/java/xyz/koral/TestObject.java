package xyz.koral;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TestObject
{
	public long index;
	
	public double number1;
	public String text1;
	public List<String> values;
	
	public TestObject()
	{
		
	}
	
	public TestObject(double number1, String text1, String... values) 
	{
		this.number1 = number1;
		this.text1 = text1;
		this.values = new ArrayList<String>();
		for (String v : values) this.values.add(v);
	}

	public String toString() 
	{
		return "TestObject " + number1 + " " + text1 + " " + Arrays.toString(values.toArray(new String[0]));
	}	
}
