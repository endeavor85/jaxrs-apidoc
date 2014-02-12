package endeavor85.jsonapigen.rest;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.ws.rs.Path;

public class RestResource
{
	private Class<?>			resourceClass;
	private String				resourceRootUrl	= null;
	private List<RestMethod>	apiMethods		= new ArrayList<RestMethod>();

	private RestResource(Class<?> clazz)
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
				RestMethod apiMethod = RestMethod.parseMethod(method, resourceRootUrl);

				// if method is a REST method, add it to apiMethods
				if(apiMethod != null)
					apiMethods.add(apiMethod);
			}

			// sort API methods alphabetically
			Collections.sort(apiMethods);
		}
	}

	public static RestResource parseClass(Class<?> clazz)
	{
		RestResource restResource = new RestResource(clazz);

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

	public List<RestMethod> getApiMethods()
	{
		return apiMethods;
	}

	public void setApiMethods(List<RestMethod> apiMethods)
	{
		this.apiMethods = apiMethods;
	}

	public Set<Class<?>> getReferencedTypes()
	{
		Set<Class<?>> referencedTypes = new HashSet<>();

		for(RestMethod apiMethod : apiMethods)
			referencedTypes.addAll(apiMethod.getReferencedTypes());

		return referencedTypes;
	}

	public Class<?> getResourceClass()
	{
		return resourceClass;
	}

	public void setResourceClass(Class<?> resourceClass)
	{
		this.resourceClass = resourceClass;
	}

	public String getResourceRootUrl()
	{
		return resourceRootUrl;
	}

	public void setResourceRootUrl(String resourceRootUrl)
	{
		this.resourceRootUrl = resourceRootUrl;
	}
}
