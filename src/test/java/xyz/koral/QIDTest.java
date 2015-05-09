package xyz.koral;

import xyz.koral.QID;
import xyz.koral.internal.KoralError;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class QIDTest extends TestCase
{
    public QIDTest(String testName)
    {
        super(testName);
    }

    public static Test suite()
    {
        return new TestSuite(QIDTest.class);
    }
    
    public void test0() throws Exception
    {
    	assertEquals(4, new QID("nHa.b.cde_3H232a.acHqszqa").levels.size());
    	
    	assertEquals("bcd", new QID("na.bcd").getID());
    	assertEquals("na.x", new QID("na.x.bcd").getNamespace());
    	assertEquals("na.x", new QID("na", "x", "bcd").getNamespace());
    	
    	QID qid = new QID("a", "b", "c");
    	
    	
    	assertEquals("a.b", qid.getNamespaceQID().get());
    	assertEquals(2, qid.noOfSameLevels(new QID("a.b.d")));
    	assertEquals("b.c", qid.split(new QID("a")).get());
    	assertEquals("a.b", qid.base(2).get());
    	assertNull(qid.split(new QID("a.b.c")));
	
    	try 
    	{
    		new QID("Awqopjeopqwjdopqwpdokpwqkd.dwqdqwdqwd...dwqa");
    	    fail("no execption thrown for wrong QID");
    	} catch (KoralError ex) 
    	{
    		
    	}
    }
}
