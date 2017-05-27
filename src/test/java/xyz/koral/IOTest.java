package xyz.koral;

import java.io.File;

import org.junit.Test;

public class IOTest 
{
	@Test
	public void listFilesJSONTest() 
	{
		System.out.println(IO.listFilesJSON(new File("C:/Temp/wos"), null));
	}
	
}
