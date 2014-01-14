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
	public static String getReturnTypeStr(Method method)
	{
		Class<?> returnType = method.getReturnType();

		if(returnType != null)
		{
			if(returnType.isArray())
			{
				return returnType.getComponentType().getSimpleName() + "[]";
			}
			else if(Collection.class.isAssignableFrom(returnType))
			{
				String typeStr = "";
				Type collectionType = method.getGenericReturnType();
				if(collectionType instanceof ParameterizedType)
				{
					List<String> typeArgs = new ArrayList<String>();
					ParameterizedType type = (ParameterizedType) collectionType;
					Type[] typeArguments = type.getActualTypeArguments();
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

					typeStr += StringUtils.join(typeArgs, ",");
				}
				return typeStr + "[]";
			}
			else
			{
				return method.getReturnType().getSimpleName();
			}
		}
		else
			return null;
	}
}
