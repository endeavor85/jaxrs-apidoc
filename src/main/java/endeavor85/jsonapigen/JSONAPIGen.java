package endeavor85.jsonapigen;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonView;
import com.google.common.reflect.ClassPath;

public class JSONAPIGen
{
	public static void main(String[] args)
	{
		if(args.length > 0)
		{
			new JSONAPIGen(args);
		}
		else
			System.err.println("Add package name arguments (space-separated). E.g., java JSONAPIGen com.");
	}

	Map<String, JsonApiType>	jsonType	= new TreeMap<>();
	Set<Class<?>>				parsedTypes	= new HashSet<>();

	public JSONAPIGen(String[] rootPackages)
	{
		for(String rootPackageName : rootPackages)
		{
			try
			{
				ClassPath classpath = ClassPath.from(JSONAPIGen.class.getClassLoader());
				for(ClassPath.ClassInfo classInfo : classpath.getTopLevelClassesRecursive(rootPackageName))
				{
					Class<?> clazz = classInfo.load();
					if(clazz.isEnum())
					{
						jsonType.put(clazz.getSimpleName(), inspectEnum(clazz));
						parsedTypes.add(clazz);
					}
					else
					{
						JsonApiClass classType = inspectClass(clazz, false);
						// only include types that have json properties
						if(!classType.getProperties().isEmpty())
						{
							jsonType.put(clazz.getSimpleName(), classType);
							parsedTypes.add(clazz);
						}
					}
				}
			}
			catch(IOException e)
			{
				e.printStackTrace();
			}
		}

		for(JsonApiType type : jsonType.values())
		{
			if(type instanceof JsonApiClass)
				System.out.print(((JsonApiClass) type).toString(parsedTypes));
			else
				System.out.print(type.toString());
		}
	}

	private JsonApiEnum inspectEnum(Class<?> enumClass)
	{
		JsonApiEnum enumType = new JsonApiEnum(enumClass);

		for(Object o : enumClass.getEnumConstants())
			enumType.getValues().add(o.toString());

		return enumType;
	}

	private JsonApiClass inspectClass(Class<?> clazz, boolean includeAllGetters)
	{
		JsonApiClass classType = new JsonApiClass(clazz);

		for(Method method : clazz.getMethods())
		{
			boolean valid = false;
			JsonApiProperty property = new JsonApiProperty();

			JsonProperty jsonProperty = method.getAnnotation(JsonProperty.class);

			if(jsonProperty != null)
			{
				valid = true;
				property.setName(jsonProperty.value());
			}

			JsonView jsonView = method.getAnnotation(JsonView.class);

			if(jsonView != null)
			{
				valid = true;
				Class<?>[] viewClasses = jsonView.value();
				for(Class<?> viewClass : viewClasses)
					property.getViews().addAll(getViews(viewClass));

				// add views to set of all views for this class (this will ensure there are view columns for them in the table)
				classType.getViewClasses().addAll(property.getViews());
			}

			if(!valid && includeAllGetters && isGetter(method))
				valid = true;

			if(valid)
			{
				// if property name is still unknown, infer from method name
				if(property.getName() == null)
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

				property.setType(TypeUtil.getReturnType(method));

				classType.getProperties().add(property);
			}
		}

		return classType;
	}

	private boolean isGetter(Method method)
	{
		if(!method.getName().startsWith("get") && !method.getName().startsWith("is"))
			return false;
		if(method.getParameterTypes().length != 0)
			return false;
		if(void.class.equals(method.getReturnType()))
			return false;
		return true;
	}

	private List<Class<?>> getViews(Class<?> viewClass)
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

	private static class JsonApiType
	{
		Class<?>	type;

		public JsonApiType(Class<?> type)
		{
			this.type = type;
		}

		public Class<?> getType()
		{
			return type;
		}

		@Override
		public String toString()
		{
			return "### " + getType().getSimpleName() + "\n\n";
		}
	}

	private static class JsonApiEnum extends JsonApiType
	{
		List<String>	values	= new ArrayList<>();

		public JsonApiEnum(Class<?> type)
		{
			super(type);
		}

		public List<String> getValues()
		{
			return values;
		}

		@Override
		public String toString()
		{
			StringBuilder result = new StringBuilder(super.toString());
			result.append("<tt>" + StringUtils.join(getValues(), "</tt> | <tt>") + "</tt>");
			result.append("\n\n");
			return result.toString();
		}
	}

	private static class JsonApiClass extends JsonApiType
	{
		List<JsonApiProperty>	properties	= new ArrayList<>();
		Set<Class<?>>			viewClasses	= new HashSet<Class<?>>();

		public JsonApiClass(Class<?> type)
		{
			super(type);
		}

		public List<JsonApiProperty> getProperties()
		{
			return properties;
		}

		public Set<Class<?>> getViewClasses()
		{
			return viewClasses;
		}

		public String toString(Set<Class<?>> hyperlinkToTypes)
		{
			if(hyperlinkToTypes == null)
				hyperlinkToTypes = new HashSet<>();

			StringBuilder result = new StringBuilder(super.toString());

			if(!properties.isEmpty())
			{
				List<String> viewNames = new ArrayList<String>(viewClasses.size());
				for(Class<?> viewClass : viewClasses)
					viewNames.add(ViewClassUtil.getSanitizedViewClassName(viewClass));

				result.append("<table>\n");
				result.append("  <tr><th rowspan='2'>Property name</th><th rowspan='2'>Type</th><th colspan='" + viewNames.size() + "'>Views</th></tr>\n");
				result.append("  <tr>");
				for(String viewName : viewNames)
					result.append("<th>" + viewName + "</th>");
				result.append("</tr>\n");

				Map<String, String> propertyRows = new TreeMap<>();

				for(JsonApiProperty property : properties)
				{
					StringBuilder row = new StringBuilder("  <tr>");
					row.append(getCell("<tt>" + property.getName() + "</tt>"));
					SanitizedType returnType = property.getType();
					if(returnType.getType() != null && hyperlinkToTypes.contains(returnType.getType()))
						row.append(getCell("<tt><a href='#" + returnType.getType().getSimpleName().toLowerCase() + "'>" + returnType.getType().getSimpleName() + (returnType.isArray() ? "[]" : "")
								+ "</a></tt>"));
					else
						row.append(getCell("<tt>" + returnType + "</tt>"));
					for(Class<?> viewClass : viewClasses)
					{
						if(property.getViews().contains(viewClass))
							row.append(getCell("&#x2713;"));
						else
							row.append(getCell("&nbsp;"));
					}
					row.append("</tr>\n");

					propertyRows.put(property.name, row.toString());
				}

				for(String propertyRow : propertyRows.values())
				{
					result.append(propertyRow);
				}

				result.append("</table>\n\n");
			}

			return result.toString();
		}

		@Override
		public String toString()
		{
			return this.toString(null);
		}

		private String getCell(String content)
		{
			return "<td>" + content + "</td>";
		}
	}

	private static class JsonApiProperty
	{
		String			name;
		SanitizedType	type;
		List<Class<?>>	views	= new ArrayList<>();

		public String getName()
		{
			return name;
		}

		public void setName(String name)
		{
			this.name = name;
		}

		public SanitizedType getType()
		{
			return type;
		}

		public void setType(SanitizedType type)
		{
			this.type = type;
		}

		public List<Class<?>> getViews()
		{
			return views;
		}
	}
}
