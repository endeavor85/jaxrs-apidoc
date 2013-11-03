package endeavor85.jsonapigen;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HEAD;
import javax.ws.rs.OPTIONS;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

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

					apiMethods.add(apiMethod);
				}
			}

			if(!apiMethods.isEmpty())
			{
				Collections.sort(apiMethods);

				StringBuilder table = new StringBuilder("### " + clazz.getSimpleName() + "\n\n");
				table.append("<table>\n");
				table.append("  <tr><th>Method</th><th>URL</th><th>Consumes</th><th>Produces</th><th>Description</th></tr>\n");
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
		row.append(getCell(""));
		return row.append("</tr>\n").toString();
	}

	private String getCell(String content)
	{
		return "<td>" + content + "</td>";
	}

	private static class RestApiMethod implements Comparable<RestApiMethod>
	{
		String	httpMethod;
		String	url;
		String	consumes[];
		String	produces[];

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
	}
}
