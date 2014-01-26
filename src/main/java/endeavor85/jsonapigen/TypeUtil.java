package endeavor85.jsonapigen;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

public class TypeUtil
{
	public static SanitizedType getReturnType(Method method)
	{
		SanitizedType sanitizedType = null;

		Class<?> returnType = method.getReturnType();

		if(returnType != null)
		{
			sanitizedType = new SanitizedType();

			if(returnType.isArray())
			{
				sanitizedType.setArray(true);
				sanitizedType.setType(returnType.getComponentType());
			}
			else if(Collection.class.isAssignableFrom(returnType))
			{
				sanitizedType.setArray(true);
				Type collectionType = method.getGenericReturnType();
				if(collectionType instanceof ParameterizedType)
				{
					ParameterizedType type = (ParameterizedType) collectionType;
					Type[] typeArguments = type.getActualTypeArguments();

					if(typeArguments.length == 1)
					{
						List<String> typeArgs = new ArrayList<String>();

						for(Type typeArgument : typeArguments)
						{
							if(typeArgument instanceof WildcardType)
							{
								typeArgs.add(((WildcardType) typeArgument).toString());
							}
							else if(typeArgument instanceof TypeVariable<?>)
							{
								typeArgs.add(((TypeVariable<?>) typeArgument).toString());
							}
							else
							{
								typeArgs.add(((Class<?>) typeArgument).getSimpleName());
							}
						}

						// TODO: need to find a way to link the actual class types
						sanitizedType.setTypeStr(StringUtils.join(typeArgs, ","));
					}
					else
					{
						Type typeArgument = typeArguments[0];

						if(typeArgument instanceof WildcardType)
						{
							// TODO: need to find a way to link the actual class types
							sanitizedType.setTypeStr(((WildcardType) typeArgument).toString());
						}
						else if(typeArgument instanceof TypeVariable<?>)
						{
							// TODO: need to find a way to link the actual class types
							sanitizedType.setTypeStr(((TypeVariable<?>) typeArgument).toString());
						}
						else
						{
							sanitizedType.setType((Class<?>) typeArgument);
						}
					}
				}
			}
			else
			{
				sanitizedType.setArray(false);
				sanitizedType.setType(method.getReturnType());
			}
		}

		return sanitizedType;
	}
}
