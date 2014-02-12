package endeavor85.jaxrsapidoc;

import static org.junit.Assert.assertEquals;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.Vector;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import endeavor85.jsonapigen.SanitizedType;

public class TestSanitizedType
{
	@BeforeClass
	public static void setUpBeforeClass() throws Exception
	{

	}

	@Before
	public void setUp() throws Exception
	{}

	@Test
	public void testReturnTypeRecognition()
	{
		for(Method method : DummyClass.class.getMethods())
		{
			if(method.getName().endsWith("paramTypeMethod"))
			{
				assertEquals("HashMap<Integer, Boolean>", SanitizedType.fromMethodReturnType(method).toString());
			}
			else if(method.getName().endsWith("paramTypeWildcardMethod"))
			{
				assertEquals("TreeMap<List<?>, Boolean>", SanitizedType.fromMethodReturnType(method).toString());
			}
			else if(method.getName().endsWith("paramTypeVariableMethod"))
			{
				assertEquals("Vector<T>", SanitizedType.fromMethodReturnType(method).toString());
			}
			else if(method.getName().endsWith("duplicateTypeMethod"))
			{
				assertEquals("Map<Long, Long>", SanitizedType.fromMethodReturnType(method).toString());
			}
			else if(method.getName().endsWith("hardMethod"))
			{
				assertEquals("HashMap<ArrayList<TreeMap<?, T>>, HashMap<TreeMap<T, T>, TreeMap<?, ?>>>",
						SanitizedType.fromMethodReturnType(method).toString());
			}
		}
	}

	@Test
	public void testGetReferencedTypes()
	{
		for(Method method : DummyClass.class.getMethods())
		{
			if(method.getName().endsWith("paramTypeMethod"))
			{
				assertEquals(3, SanitizedType.fromMethodReturnType(method).getReferencedTypes().size());
			}
			else if(method.getName().endsWith("paramTypeWildcardMethod"))
			{
				assertEquals(4, SanitizedType.fromMethodReturnType(method).getReferencedTypes().size());
			}
			else if(method.getName().endsWith("paramTypeVariableMethod"))
			{
				assertEquals(2, SanitizedType.fromMethodReturnType(method).getReferencedTypes().size());
			}
			else if(method.getName().endsWith("duplicateTypeMethod"))
			{
				assertEquals(2, SanitizedType.fromMethodReturnType(method).getReferencedTypes().size());
			}
			else if(method.getName().endsWith("hardMethod"))
			{
				assertEquals(5, SanitizedType.fromMethodReturnType(method).getReferencedTypes().size());
			}
		}
	}

	public class DummyClass
	{
		public HashMap<Integer, Boolean> paramTypeMethod()
		{
			return null;
		}

		public TreeMap<List<?>, Boolean> paramTypeWildcardMethod()
		{
			return null;
		}

		public <T> Vector<T> paramTypeVariableMethod()
		{
			return null;
		}

		public Map<Long, Long> duplicateTypeMethod()
		{
			return null;
		}

		public <Q> HashMap<ArrayList<TreeMap<?, Q>>, HashMap<TreeMap<Q, Q>, TreeMap<?, ?>>> hardMethod()
		{
			return null;
		}
	}
}
