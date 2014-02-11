package endeavor85.jsonapigen.json;

import java.util.ArrayList;
import java.util.List;

import endeavor85.jsonapigen.SanitizedType;

public class JsonApiProperty
{
	String			name;
	SanitizedType	type;
	List<Class<?>>	views	= new ArrayList<>();

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

	public List<Class<?>> getViews()
	{
		return views;
	}
}