package xyz.koral;

import static org.junit.Assert.assertEquals;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.lang.reflect.Proxy;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.DoubleBinaryOperator;
import java.util.function.DoubleUnaryOperator;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.junit.Test;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

public class KoralTest {

	@Test
	public void testCall() 
	{
		assertEquals(42, KoralFunction.createFunctionCall("xyz.koral.CallTestFunctions.answerToAllQuestions", null).get());
		assertEquals(1.0, KoralFunction.createFunctionCall("xyz.koral.CallTestFunctions.log10", "{ 'a': 10 }").get());
		assertEquals("HOI", KoralFunction.createFunctionCall("xyz.koral.CallTestFunctions.upperCase", "{ 'text': 'hoi' }").get());
		assertEquals("hoi", KoralFunction.createFunctionCall("xyz.koral.CallTestFunctions.upperCase2", "{ 'text': 'hoi', 'really':false }").get());
		assertEquals(CallTestFunctions.square(17L), KoralFunction.createFunctionCall("xyz.koral.CallTestFunctions.square", "{ 'val': 17 }").get());
		
		assertEquals(CallTestFunctions.scale(new Vec(12, 3.1, new Z("aname")), 2.0), 
				KoralFunction.createFunctionCall("xyz.koral.CallTestFunctions.scale", "{ 'v': { 'x': 12, 'y': 3.1, 'label': { 'name':'aname' } }, 'scale': 2 }").get());
		
		assertEquals(CallTestFunctions.log10(CallTestFunctions.sq(10)),
				KoralFunction.createFunctionCall("xyz.koral.CallTestFunctions.log10", "{}", "{ 'a': { 'f':'xyz.koral.CallTestFunctions.sq', 'x': { 'a' : 10 } } }").get());
		
		assertEquals(CallTestFunctions.log10(CallTestFunctions.sq(CallTestFunctions.add1(10))), 
				KoralFunction.createFunctionCall("xyz.koral.CallTestFunctions.log10", "{}", 
				"{ 'a': { 'f':'xyz.koral.CallTestFunctions.sq', 'g': "
			  + "{ 'a': { 'f':'xyz.koral.CallTestFunctions.add1', 'x': { 'a' : 10 } } } } }").get());
		
		assertEquals(CallTestFunctions.u(12, CallTestFunctions::sq), KoralFunction.createFunctionCall("xyz.koral.CallTestFunctions.u", "{ 'v': 12 }", 
				"{ 'before': { 'f':'xyz.koral.CallTestFunctions.sq' } }").get());
		
		assertEquals(CallTestFunctions.v(12,  CallTestFunctions::sub), KoralFunction.createFunctionCall("xyz.koral.CallTestFunctions.v", "{ 'v': 12 }", 
				"{ 'z': { 'f':'xyz.koral.CallTestFunctions.sub' } }").get());
		
		System.out.println("token=" + IO.generateRandomToken());
	}
}

interface KoralFunction 
{
	static Supplier<Object> createFunctionCall(String methodReference, String jsonArgs)
	{
		return createFunctionCall(methodReference, jsonArgs, null);
	}
	
	/**
	 * javac -parameters
	 * must be used when compiling
	 * 
	 * 
	 * @param methodReference fully qualified method reference (package.class.methodname)
	 * @param jsonArgs var name -> serialized json value
	 */
	static Supplier<Object> createFunctionCall(String methodReference, String jsonArgs, String jsonFunctions)
	{
		Method method = staticMethod(methodReference); 
		Gson gson = new Gson();

		Type type = new TypeToken<Map<String, Object>>(){}.getType();
		Map<String, Object> argsToObject = gson.fromJson(jsonArgs, type);
		Map<String, String> argsToJsonString = argsToObject == null ? new HashMap<>() : argsToObject.entrySet().stream().collect(Collectors.toMap(e -> e.getKey(), e -> gson.toJson(e.getValue())));
		
		type = new TypeToken<Map<String, KoralFunc>>(){}.getType();
		Map<String, KoralFunc> argsToFunc = gson.fromJson(jsonFunctions, type);
		if (argsToFunc == null) argsToFunc = new HashMap<>();
		
		Class<?>[] params = method.getParameterTypes();
		Parameter[] paramNames = method.getParameters();
		List<Supplier<Object>> valueSupplier = new ArrayList<>(params.length);
		for (int j=0; j<params.length; j++)
		{
			String t = params[j].getSimpleName();
			String name = paramNames[j].getName();
			
			boolean isInterface = params[j].isInterface();
			String arg = null;
			if (!isInterface && (arg = argsToJsonString.get(name)) != null)
			{
				try
				{
					Object value = gson.fromJson(arg, params[j]);
					valueSupplier.add(() -> value);
				}
				catch (Exception ex)
				{
					throw new KoralError(methodReference + ": provided arg value for '" + name + "' could not be deserialized to type " + t);
				}
			}
			else
			{
				KoralFunc func = argsToFunc.get(name);
				if (func == null) throw new KoralError(methodReference + ": value or function for parameter '" + t + " " + name + "' missing.");
				
				if (!isInterface)
				{
					valueSupplier.add(createFunctionCall(func.f, gson.toJson(func.x), gson.toJson(func.g)));
				}
				else
				{
					List<Method> imethods = new ArrayList<>();
					for (Method m : params[j].getMethods())
					{
						if (m.isDefault() || Modifier.isStatic(m.getModifiers()) || m.getReturnType().equals(Void.TYPE)) continue;
						imethods.add(m);
					}
					if (imethods.size() == 0) throw new KoralError(methodReference + ": interface '" + t + " " + name + "' is empty.");
					if (imethods.size() > 1) throw new KoralError(methodReference + ": '" + t + " " + name + "' is not a functional interface (has more than 1 method).");
					//Method imethod = imethods.get(0);
					
					
					Method g = staticMethod(func.f);
					
					Object proxyInterface = Proxy.newProxyInstance(
							params[j].getClassLoader(), 
							new java.lang.Class[] { params[j] }, 
							(proxy, method_, args) -> {
								return g.invoke(null, args);
							});
					valueSupplier.add(() -> proxyInterface);
				}
			}
		}
		
		return () -> {
			try 
			{
				Object[] values = new Object[valueSupplier.size()];
				for (int j=0; j<values.length; j++) values[j] = valueSupplier.get(j).get(); 
				return method.invoke(null, values);
			} 
			catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) 
			{
				throw new KoralError(ex);
			}
		};
	}
	
	static Method staticMethod(String methodReference)
	{
		if (methodReference == null || methodReference.trim().length() == 0) throw new KoralError("No method reference specified");
		methodReference = methodReference.trim();
		int i = methodReference.lastIndexOf(".");
		if (i == -1) throw new KoralError("Invalid method reference: " + methodReference);
		Class<?> clazz = null;
		try 
		{
			clazz = Class.forName(methodReference.substring(0,  i));
		} 
		catch (ClassNotFoundException ex) 
		{
			throw new KoralError(ex);
		}
		
		String methodName = methodReference.substring(i + 1);
		
		List<Method> methods = new ArrayList<>();
		for (Method m : clazz.getMethods())
		{
			if (m.getName().equals(methodName) && Modifier.isStatic(m.getModifiers()) && !m.getReturnType().equals(Void.TYPE))
			{
				methods.add(m);
			}
		}
		if (methods.size() == 0) throw new KoralError("No public static non-void method '" + methodName + "' found in '" + clazz.getName() + "'");
		if (methods.size() > 1) throw new KoralError("Method reference " + methodReference + " is ambiguous due to method overloading. " + methods.size() + " methods match.");
		return methods.get(0);
	}
}

class KoralFunc 
{
	public String url;
	public String f;
	public Object x;
	public Object g;
}


class CallTestFunctions
{
	public static int answerToAllQuestions()
	{
		return 42;
	}
	
	public static double log10(double a)
	{
		return Math.log10(a);
	}
	
	public static int add1(int a)
	{
		return a + 1;
	}
	
	public static String upperCase(String text)
	{
		return text.toUpperCase();
	}
	
	public static String upperCase2(String text, boolean really)
	{
		return really ? text.toUpperCase() : text;
	}
	
	public static long square(Long val)
	{
		return val * val;
	}
	
	public static Vec scale(Vec v, double scale)
	{
		return new Vec(v.x * scale, v.y * scale, v.label);
	}
	
	public static double sq(double a)
	{
		return a*a;
	}
	
	public static double u(double v, DoubleUnaryOperator before)
	{
		return before.applyAsDouble(v) - 1;
	}
	
	public static double v(double v, DoubleBinaryOperator z)
	{
		return z.applyAsDouble(v, v/2) - 1;
	}
	
	public static double sub(double a, double b)
	{
		return a - b;
	}
}

class Z
{
	String name;
	
	public Z(String name) 
	{
		this.name = name;
	}

	public int hashCode() 
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		return result;
	}

	public boolean equals(Object obj) 
	{
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Z other = (Z) obj;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		return true;
	}
}

class Vec
{
	public double x;
	public double y;
	Z label;
	
	public Vec(double x, double y, Z label)
	{
		this.x = x;
		this.y = y;
		this.label = label;
	}

	public String toString() 
	{
		return "Vec [x=" + x + ", y=" + y + " name=" + (label != null ? label.name : null) + "]";
	}

	public int hashCode() 
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + ((label == null) ? 0 : label.hashCode());
		long temp;
		temp = Double.doubleToLongBits(x);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		temp = Double.doubleToLongBits(y);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		return result;
	}


	public boolean equals(Object obj) 
	{
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Vec other = (Vec) obj;
		if (label == null) {
			if (other.label != null)
				return false;
		} else if (!label.equals(other.label))
			return false;
		if (Double.doubleToLongBits(x) != Double.doubleToLongBits(other.x))
			return false;
		if (Double.doubleToLongBits(y) != Double.doubleToLongBits(other.y))
			return false;
		return true;
	}
}
