package endeavor85.jsonapigen;

import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.annotation.JsonView;
import com.google.common.reflect.ClassPath;

public class JSONAPIGen
{
	public static void main(String[] args)
	{
		if(args.length > 0)
			new JSONAPIGen(args);
		else
			System.err.println("Add package name arguments (space-separated). E.g., java JSONAPIGen com.");
	}

	Map<String, JsonApiType>	jsonType	= new TreeMap<>();
	Set<Class<?>>				parsedTypes	= new HashSet<>();

	public JSONAPIGen(String[] rootPackages)
	{
		List<Class<?>> types = new ArrayList<>();

		// inspect packages for types
		for(String rootPackageName : rootPackages)
		{
			try
			{
				ClassPath classpath = ClassPath.from(JSONAPIGen.class.getClassLoader());
				for(ClassPath.ClassInfo classInfo : classpath.getTopLevelClassesRecursive(rootPackageName))
				{
					// inspect top level class for inner classes (recursively)
					Class<?> topLevelClass = classInfo.load();
					types.addAll(inspectTypesRecursive(topLevelClass));
				}
			}
			catch(IOException e)
			{
				e.printStackTrace();
			}
		}

		// parse included classes
		for(Class<?> type : types)
		{
			if(type.isEnum())
			{
				jsonType.put(type.getSimpleName(), new JsonApiEnum(type));
				parsedTypes.add(type);
			}
			else
			{
				JsonApiClass classType = new JsonApiClass(type);

				// only include types that have json properties
				if(!classType.getProperties().isEmpty())
				{
					jsonType.put(type.getSimpleName(), classType);
					parsedTypes.add(type);
				}
			}
		}

		// check inheritance
		for(JsonApiType type : jsonType.values())
		{
			if(type instanceof JsonApiClass)
			{
				JsonApiClass apiClass = (JsonApiClass) type;
				Class<?> superclass = apiClass.getType().getSuperclass();
				JsonApiType parentType = jsonType.get(superclass.getSimpleName());
				if(parentType != null && parentType instanceof JsonApiClass)
				{
					JsonApiClass parentApiType = (JsonApiClass) parentType;
					parentApiType.subTypes.add(apiClass);
					// copy json type property from parent (will need to display this along with this type's specific name value)
					apiClass.jsonTypeProperty = parentApiType.jsonTypeProperty;

					// look for json type name
					JsonTypeName jsonTypeNameAnnot = apiClass.getType().getAnnotation(JsonTypeName.class);
					if(jsonTypeNameAnnot != null)
					{
						apiClass.jsonTypeName = jsonTypeNameAnnot.value();
					}
				}
			}
		}

		// write markdown
		for(JsonApiType type : jsonType.values())
		{
			if(type instanceof JsonApiClass)
				System.out.print(((JsonApiClass) type).toString(parsedTypes));
			else
				System.out.print(type.toString());
		}
	}

	private List<Class<?>> inspectTypesRecursive(Class<?> rootType)
	{
		List<Class<?>> types = new ArrayList<>();

		// include root type
		types.add(rootType);

		// recursively inspect inner classes
		for(Class<?> innerClass : rootType.getDeclaredClasses())
			types.addAll(inspectTypesRecursive(innerClass));
		
		return types;
	}

	private static class JsonApiType
	{
		boolean				abstractType;
		Class<?>			type;
		Set<JsonApiType>	subTypes	= new HashSet<>();

		public JsonApiType(Class<?> type)
		{
			this.type = type;
		}

		public Class<?> getType()
		{
			return type;
		}

		protected String getHyperlinkFor(Class<?> clazz)
		{
			return "<a href='#" + clazz.getSimpleName().toLowerCase() + "'>" + clazz.getSimpleName() + "</a>";
		}

		@Override
		public String toString()
		{
			String abstractWrapper = abstractType ? "_" : "";
			return "### " + abstractWrapper + getType().getSimpleName() + abstractWrapper + "\n_(" + getType().getName() + ")_\n\n";
		}
	}

	private static class JsonApiEnum extends JsonApiType
	{
		List<String>	values;

		public JsonApiEnum(Class<?> enumType)
		{
			super(enumType);
			abstractType = false;

			values = new ArrayList<>();

			for(Object o : enumType.getEnumConstants())
				values.add(o.toString());
		}

		@Override
		public String toString()
		{
			StringBuilder result = new StringBuilder(super.toString());
			result.append("<tt>" + StringUtils.join(values, "</tt> | <tt>") + "</tt>");
			result.append("\n\n");
			return result.toString();
		}
	}

	private static class JsonApiClass extends JsonApiType
	{
		List<JsonApiProperty>	properties	= new ArrayList<>();
		Set<Class<?>>			viewClasses	= new HashSet<>();
		String					jsonTypeProperty;
		String					jsonTypeName;

		public JsonApiClass(Class<?> clazz)
		{
			this(clazz, false);
		}

		public JsonApiClass(Class<?> clazz, boolean includeAllGetters)
		{
			super(clazz);
			abstractType = Modifier.isAbstract(type.getModifiers());

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
					Class<?>[] viewClasses = jsonView.value();
					for(Class<?> viewClass : viewClasses)
						property.getViews().addAll(getViews(viewClass));

					// add views to set of all views for this class (this will ensure there are view columns for them in the table)
					this.viewClasses.addAll(property.getViews());
				}

				if(!valid && includeAllGetters && isGetter(method))
					valid = true;

				SanitizedType returnType = TypeUtil.getReturnType(method);
				// ignore void returns (probably just setters)
				if(returnType == null)
					valid = false;

				if(valid)
				{
					property.setType(returnType);

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

					properties.add(property);
				}
			}
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

		public List<JsonApiProperty> getProperties()
		{
			return properties;
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
				result.append("  <tr><th rowspan='2'>Property name</th><th rowspan='2'>Type</th>" + (viewNames.isEmpty() ? "" : ("<th colspan='" + viewNames.size() + "'>Views</th>")) + "</tr>\n");
				result.append("  <tr>");
				for(String viewName : viewNames)
					result.append("<th>" + viewName + "</th>");
				result.append("</tr>\n");

				// add type property if necessary
				if(jsonTypeProperty != null && !jsonTypeProperty.isEmpty())
				{
					result.append("  <tr><td><tt>" + jsonTypeProperty + "</tt></td>");

					// if this is the parent type
					if(abstractType)
						result.append(getCell("<tt>String</tt> <i>(implementor's type)</i>"));
					else
						result.append(getCell("<tt>String</tt> = <tt>\"" + jsonTypeName + "\"</tt>"));

					for(int i = 0; i < viewClasses.size(); i++)
						result.append(getCell("&#x2713;"));

					result.append("</tr>\n");
				}

				// add annotated json properties
				Map<String, String> propertyRows = new TreeMap<>();

				for(JsonApiProperty property : properties)
				{
					StringBuilder row = new StringBuilder("  <tr>");
					row.append(getCell("<tt>" + property.getName() + "</tt>"));
					SanitizedType returnType = property.getType();
					if(returnType.getType() != null && hyperlinkToTypes.contains(returnType.getType()))
						row.append(getCell("<tt>" + getHyperlinkFor(returnType.getType()) + (returnType.isArray() ? "[]" : "") + "</tt>"));
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

			// for abstract types, check for implementors and list them
			if(abstractType && !subTypes.isEmpty())
			{
				result.append("##### Implementors\n\n");
				for(JsonApiType subType : subTypes)
				{
					result.append("* " + getHyperlinkFor(subType.getType()) + "\n");
				}
				result.append("\n");
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
