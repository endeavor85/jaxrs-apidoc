package endeavor85.jaxrsapidoc;

import java.io.IOException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import endeavor85.jaxrsapidoc.json.JsonApiGenerator;
import endeavor85.jaxrsapidoc.json.JsonType;
import endeavor85.jaxrsapidoc.rest.RestApiGenerator;
import endeavor85.jaxrsapidoc.rest.RestResource;
import endeavor85.jaxrsapidoc.writer.ApiDocWriter;
import endeavor85.jaxrsapidoc.writer.HtmlWriter;

public class ApiGenerator
{
	public static void main(String[] args)
	{
		if(args.length < 1)
		{
			System.err.println("Add package name arguments (space-separated). E.g., java ApiGenerator com.example.package com.example.xyz");
			return;
		}

		// parse resources in each package argument
		Map<String, RestResource> restResources = new TreeMap<>();

		for(String arg : args)
			restResources.putAll(RestApiGenerator.parseRestResources(arg));

		// get types referenced by REST resources
		Set<Class<?>> referencedTypes = new HashSet<>();
		for(RestResource restResource : restResources.values())
			referencedTypes.addAll(restResource.getReferencedTypes());

		// add subtypes of referenced types
		for(Class<?> clazz : referencedTypes)
			referencedTypes.addAll(JsonApiGenerator.inspectTypesRecursive(clazz));

		// TODO: some types may reference other types that will need to be parsed, need to remember to parse them also

		// parse referenced types for JSON properties
		Map<String, JsonType> jsonTypes = new TreeMap<>();

		for(Class<?> clazz : referencedTypes)
		{
			JsonType jsonType = JsonApiGenerator.parseType(clazz);
			if(jsonType != null)
				jsonTypes.put(clazz.getName(), jsonType);
		}

		JsonApiGenerator.buildInheritanceRelations(jsonTypes);

		// print REST/JSON API
		ApiDocWriter adw = new HtmlWriter(System.out);
		adw.getResources().putAll(restResources);
		adw.getTypes().putAll(jsonTypes);
		try
		{
			adw.writeFullApi();
		}
		catch(IOException e)
		{
			e.printStackTrace();
		}
	}
}
