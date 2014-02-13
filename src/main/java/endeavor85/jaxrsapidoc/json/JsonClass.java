package endeavor85.jaxrsapidoc.json;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import com.fasterxml.jackson.annotation.JsonView;

import endeavor85.jaxrsapidoc.SanitizedType;

public class JsonClass extends JsonType
{
	Map<String, JsonProperty>	properties	= new TreeMap<>();
	Set<Class<?>>				viewClasses	= new HashSet<>();
	String						jsonTypeProperty;
	String						jsonTypeName;					// string value used to indicate this type when deserializing super type

	public JsonClass(Class<?> clazz)
	{
		this(clazz, false);
	}

	public JsonClass(Class<?> clazz, boolean includeAllGetters)
	{
		super(clazz);
		abstractType = Modifier.isAbstract(getType().getModifiers());

		// if abstract, find type property
		if(abstractType)
		{
			JsonTypeInfo jsonTypeInfo = clazz.getAnnotation(JsonTypeInfo.class);

			if(jsonTypeInfo != null)
			{
				jsonTypeProperty = (jsonTypeInfo.property().isEmpty() ? jsonTypeInfo.use().getDefaultPropertyName() : jsonTypeInfo.property());
			}
		}

		for(Method method : clazz.getMethods())
		{
			boolean valid = false;
			JsonProperty property = new JsonProperty();

			com.fasterxml.jackson.annotation.JsonProperty jsonProperty = method.getAnnotation(com.fasterxml.jackson.annotation.JsonProperty.class);
			if(jsonProperty != null)
			{
				valid = true;
				property.setName(jsonProperty.value());
			}

			JsonUnwrapped jsonUnwrapped = method.getAnnotation(JsonUnwrapped.class);
			if(jsonUnwrapped != null)
			{
				valid = true;
				property.setUnwrapped(true);
			}

			// if override flag is set, include all getters regardless of the presence of the @JsonProperty annotation
			if(!valid && includeAllGetters && isGetter(method))
				valid = true;

			SanitizedType returnType = SanitizedType.fromMethodReturnType(method);

			// ignore void returns (probably just setters)
			if(returnType.isVoid())
				valid = false;

			if(valid)
			{
				property.setType(returnType);

				// if property name is unknown
				if(property.getName() == null)
				{
					// if property is unwrapped, just set a unique dummy name
					if(property.isUnwrapped())
					{
						property.setName(returnType.toString() + "-unwrapped");
					}
					// if property is not unwrapped, infer property name from method name
					else
					{
						String methodName = method.getName();

						String strip = null;
						if(methodName.startsWith("get"))
							strip = methodName.substring(3);
						else if(methodName.startsWith("is"))
							strip = methodName.substring(2);
						if(strip != null)
							property.setName(Character.toLowerCase(strip.charAt(0)) + strip.substring(1));
					}
				}

				JsonView jsonView = method.getAnnotation(JsonView.class);

				if(jsonView != null)
				{
					Class<?>[] viewClasses = jsonView.value();
					for(Class<?> viewClass : viewClasses)
						property.getViews().addAll(getViews(viewClass));

					// add views to set of all views for this class (this will ensure there are view columns for them in the table)
					this.viewClasses.addAll(property.getViews());
				}

				properties.put(property.getName(), property);
			}
		}
	}

	public Map<String, JsonProperty> getProperties()
	{
		return properties;
	}

	public Set<Class<?>> getViewClasses()
	{
		return viewClasses;
	}

	public String getJsonTypeProperty()
	{
		return jsonTypeProperty;
	}

	public void setJsonTypeProperty(String jsonTypeProperty)
	{
		this.jsonTypeProperty = jsonTypeProperty;
	}

	public String getJsonTypeName()
	{
		return jsonTypeName;
	}

	public void setJsonTypeName(String jsonTypeName)
	{
		this.jsonTypeName = jsonTypeName;
	}

	@Override
	public Set<Class<?>> getReferencedTypes()
	{
		Set<Class<?>> referencedTypes = new HashSet<>();

		for(JsonProperty property : properties.values())
			referencedTypes.addAll(property.getType().getReferencedTypes());

		return referencedTypes;
	}

	private static boolean isGetter(Method method)
	{
		if(!method.getName().startsWith("get") && !method.getName().startsWith("is"))
			return false;
		if(method.getParameterTypes().length != 0)
			return false;
		if(void.class.equals(method.getReturnType()))
			return false;
		return true;
	}

	private static List<Class<?>> getViews(Class<?> viewClass)
	{
		List<Class<?>> views = new ArrayList<Class<?>>();
		Class<?> declaringClass = viewClass.getDeclaringClass();
		Class<?> possibleViews[] = null;

		if(declaringClass != null)
		{
			possibleViews = declaringClass.getDeclaredClasses();

			for(Class<?> possibleView : possibleViews)
			{
				if(viewClass.isAssignableFrom(possibleView))
					views.add(possibleView);
			}
		}
		else
			views.add(viewClass);

		return views;
	}
}
