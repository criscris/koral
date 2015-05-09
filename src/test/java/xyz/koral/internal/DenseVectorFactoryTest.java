package xyz.koral.internal;

import xyz.koral.internal.DenseVectorFactory;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class DenseVectorFactoryTest extends TestCase
{

    public DenseVectorFactoryTest(String testName)
    {
        super(testName);
    }

    public static Test suite()
    {
        return new TestSuite(DenseVectorFactoryTest.class);
    }
    
    public void test1() throws Exception
    {
    	System.out.println(DenseVectorFactory.create(Float.class));
    }
}
