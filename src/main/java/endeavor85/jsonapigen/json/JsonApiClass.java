package endeavor85.jsonapigen.json;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonView;

import endeavor85.jsonapigen.SanitizedType;
import endeavor85.jsonapigen.TypeUtil;
import endeavor85.jsonapigen.ViewClassUtil;

public class JsonApiClass extends JsonApiType
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

	public Set<Class<?>> getViewClasses()
	{
		return viewClasses;
	}

	public void setViewClasses(Set<Class<?>> viewClasses)
	{
		this.viewClasses = viewClasses;
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

	public void setProperties(List<JsonApiProperty> properties)
	{
		this.properties = properties;
	}
}