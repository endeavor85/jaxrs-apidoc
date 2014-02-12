package endeavor85.jsonapigen;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;

import endeavor85.jsonapigen.generics.Variable;
import endeavor85.jsonapigen.generics.Wildcard;

public class SanitizedType
{
	Class<?>			baseType;
	boolean				isArray;
	List<SanitizedType>	parameterizedTypes	= new ArrayList<>();

	public static SanitizedType fromClass(Class<?> clazz)
	{
		return new SanitizedType(clazz);
	}

	public static SanitizedType fromMethodReturnType(Method method)
	{
		return SanitizedType.fromType(method.getGenericReturnType());
	}

	public static SanitizedType fromType(Type type)
	{
		// <?>
		if(type instanceof WildcardType)
		{
			return new SanitizedType(Wildcard.class);
		}
		// <T>
		else if(type instanceof TypeVariable<?>)
		{
			return new SanitizedType(Variable.class);
		}
		// Known<Parameter, Types>
		else if(type instanceof ParameterizedType)
		{
			ParameterizedType parameterizedType = (ParameterizedType) type;
			Class<?> rawType = (Class<?>) parameterizedType.getRawType();

			// Map<..., ...> or Collection<...>
			// if parameterized type is a Collection or Map (those are the only ParameterizedTypes we really care to drill into)
			if(Collection.class.isAssignableFrom(rawType) || Map.class.isAssignableFrom(rawType))
			{
				return new SanitizedType(parameterizedType);
			}
			// we don't care to inspect other parameterized types, just use the raw type
			else
			{
				return new SanitizedType(rawType);
			}

		}
		// just a plain old Java class
		else
		{
			return new SanitizedType((Class<?>) type);
		}
	}

	public static SanitizedType fromParameterizedType(ParameterizedType type)
	{
		return new SanitizedType(type);
	}

	protected SanitizedType(ParameterizedType type)
	{
		// get the main part (the one Here<...>)
		baseType = (Class<?>) type.getRawType();

		// determine the parameterized parts (the stuff in ...<Here, AndHere>)
		for(Type actualTypeArgument : type.getActualTypeArguments())
		{
			if(actualTypeArgument instanceof WildcardType)
			{
				parameterizedTypes.add(new SanitizedType(Wildcard.class));
			}
			else if(actualTypeArgument instanceof TypeVariable<?>)
			{
				parameterizedTypes.add(new SanitizedType(Variable.class));
			}
			else if(actualTypeArgument instanceof ParameterizedType)
			{
				parameterizedTypes.add(new SanitizedType((ParameterizedType) actualTypeArgument));
			}
			else
			{
				parameterizedTypes.add(new SanitizedType((Class<?>) actualTypeArgument));
			}
		}
	}

	protected SanitizedType(Class<?> clazz)
	{
		if(clazz.isArray())
		{
			baseType = clazz.getComponentType();
			isArray = true;
		}
		else
		{
			baseType = clazz;
		}
	}

	@Override
	public String toString()
	{
		String baseName;
		if(baseType.equals(Wildcard.class))
			baseName = "?";
		else if(baseType.equals(Variable.class))
			baseName = "T";
		else
			baseName = baseType.getSimpleName();

		// example format: BaseClass<Any, ParamTypes, Recursive>[]
		return baseName + (!parameterizedTypes.isEmpty() ? "<" + StringUtils.join(parameterizedTypes, ", ") + ">" : "") + (isArray() ? "[]" : "");
	}

	public Class<?> getBaseType()
	{
		return baseType;
	}

	public boolean isVoid()
	{
		return baseType.equals(Void.TYPE);
	}

	public boolean isArray()
	{
		return isArray;
	}

	public List<SanitizedType> getParameterizedTypes()
	{
		return parameterizedTypes;
	}

	public Set<Class<?>> getReferencedTypes()
	{
		Set<Class<?>> referencedTypes = new HashSet<>();

		recurseReferencedTypes(this, referencedTypes);

		return referencedTypes;
	}

	private static void recurseReferencedTypes(SanitizedType sanitizedType, Set<Class<?>> referencedTypes)
	{
		referencedTypes.add(sanitizedType.getBaseType());

		for(SanitizedType parameterizedType : sanitizedType.getParameterizedTypes())
			recurseReferencedTypes(parameterizedType, referencedTypes);
	}
}
