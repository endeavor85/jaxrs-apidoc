package endeavor85.jaxrsapidoc.rest;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HEAD;
import javax.ws.rs.OPTIONS;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import com.fasterxml.jackson.annotation.JsonView;

import endeavor85.jaxrsapidoc.SanitizedType;

public class RestMethod implements Comparable<RestMethod>
{
	HttpMethod			httpMethod;
	String				url;
	String				consumes[];
	String				produces[];
	SanitizedType		returnType;
	Class<?>			jsonView;
	List<RestParam>	pathParams	= new ArrayList<>();
	List<RestParam>	queryParams	= new ArrayList<>();
	List<RestParam>	formParams	= new ArrayList<>();

	private RestMethod(Method method, String resourceUrl)
	{
		Consumes consumesAnnot = method.getAnnotation(Consumes.class);
		if(consumes != null)
			consumes = consumesAnnot.value();

		Produces producesAnnot = method.getAnnotation(Produces.class);
		if(produces != null)
			produces = producesAnnot.value();

		JsonView jsonView = method.getAnnotation(JsonView.class);
		if(jsonView != null)
		{
			Class<?>[] viewClasses = jsonView.value();
			if(viewClasses.length == 1)
				setJsonView(viewClasses[0]);
		}

		// start with resource path
		url = resourceUrl;

		// add method path if present
		Path path = method.getAnnotation(Path.class);
		if(path != null)
			url = resourceUrl + "/" + path.value();

		// parse parameters for PathParams, QueryParams, and FormParams
		Annotation[][] paramAnnotations = method.getParameterAnnotations();
		Class<?>[] paramTypes = method.getParameterTypes();

		if(paramTypes != null)
		{
			for(int i = 0; i < paramTypes.length; i++)
			{
				RestParam param = new RestParam(paramTypes[i], paramAnnotations[i]);

				if(param != null)
				{
					switch(param.getMeans())
					{
					case Path:
						pathParams.add(param);
						break;
					case Query:
						queryParams.add(param);
						break;
					case Form:
					default:
						formParams.add(param);
					}
				}
			}
		}

		returnType = SanitizedType.fromMethodReturnType(method);
	}

	public static RestMethod parseMethod(Method method, String resourceRootUrl)
	{
		String httpMethod = null;

		// get HTTP Method
		if(method.isAnnotationPresent(GET.class))
			httpMethod = "GET";
		else if(method.isAnnotationPresent(PUT.class))
			httpMethod = "PUT";
		else if(method.isAnnotationPresent(POST.class))
			httpMethod = "POST";
		else if(method.isAnnotationPresent(DELETE.class))
			httpMethod = "DELETE";
		else if(method.isAnnotationPresent(HEAD.class))
			httpMethod = "HEAD";
		else if(method.isAnnotationPresent(OPTIONS.class))
			httpMethod = "OPTIONS";

		// if this method is annotated with a HTTP Method
		if(httpMethod != null)
		{
			RestMethod apiMethod = new RestMethod(method, resourceRootUrl);
			apiMethod.setHttpMethod(httpMethod);
			return apiMethod;
		}
		else
			return null;
	}

	public HttpMethod getHttpMethod()
	{
		return httpMethod;
	}

	public void setHttpMethod(String httpMethod)
	{
		this.httpMethod = HttpMethod.valueOf(httpMethod);
	}

	public void setHttpMethod(HttpMethod httpMethod)
	{
		this.httpMethod = httpMethod;
	}

	public String getUrl()
	{
		return url;
	}

	public void setUrl(String url)
	{
		this.url = url;
	}

	public String[] getConsumes()
	{
		return consumes;
	}

	public void setConsumes(String consumes[])
	{
		this.consumes = consumes;
	}

	public String[] getProduces()
	{
		return produces;
	}

	public void setProduces(String produces[])
	{
		this.produces = produces;
	}

	@Override
	public int compareTo(RestMethod other)
	{
		int result = this.url.compareTo(other.getUrl());
		if(result == 0)
			result = other.getMethodOrdinal() - this.getMethodOrdinal();
		return result;
	}

	private int getMethodOrdinal()
	{
		if(httpMethod == null)
			return -1;
		else if(httpMethod.equals("HEAD"))
			return 0;
		else if(httpMethod.equals("OPTIONS"))
			return 1;
		else if(httpMethod.equals("GET"))
			return 2;
		else if(httpMethod.equals("POST"))
			return 3;
		else if(httpMethod.equals("PUT"))
			return 4;
		else if(httpMethod.equals("DELETE"))
			return 5;
		else
			return 0;
	}

	public SanitizedType getReturnType()
	{
		return returnType;
	}

	public List<RestParam> getPathParams()
	{
		return pathParams;
	}

	public List<RestParam> getQueryParams()
	{
		return queryParams;
	}

	public List<RestParam> getFormParams()
	{
		return formParams;
	}

	public Class<?> getJsonView()
	{
		return jsonView;
	}

	public void setJsonView(Class<?> jsonView)
	{
		this.jsonView = jsonView;
	}

	public Set<Class<?>> getReferencedTypes()
	{
		Set<Class<?>> referencedTypes = new HashSet<>();

		List<RestParam> allParams = new ArrayList<>();
		allParams.addAll(pathParams);
		allParams.addAll(queryParams);
		allParams.addAll(formParams);

		for(RestParam param : allParams)
			referencedTypes.addAll(param.getType().getReferencedTypes());

		if(returnType != null)
			referencedTypes.addAll(returnType.getReferencedTypes());

		return referencedTypes;
	}

	public enum HttpMethod
	{
		GET, PUT, POST, DELETE, HEAD, OPTIONS, PATCH
	}
}
