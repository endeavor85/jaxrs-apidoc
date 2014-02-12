package endeavor85.jaxrsapidoc;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.TreeMap;
import java.util.Vector;

import endeavor85.jaxrsapidoc.SanitizedType;

public class TryIt
{
	public static void main(String[] args)
	{
		for(Method method : TryIt.class.getMethods())
		{
			if(method.getName().endsWith("Method"))
			{
				System.out.println("Method: " + method.getName());
				SanitizedType returnType = SanitizedType.fromMethodReturnType(method);
				System.out.println("  " + returnType);
				for(Class<?> c : returnType.getReferencedTypes())
					System.out.println("    " + c.getName());
			}
		}
	}

	public HashMap<Integer, Boolean> someMethod()
	{
		return null;
	}

	public TreeMap<List<?>, Boolean> someOtherMethod()
	{
		return null;
	}

	public <T> Vector<T> yetAnotherMethod()
	{
		return null;
	}

	public HashMap<Long, Boolean> imposterMethod()
	{
		return null;
	}

	public HashMap<Long, Long> duplicateMethod()
	{
		return null;
	}

	public <Q> HashMap<ArrayList<TreeMap<?, Q>>, HashMap<TreeMap<Q, Q>, TreeMap<?, ?>>> hardMethod()
	{
		return null;
	}
}
