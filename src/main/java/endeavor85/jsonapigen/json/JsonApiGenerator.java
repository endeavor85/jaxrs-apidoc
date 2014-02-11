package endeavor85.jsonapigen.json;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.google.common.reflect.ClassPath;

public class JsonApiGenerator
{
	public static void main(String[] args)
	{
		if(args.length > 0)
			new JsonApiGenerator(args);
		else
			System.err.println("Add package name arguments (space-separated). E.g., java JsonApiGenerator com.example.package com.example.xyz");
	}

	Map<String, JsonApiType>	jsonType	= new TreeMap<>();
	Set<Class<?>>				parsedTypes	= new HashSet<>();

	public JsonApiGenerator(String[] rootPackages)
	{
		List<Class<?>> types = new ArrayList<>();

		// inspect packages for types
		for(String rootPackageName : rootPackages)
		{
			try
			{
				ClassPath classpath = ClassPath.from(JsonApiGenerator.class.getClassLoader());
				for(ClassPath.ClassInfo classInfo : classpath.getTopLevelClassesRecursive(rootPackageName))
				{
					// inspect top level class for inner classes (recursively)
					Class<?> topLevelClass = classInfo.load();
					types.addAll(inspectTypesRecursive(topLevelClass));
				}
			}
			catch(IOException e)
			{
				e.printStackTrace();
			}
		}

		// parse included classes
		for(Class<?> type : types)
		{
			if(type.isEnum())
			{
				jsonType.put(type.getSimpleName(), new JsonApiEnum(type));
				parsedTypes.add(type);
			}
			else
			{
				JsonApiClass classType = new JsonApiClass(type);

				// only include types that have json properties
				if(!classType.getProperties().isEmpty())
				{
					jsonType.put(type.getSimpleName(), classType);
					parsedTypes.add(type);
				}
			}
		}

		// check inheritance
		for(JsonApiType type : jsonType.values())
		{
			if(type instanceof JsonApiClass)
			{
				JsonApiClass apiClass = (JsonApiClass) type;
				Class<?> superclass = apiClass.getType().getSuperclass();
				JsonApiType parentType = jsonType.get(superclass.getSimpleName());
				if(parentType != null && parentType instanceof JsonApiClass)
				{
					JsonApiClass parentApiType = (JsonApiClass) parentType;
					parentApiType.getSubTypes().add(apiClass);
					// copy json type property from parent (will need to display this along with this type's specific name value)
					apiClass.setJsonTypeProperty(parentApiType.getJsonTypeProperty());

					// look for json type name
					JsonTypeName jsonTypeNameAnnot = apiClass.getType().getAnnotation(JsonTypeName.class);
					if(jsonTypeNameAnnot != null)
					{
						apiClass.setJsonTypeName(jsonTypeNameAnnot.value());
					}
				}
			}
		}

		// write markdown
		for(JsonApiType type : jsonType.values())
		{
			if(type instanceof JsonApiClass)
				System.out.print(((JsonApiClass) type).toString(parsedTypes));
			else
				System.out.print(type.toString());
		}
	}

	public static List<Class<?>> inspectTypesRecursive(Class<?> rootType)
	{
		List<Class<?>> types = new ArrayList<>();

		// include root type
		types.add(rootType);

		// recursively inspect inner classes
		for(Class<?> innerClass : rootType.getDeclaredClasses())
			types.addAll(inspectTypesRecursive(innerClass));

		return types;
	}
}
