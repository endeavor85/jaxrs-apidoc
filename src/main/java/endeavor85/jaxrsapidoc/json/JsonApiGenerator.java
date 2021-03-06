package endeavor85.jaxrsapidoc.json;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.google.common.reflect.ClassPath;

import endeavor85.jaxrsapidoc.writer.ApiDocWriter;
import endeavor85.jaxrsapidoc.writer.HtmlWriter;

public class JsonApiGenerator
{
	public static void main(String[] args)
	{
		if(args.length > 0)
		{
			Set<Class<?>> types = new HashSet<>();

			// parse resources in each package argument
			for(String arg : args)
				types.addAll(JsonApiGenerator.searchPackageForTypes(arg));

			Map<Class<?>, JsonType> jsonTypes = parseTypes(types);

			buildInheritanceRelations(jsonTypes);

			// print JSON type API
			ApiDocWriter adw = new HtmlWriter(System.out);
			adw.getTypes().putAll(jsonTypes);
			try
			{
				adw.writeTypeApi();
			}
			catch(IOException e)
			{
				e.printStackTrace();
			}
		}
		else
			System.err.println("Add package name arguments (space-separated). E.g., java JsonApiGenerator com.example.package com.example.xyz");
	}

	public static JsonType parseType(Class<?> clazz)
	{
		JsonType jsonType = null;

		if(clazz.isEnum())
		{
			jsonType = new JsonEnum(clazz);
		}
		else
		{
			JsonClass jsonClass = new JsonClass(clazz);

			// only include types that have json properties
			if(!jsonClass.getProperties().isEmpty() || jsonClass.getJsonTypeProperty() != null)
				jsonType = jsonClass;
		}

		return jsonType;
	}

	public static Set<Class<?>> searchPackageForTypes(String rootPackage)
	{
		Set<Class<?>> types = new HashSet<>();

		// inspect package for types
		try
		{
			ClassPath classpath = ClassPath.from(JsonApiGenerator.class.getClassLoader());
			for(ClassPath.ClassInfo classInfo : classpath.getTopLevelClassesRecursive(rootPackage))
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

		return types;
	}

	public static Map<Class<?>, JsonType> parseTypes(Set<Class<?>> types)
	{
		Map<Class<?>, JsonType> jsonTypes = new HashMap<>();

		// parse included classes
		for(Class<?> type : types)
		{
			JsonType jsonType = parseType(type);
			if(jsonType != null)
				jsonTypes.put(type, jsonType);
		}

		return jsonTypes;
	}

	public static void buildInheritanceRelations(Map<Class<?>, JsonType> jsonTypes)
	{
		for(JsonType type : jsonTypes.values())
		{
			if(type instanceof JsonClass)
			{
				JsonClass apiClass = (JsonClass) type;
				Class<?> superclass = apiClass.getType().getSuperclass();
				JsonType parentType = jsonTypes.get(superclass);
				if(parentType != null && parentType instanceof JsonClass)
				{
					JsonClass parentApiType = (JsonClass) parentType;
					parentApiType.getInnerTypes().add(apiClass);
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
