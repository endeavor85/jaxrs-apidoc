package endeavor85.jsonapigen.rest;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.ws.rs.Path;

public class RestApiResource
{
	private Class<?>			resourceClass;
	private String				resourceRootUrl	= null;
	private List<RestApiMethod>	apiMethods		= new ArrayList<RestApiMethod>();

	private RestApiResource(Class<?> clazz)
	{
		resourceClass = clazz;

		Path resourcePath = clazz.getAnnotation(Path.class);

		// if this is a resource class
		if(resourcePath != null)
		{
			resourceRootUrl = resourcePath.value();

			// parse each of the resource's methods
			for(Method method : clazz.getMethods())
			{
				RestApiMethod apiMethod = RestApiMethod.parseMethod(method, resourceRootUrl);

				// if method is a REST method, add it to apiMethods
				if(apiMethod != null)
					apiMethods.add(apiMethod);
			}

			// sort API methods alphabetically
			Collections.sort(apiMethods);
		}
	}

	public static RestApiResource parseClass(Class<?> clazz)
	{
		RestApiResource restResource = new RestApiResource(clazz);

		if(restResource != null && restResource.getResourceUrl() != null)
			return restResource;
		else
			return null;
	}

	public String getResourceUrl()
	{
		return resourceRootUrl;
	}

	public void setResourceUrl(String resourceUrl)
	{
		this.resourceRootUrl = resourceUrl;
	}

	public List<RestApiMethod> getApiMethods()
	{
		return apiMethods;
	}

	public void setApiMethods(List<RestApiMethod> apiMethods)
	{
		this.apiMethods = apiMethods;
	}

	public Set<Class<?>> getReferencedTypes()
	{
		Set<Class<?>> referencedTypes = new HashSet<>();

		for(RestApiMethod apiMethod : apiMethods)
			referencedTypes.addAll(apiMethod.getReferencedTypes());

		return referencedTypes;
	}

	public String toString()
	{
		StringBuilder table = new StringBuilder("### " + resourceClass.getSimpleName() + "\n\n");
		table.append("<table>\n");
		table.append("  <tr><th>Method</th><th>URL</th><th>Consumes</th><th>Produces</th></tr>\n");
		for(RestApiMethod apiMethod : apiMethods)
			table.append(apiMethod.toTableRow());
		table.append("</table>\n");
		return table.toString();
	}
}
