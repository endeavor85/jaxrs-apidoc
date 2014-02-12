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

		// store REST resources and types
		Map<String, RestResource> restResources = new TreeMap<>();
		Map<String, JsonType> jsonTypes = new TreeMap<>();
		Set<Class<?>> parsedTypes = new HashSet<>(); // remember which classes have already been parsed (since some will be parsed but not added to jsonTypes)
		// store all types referenced by the REST resources (these will be parsed and added to jsonTypes)
		Set<Class<?>> referencedTypes = new HashSet<>();

		// parse resources in each package argument
		for(String arg : args)
			restResources.putAll(RestApiGenerator.parseRestResources(arg));

		// gather the types referenced by REST resources
		for(RestResource restResource : restResources.values())
		{
			for(Class<?> clazz : restResource.getReferencedTypes())
				addReferencedType(clazz, referencedTypes);
		}

		// inspect referenced types until all have been parsed
		while(!referencedTypes.isEmpty())
		{
			// gather types referenced by other types (types found while parsing other types)
			Set<Class<?>> newlyReferencedTypes = new HashSet<>();

			// parse referenced types for JSON properties
			for(Class<?> clazz : referencedTypes)
			{
				// if clazz hasn't already been parsed
				if(!parsedTypes.contains(clazz))
				{
					// parse the class for json properties
					JsonType jsonType = JsonApiGenerator.parseType(clazz);

					// add parsed type to parsedTypes set
					parsedTypes.add(clazz);

					// if json type was found
					if(jsonType != null)
					{
						// add parsed type to jsonTypes map
						jsonTypes.put(clazz.getName(), jsonType);

						// add any new references to newlyReferencedTypes set
						newlyReferencedTypes.addAll(jsonType.getReferencedTypes());
					}
				}
			}

			// we've parsed all previous referenced types, clear the set
			referencedTypes.clear();

			// add newlyReferencedTypes to referenced types set
			for(Class<?> clazz : newlyReferencedTypes)
			{
				// if clazz hasn't already been parsed
				if(!parsedTypes.contains(clazz))
				{
					addReferencedType(clazz, referencedTypes);
				}
			}
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

	private static void addReferencedType(Class<?> clazz, Set<Class<?>> referencedTypes)
	{
		referencedTypes.addAll(JsonApiGenerator.inspectTypesRecursive(clazz));
	}
}
