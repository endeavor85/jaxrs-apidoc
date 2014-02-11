package endeavor85.jsonapigen;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import endeavor85.jsonapigen.json.JsonApiClass;
import endeavor85.jsonapigen.json.JsonApiGenerator;
import endeavor85.jsonapigen.json.JsonApiType;
import endeavor85.jsonapigen.rest.RestApiGenerator;
import endeavor85.jsonapigen.rest.RestApiResource;

public class ApiGenerator
{
	public static void main(String[] args)
	{
		if(args.length > 0)
		{
			// parse resources in each package argument
			List<RestApiResource> restResources = new ArrayList<>();

			for(String arg : args)
				restResources.addAll(RestApiGenerator.parseRestResources(arg));

			// get types referenced by REST resources
			Set<Class<?>> referencedTypes = new HashSet<>();
			for(RestApiResource restResource : restResources)
				referencedTypes.addAll(restResource.getReferencedTypes());

			// parse referenced types for JSON properties
			Map<Class<?>, JsonApiType> jsonTypesMap = new TreeMap<>();
			for(Class<?> clazz : referencedTypes)
			{
				// TODO: some type may reference other types that will need to be parsed, need to parse them also

				// List<>
				// JsonApiGenerator.
				// jsonTypesMap.put(clazz, )
			}

			// print REST resource API
			for(RestApiResource restResource : restResources)
				System.out.println(restResource.toString());

			// print JSON types
			for(JsonApiType jsonApiType : jsonTypesMap.values())
			{
				if(jsonApiType instanceof JsonApiClass)
					System.out.print(((JsonApiClass) jsonApiType).toString(jsonTypesMap.keySet()));
				else
					System.out.print(jsonApiType.toString());
			}
		}
		else
			System.err.println("Add package name arguments (space-separated). E.g., java ApiGenerator com.example.package com.example.xyz");
	}
}
