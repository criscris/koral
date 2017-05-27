package xyz.koral;

import org.junit.Test;

public class HTMLTest 
{
	@Test
	public void test1() 
	{
		System.out.println(
				HTML
				.koral("test", "http://koral.xyz/")
				.add("article", null, "class", "koral")
				.child()
				.add("h2", "This is the headline")
				.add("p", "text here.")
				.child()
				.add("br", null)
				.add("more.")
				.parent()
				.parent()
				.add("div", null, "class", "supi")
				.create());
	}
}

