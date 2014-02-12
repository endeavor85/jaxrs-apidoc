package endeavor85.jsonapigen.rest;

import java.io.IOException;
import java.util.Map;
import java.util.TreeMap;

import com.google.common.reflect.ClassPath;

import endeavor85.jsonapigen.writer.ApiDocWriter;
import endeavor85.jsonapigen.writer.HtmlWriter;

public class RestApiGenerator
{
	public static void main(String[] args)
	{
		if(args.length > 0)
		{
			Map<String, RestResource> restResources = new TreeMap<>();

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

	public static Map<String, RestResource> parseRestResources(String rootPackageName)
	{
		Map<String, RestResource> restResources = new TreeMap<>();

		try
		{
			ClassPath classpath = ClassPath.from(RestApiGenerator.class.getClassLoader());
			for(ClassPath.ClassInfo classInfo : classpath.getTopLevelClassesRecursive(rootPackageName))
			{
				Class<?> clazz = classInfo.load();

				RestResource restResource = RestResource.parseClass(clazz);
				if(restResource != null)
					restResources.put(clazz.getName(), restResource);
			}
		}
		catch(IOException e)
		{
			e.printStackTrace();
		}

		return restResources;
	}
}
