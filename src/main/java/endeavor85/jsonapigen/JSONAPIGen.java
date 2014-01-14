package endeavor85.jsonapigen;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
			for(String arg : args)
				new JSONAPIGen(arg);
		}
		else
			System.err.println("Add package name arguments (space-separated). E.g., java JSONAPIGen com.");
	}

	public JSONAPIGen(String rootPackageName)
	{
		try
		{
			ClassPath classpath = ClassPath.from(JSONAPIGen.class.getClassLoader());
			for(ClassPath.ClassInfo classInfo : classpath.getTopLevelClassesRecursive(rootPackageName))
			{
				Class<?> clazz = classInfo.load();
				if(clazz.isEnum())
					inspectEnum(clazz);
				else
					inspectClass(clazz, false);
			}
		}
		catch(IOException e)
		{
			e.printStackTrace();
		}
	}

	private void inspectEnum(Class<?> enumClass)
	{
		StringBuilder result = new StringBuilder("### " + enumClass.getSimpleName() + "\n\n");
		result.append("<tt>" + StringUtils.join(Arrays.asList(enumClass.getEnumConstants()), "</tt> | <tt>") + "</tt>");
		result.append("\n");
		System.out.println(result.toString());
	}

	private void inspectClass(Class<?> clazz, boolean includeAllGetters)
	{
		List<JsonApiProperty> properties = new ArrayList<JSONAPIGen.JsonApiProperty>();
		Set<Class<?>> allViewClasses = new HashSet<Class<?>>();

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
				allViewClasses.addAll(property.getViews());
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

				property.setType(TypeUtil.getReturnTypeStr(method));

				properties.add(property);
			}
		}

		if(!properties.isEmpty())
		{
			List<Class<?>> viewClassList = new ArrayList<Class<?>>(allViewClasses);
			List<String> viewNames = new ArrayList<String>(allViewClasses.size());
			for(Class<?> viewClass : allViewClasses)
				viewNames.add(ViewClassUtil.getSanitizedViewClassName(viewClass));

			StringBuilder table = new StringBuilder("### " + clazz.getSimpleName() + "\n\n");
			table.append("<table>\n");
			table.append("  <tr><th rowspan='2'>Property name</th><th rowspan='2'>Type</th><th colspan='" + viewNames.size() + "'>Views</th></tr>\n");
			table.append("  <tr>");
			for(String viewName : viewNames)
				table.append("<th>" + viewName + "</th>");
			table.append("</tr>\n");
			for(JsonApiProperty property : properties)
				table.append(propertyToRow(property, viewClassList));
			table.append("</table>\n");
			System.out.println(table.toString());
		}
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

	private String propertyToRow(JsonApiProperty property, List<Class<?>> allViewClasses)
	{
		StringBuilder row = new StringBuilder("  <tr>");
		row.append(getCell("<tt>" + property.getName() + "</tt>"));
		row.append(getCell("<tt>" + property.getType() + "</tt>"));
		for(Class<?> viewClass : allViewClasses)
		{
			if(property.getViews().contains(viewClass))
				row.append(getCell("&#x2713;"));
			else
				row.append(getCell("&nbsp;"));
		}
		return row.append("</tr>\n").toString();
	}

	private String getCell(String content)
	{
		return "<td>" + content + "</td>";
	}

	private static class JsonApiProperty
	{
		String			name;
		String			type;
		List<Class<?>>	views	= new ArrayList<Class<?>>();

		public String getName()
		{
			return name;
		}

		public void setName(String name)
		{
			this.name = name;
		}

		public String getType()
		{
			return type;
		}

		public void setType(String type)
		{
			this.type = type;
		}

		public List<Class<?>> getViews()
		{
			return views;
		}
	}
}
