package endeavor85.jsonapigen.rest;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.google.common.reflect.ClassPath;

public class RestApiGenerator
{
	public static void main(String[] args)
	{
		if(args.length > 0)
		{
			List<RestApiResource> restResources = new ArrayList<>();

			// parse resources in each package argument
			for(String arg : args)
				restResources.addAll(RestApiGenerator.parseRestResources(arg));

			// print REST resource API
			for(RestApiResource restResource : restResources)
				System.out.println(restResource.toString());
		}
		else
			System.err.println("Add package name arguments (space-separated). E.g., java RestApiGenerator com.example.package com.example.xyz");
	}

	public static List<RestApiResource> parseRestResources(String rootPackageName)
	{
		List<RestApiResource> restResources = new ArrayList<>();

		try
		{
			ClassPath classpath = ClassPath.from(RestApiGenerator.class.getClassLoader());
			for(ClassPath.ClassInfo classInfo : classpath.getTopLevelClassesRecursive(rootPackageName))
			{
				Class<?> clazz = classInfo.load();

				RestApiResource restResource = RestApiResource.parseClass(clazz);
				if(restResource != null)
					restResources.add(restResource);
			}
		}
		catch(IOException e)
		{
			e.printStackTrace();
		}

		return restResources;
	}
}
