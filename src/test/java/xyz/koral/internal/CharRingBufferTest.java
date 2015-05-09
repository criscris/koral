package xyz.koral.internal;

import java.util.Arrays;

import xyz.koral.internal.CharRingBuffer;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class CharRingBufferTest extends TestCase
{
    public CharRingBufferTest(String testName)
    {
        super(testName);
    }

    public static Test suite()
    {
        return new TestSuite(CharRingBufferTest.class);
    }
    
    public void test0() throws Exception
    {
    	check("a", 1);
    	check("ab", 1);
    	check("abc", 1);
    	check("abc", 2);
    	check("adbc", 2);
    	check("ajiojoijiojiojnjnjknknjknjiuhiubcxy", 2);
    	check("ajiojoijiojiojnjnjknknjkn1234567890", 10);
    	check("abcde", 23);
    	
    	match("abc", 3, "abc", true);
    	match("dabc", 3, "abc", true);
    	match("huhuihiudabc", 3, "abc", true);
    	match("dabc", 1, "c", true);
    	match("dabc", 1, "b", false);
    }
    
    static void check(String text, int maxBufferLength)
    {
    	CharRingBuffer buf = new CharRingBuffer(maxBufferLength);
    	buf.add(text);
    	
    	System.out.println(text + " " + Arrays.toString(buf.buf) + " l=" + buf.length + " p=" + buf.pointer);
    	
    	int c = Math.min(text.length(), maxBufferLength);
    	
    	for (int i=0; i<c; i++)
    	{
    		assertEquals(text.charAt(text.length() - c + i), buf.get(i));
    	}
    }
    
    static void match(String text, int maxBufferLength, String find, boolean expected)
    {
    	CharRingBuffer buf = new CharRingBuffer(maxBufferLength);
    	buf.add(text);
    	
    	
    	System.out.println(text + " " + Arrays.toString(buf.buf) + " l=" + buf.length + " p=" + buf.pointer + " " + find);
    	assertEquals(expected, buf.startsWith(find.toCharArray()));
    }
 
}
