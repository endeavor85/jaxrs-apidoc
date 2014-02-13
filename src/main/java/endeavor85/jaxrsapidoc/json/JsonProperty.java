package endeavor85.jaxrsapidoc.json;

import java.util.ArrayList;
import java.util.List;

import endeavor85.jaxrsapidoc.SanitizedType;

public class JsonProperty
{
	String			name;
	SanitizedType	type;
	boolean			unwrapped	= false;
	List<Class<?>>	views		= new ArrayList<>();

	public String getName()
	{
		return name;
	}

	public void setName(String name)
	{
		this.name = name;
	}

	public SanitizedType getType()
	{
		return type;
	}

	public void setType(SanitizedType type)
	{
		this.type = type;
	}

	public boolean isUnwrapped()
	{
		return unwrapped;
	}

	public void setUnwrapped(boolean unwrapped)
	{
		this.unwrapped = unwrapped;
	}

	public List<Class<?>> getViews()
	{
		return views;
	}
}
