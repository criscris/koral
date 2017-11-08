package xyz.koral.server;

import java.util.List;
import java.util.function.Predicate;

public class Method
{
	public List<Predicate<Request>> conditions; 
	public MethodExecutor executor;
	
	public Method(List<Predicate<Request>> conditions, MethodExecutor executor)
	{
		this.conditions = conditions;
		this.executor = executor;
	}
	
	public static Method match(List<Method> methods, Request request)
	{
		Method method = null;
		for (Method m : methods)
		{
			boolean use = true;
			for (Predicate<Request> assertion : m.conditions)
			{
				if (!assertion.test(request))
				{
					use = false;
					break;
				}
			}
			if (use)
			{
				method = m;
				break;
			}
		}
		return method;
	}
}
