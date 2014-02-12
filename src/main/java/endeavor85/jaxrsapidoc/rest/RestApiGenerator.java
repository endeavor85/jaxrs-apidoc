package endeavor85.jaxrsapidoc.rest;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import com.google.common.reflect.ClassPath;

import endeavor85.jaxrsapidoc.writer.ApiDocWriter;
import endeavor85.jaxrsapidoc.writer.HtmlWriter;

public class RestApiGenerator
{
	public static void main(String[] args)
	{
		if(args.length > 0)
		{
			Map<Class<?>, RestResource> restResources = new HashMap<>();

			// parse resources in each package argument
			for(String arg : args)
				restResources.putAll(RestApiGenerator.parseRestResources(arg));

			// print REST resource API
			ApiDocWriter adw = new HtmlWriter(System.out);
			adw.getResources().putAll(restResources);
			try
			{
				adw.writeResourceApi();
			}
			catch(IOException e)
			{
				e.printStackTrace();
			}
		}
		else
			System.err.println("Add package name arguments (space-separated). E.g., java RestApiGenerator com.example.package com.example.xyz");
	}

	public static Map<Class<?>, RestResource> parseRestResources(String rootPackageName)
	{
		Map<Class<?>, RestResource> restResources = new HashMap<>();

		try
		{
			ClassPath classpath = ClassPath.from(RestApiGenerator.class.getClassLoader());
			for(ClassPath.ClassInfo classInfo : classpath.getTopLevelClassesRecursive(rootPackageName))
			{
				Class<?> clazz = classInfo.load();

				RestResource restResource = RestResource.parseClass(clazz);
				if(restResource != null)
					restResources.put(clazz, restResource);
			}
		}
		catch(IOException e)
		{
			e.printStackTrace();
		}

		return restResources;
	}
}
