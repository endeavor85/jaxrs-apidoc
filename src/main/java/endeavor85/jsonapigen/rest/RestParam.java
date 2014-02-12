package endeavor85.jsonapigen.rest;

import java.lang.annotation.Annotation;

import javax.ws.rs.FormParam;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;

import endeavor85.jsonapigen.SanitizedType;

public class RestParam
{
	ParamMeans		means	= ParamMeans.Form;	// TODO: assuming no annotation implies FormParam
	SanitizedType	type;
	String			value	= null;

	public RestParam(Class<?> paramType, Annotation[] annotations)
	{
		type = SanitizedType.fromClass(paramType);

		if(annotations != null)
		{
			if(annotations.length > 0)
			{
				for(Annotation annotation : annotations)
				{
					boolean restParamAnnotation = false;

					Class<?> annotationType = annotation.annotationType();

					if(annotationType == PathParam.class)
					{
						means = ParamMeans.Path;
						restParamAnnotation = true;
					}
					else if(annotationType == QueryParam.class)
					{
						means = ParamMeans.Query;
						restParamAnnotation = true;
					}
					else if(annotationType == FormParam.class)
					{
						means = ParamMeans.Form;
						restParamAnnotation = true;
					}

					if(restParamAnnotation)
					{
						// get the name value of the parameter
						try
						{
							value = (String) annotationType.getMethod("value").invoke(annotation);
						}
						catch(Exception e)
						{
							e.printStackTrace();
						}

						// found a valid rest parameter annotation, no need to continue searching
						break;
					}
				}
			}
		}
	}

	public ParamMeans getMeans()
	{
		return means;
	}

	public SanitizedType getType()
	{
		return type;
	}

	public String getValue()
	{
		return value;
	}

	public enum ParamMeans
	{
		Path, Query, Form
	}
}
