package endeavor85.jsonapigen;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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

import com.google.common.reflect.ClassPath;

public class RESTAPIGen
{
	public static void main(String[] args)
	{
		if(args.length > 0)
		{
			for(String arg : args)
				new RESTAPIGen(arg);
		}
		else
			System.err.println("Add package name arguments (space-separated). E.g., java RESTAPIGen com.");
	}

	public RESTAPIGen(String rootPackageName)
	{
		try
		{
			ClassPath classpath = ClassPath.from(RESTAPIGen.class.getClassLoader());
			for(ClassPath.ClassInfo classInfo : classpath.getTopLevelClassesRecursive(rootPackageName))
			{
				Class<?> clazz = classInfo.load();
				if(!clazz.isEnum())
					inspectClass(clazz);
			}
		}
		catch(IOException e)
		{
			e.printStackTrace();
		}
	}

	private void inspectClass(Class<?> clazz)
	{
		Path resourcePath = clazz.getAnnotation(Path.class);

		// if this is a resource class
		if(resourcePath != null)
		{
			List<RestApiMethod> apiMethods = new ArrayList<RestApiMethod>();

			for(Method method : clazz.getMethods())
			{
				RestApiMethod apiMethod = new RestApiMethod();

				// get HTTP Method
				if(method.isAnnotationPresent(GET.class))
					apiMethod.setHttpMethod("GET");
				else if(method.isAnnotationPresent(PUT.class))
					apiMethod.setHttpMethod("PUT");
				else if(method.isAnnotationPresent(POST.class))
					apiMethod.setHttpMethod("POST");
				else if(method.isAnnotationPresent(DELETE.class))
					apiMethod.setHttpMethod("DELETE");
				else if(method.isAnnotationPresent(HEAD.class))
					apiMethod.setHttpMethod("HEAD");
				else if(method.isAnnotationPresent(OPTIONS.class))
					apiMethod.setHttpMethod("OPTIONS");

				// if this method is annotated with a HTTP Method
				if(apiMethod.getHttpMethod() != null)
				{
					Consumes consumes = method.getAnnotation(Consumes.class);
					if(consumes != null)
						apiMethod.setConsumes(consumes.value());

					Produces produces = method.getAnnotation(Produces.class);
					if(produces != null)
						apiMethod.setProduces(produces.value());

					// start with resource path
					apiMethod.setUrl(resourcePath.value());

					// add method path if present
					Path path = method.getAnnotation(Path.class);
					if(path != null)
						apiMethod.setUrl(apiMethod.getUrl() + "/" + path.value());

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
								for(int j = 0; j < annotations.length; j++)
								{
									Annotation annotation = annotations[j];

									Class<?> annotationType = annotation.annotationType();

									if(annotationType == PathParam.class)
									{
										param.setName(annotation.toString());
										apiMethod.getPathParams().add(param);
									}
									else if(annotationType == QueryParam.class)
									{
										param.setName(annotation.toString());
										apiMethod.getQueryParams().add(param);
									}
									else if(annotationType == FormParam.class)
									{
										param.setName(annotation.toString());
										apiMethod.getFormParams().add(param);
									}
									else
									{
										param.setName(annotation.toString());
										apiMethod.getFormParams().add(param);
									}
								}
							}
						}
					}

					Class<?> returnType = method.getReturnType();
					apiMethod.setReturnType(returnType.getSimpleName());

					apiMethods.add(apiMethod);
				}
			}

			if(!apiMethods.isEmpty())
			{
				Collections.sort(apiMethods);

				StringBuilder table = new StringBuilder("### " + clazz.getSimpleName() + "\n\n");
				table.append("<table>\n");
				table.append("  <tr><th>Method</th><th>URL</th><th>Consumes</th><th>Produces</th></tr>\n");
				for(RestApiMethod apiMethod : apiMethods)
					table.append(apiMethodToRow(apiMethod));
				table.append("</table>\n");
				System.out.println(table.toString());
			}
		}
	}

	private String apiMethodToRow(RestApiMethod apiMethod)
	{
		StringBuilder row = new StringBuilder("  <tr>");
		row.append(getCell("<tt>" + apiMethod.getHttpMethod() + "</tt>"));
		row.append(getCell("<tt>" + apiMethod.getUrl() + "</tt>"));
		row.append(getCell(apiMethod.getConsumes() == null ? "" : ("<tt>" + StringUtils.join(apiMethod.getConsumes(), "</tt><br/><tt>") + "</tt>")));
		row.append(getCell(apiMethod.getProduces() == null ? "" : ("<tt>" + StringUtils.join(apiMethod.getProduces(), "</tt><br/><tt>") + "</tt>")));
		row.append("\n  <tr><td colspan=\"4\">");
		row.append("Consumes:");
		row.append("<ul>");
		if(apiMethod.getConsumes() != null)
		{
			for(String consume : apiMethod.getConsumes())
				row.append("<li><tt>" + consume + "</tt></li>");
		}
		else
		{
			row.append("<li><em>none</em></li>");
		}
		row.append("</ul>");
		row.append("Produces:");
		row.append("<ul>");
		if(apiMethod.getProduces() != null)
		{
			for(String produce : apiMethod.getProduces())
				row.append("<li><tt>" + apiMethod.getReturnType() + "</tt> <tt>" + produce + "</tt></li>");
		}
		else
		{
			row.append("<li><em>none</em></li>");
		}
		row.append("</ul>");
		row.append("Path Parameters:");
		row.append("<ul>");
		if(!apiMethod.getPathParams().isEmpty())
		{
			for(RestApiParam param : apiMethod.getPathParams())
			{
				row.append("<li><tt>" + param.getName() + "</tt> <tt>" + param.getType() + "</tt></li>");
			}
		}
		else
		{
			row.append("<li><em>none</em></li>");
		}
		row.append("</ul>");
		row.append("</ul>");
		row.append("Query Parameters:");
		row.append("<ul>");
		if(!apiMethod.getQueryParams().isEmpty())
		{
			for(RestApiParam param : apiMethod.getQueryParams())
			{
				row.append("<li><tt>" + param.getName() + "</tt> <tt>" + param.getType() + "</tt></li>");
			}
		}
		else
		{
			row.append("<li><em>none</em></li>");
		}
		row.append("</ul>");
		row.append("</ul>");
		row.append("Form Parameters:");
		row.append("<ul>");
		if(!apiMethod.getFormParams().isEmpty())
		{
			for(RestApiParam param : apiMethod.getFormParams())
			{
				row.append("<li><tt>" + param.getName() + "</tt> <tt>" + param.getType() + "</tt></li>");
			}
		}
		else
		{
			row.append("<li><em>none</em></li>");
		}
		row.append("</ul>");
		row.append("</td></tr>\n");
		return row.append("</tr>\n").toString();
	}

	private String getCell(String content)
	{
		return "<td>" + content + "</td>";
	}

	private static class RestApiMethod implements Comparable<RestApiMethod>
	{
		String				httpMethod;
		String				url;
		String				consumes[];
		String				produces[];
		String				returnType;
		List<RestApiParam>	pathParams	= new ArrayList<>();
		List<RestApiParam>	queryParams	= new ArrayList<>();
		List<RestApiParam>	formParams	= new ArrayList<>();

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

		public void setPathParams(List<RestApiParam> pathParams)
		{
			this.pathParams = pathParams;
		}

		public List<RestApiParam> getQueryParams()
		{
			return queryParams;
		}

		public void setQueryParams(List<RestApiParam> queryParams)
		{
			this.queryParams = queryParams;
		}

		public List<RestApiParam> getFormParams()
		{
			return formParams;
		}

		public void setFormParams(List<RestApiParam> formParams)
		{
			this.formParams = formParams;
		}
	}

	private class RestApiParam
	{
		String	type;
		String	name;

		public String getType()
		{
			return type;
		}

		public void setType(String type)
		{
			this.type = type;
		}

		public String getName()
		{
			return name;
		}

		public void setName(String name)
		{
			this.name = name;
		}
	}
}
