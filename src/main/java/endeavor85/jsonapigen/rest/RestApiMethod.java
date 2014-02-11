package endeavor85.jsonapigen.rest;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.HEAD;
import javax.ws.rs.OPTIONS;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.annotation.JsonView;

import endeavor85.jsonapigen.SanitizedType;
import endeavor85.jsonapigen.TypeUtil;
import endeavor85.jsonapigen.ViewClassUtil;

public class RestApiMethod implements Comparable<RestApiMethod>
{
	String				httpMethod;
	String				url;
	String				consumes[];
	String				produces[];
	String				returnType;
	Class<?>			jsonView;
	List<RestApiParam>	pathParams	= new ArrayList<>();
	List<RestApiParam>	queryParams	= new ArrayList<>();
	List<RestApiParam>	formParams	= new ArrayList<>();

	private RestApiMethod(Method method, String resourceUrl)
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
				RestApiParam param = new RestApiParam();

				Class<?> paramType = paramTypes[i];
				param.setType(paramType.getSimpleName());

				Annotation[] annotations = paramAnnotations[i];
				if(annotations != null)
				{
					if(annotations.length == 0)
					{
						// TODO: assuming no annotation implies FormParam
						formParams.add(param);
					}
					else
					{
						for(int j = 0; j < annotations.length; j++)
						{
							Annotation annotation = annotations[j];

							Class<?> annotationType = annotation.annotationType();

							try
							{
								param.setValue((String) annotationType.getMethod("value").invoke(annotation));
							}
							catch(Exception e)
							{
								e.printStackTrace();
							}

							if(annotationType == PathParam.class)
							{
								pathParams.add(param);
							}
							else if(annotationType == QueryParam.class)
							{
								queryParams.add(param);
							}
							else if(annotationType == FormParam.class)
							{
								formParams.add(param);
							}
						}
					}
				}
			}
		}

		SanitizedType sanitizedReturnType = TypeUtil.getReturnType(method);

		returnType = sanitizedReturnType == null ? "null" : sanitizedReturnType.toString();
	}

	public static RestApiMethod parseMethod(Method method, String resourceRootUrl)
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
			RestApiMethod apiMethod = new RestApiMethod(method, resourceRootUrl);
			apiMethod.setHttpMethod(httpMethod);
			return apiMethod;
		}
		else
			return null;
	}

	public String getHttpMethod()
	{
		return httpMethod;
	}

	public void setHttpMethod(String httpMethod)
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
	public int compareTo(RestApiMethod other)
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

	public String getReturnType()
	{
		return returnType;
	}

	public void setReturnType(String returnType)
	{
		this.returnType = returnType;
	}

	public List<RestApiParam> getPathParams()
	{
		return pathParams;
	}

	public List<RestApiParam> getQueryParams()
	{
		return queryParams;
	}

	public List<RestApiParam> getFormParams()
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

	public String toTableRow()
	{
		StringBuilder row = new StringBuilder("  <tr>");
		row.append("<th><tt>" + httpMethod + "</tt></th>");
		row.append("<th><tt>" + url + "</tt></th>");
		row.append(getCell(consumes == null ? "" : ("<tt>" + StringUtils.join(consumes, "</tt><br/><tt>") + "</tt>")));
		row.append(getCell(produces == null ? "" : ("<tt>" + StringUtils.join(produces, "</tt><br/><tt>") + "</tt>")));
		row.append("</tr>\n  <tr>\n    <td colspan=\"4\">");
		if(!pathParams.isEmpty())
		{
			row.append("Path Parameters:");
			row.append("\n      <ul>");
			for(RestApiParam param : pathParams)
			{
				row.append("\n        <li><tt>" + param.getValue() + "</tt> : <tt>" + param.getType() + "</tt></li>");
			}
			row.append("\n      </ul>");
		}
		if(!queryParams.isEmpty())
		{
			row.append("Query Parameters:");
			row.append("\n      <ul>");
			for(RestApiParam param : queryParams)
			{
				row.append("\n        <li><tt>" + param.getValue() + "</tt> : <tt>" + param.getType() + "</tt></li>");
			}
			row.append("\n      </ul>");
		}
		if(!formParams.isEmpty())
		{
			row.append("Form Parameters:");
			row.append("\n      <ul>");
			for(RestApiParam param : formParams)
			{
				row.append("\n        <li>" + (param.getValue() == null ? "" : "<tt>" + param.getValue() + "</tt> : ") + "<tt>" + param.getType() + "</tt></li>");
			}
			row.append("\n      </ul>");
		}
		if(returnType != null)
		{
			row.append("Returns:");
			row.append("\n      <ul>");
			if(returnType != null)
			{
				row.append("\n        <li><tt>" + returnType + "</tt>");
				if(jsonView != null)
					row.append(" (view: <tt>" + ViewClassUtil.getSanitizedViewClassName(jsonView) + "</tt>)");
				row.append("</li>");
			}
			row.append("\n      </ul>");
		}
		row.append("\n    </td>\n  </tr>\n");
		return row.toString();
	}

	private String getCell(String content)
	{
		return "<td>" + content + "</td>";
	}

	public Set<? extends Class<?>> getReferencedTypes()
	{
		Set<? extends Class<?>> referencedTypes = new HashSet<>();
		
		// TODO: implement
		
		return referencedTypes;
	}
}
