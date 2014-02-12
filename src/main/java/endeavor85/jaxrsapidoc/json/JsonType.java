package endeavor85.jaxrsapidoc.json;

import java.util.HashSet;
import java.util.Set;

public class JsonType
{
	boolean			abstractType;
	Class<?>		type;
	Set<JsonType>	subTypes	= new HashSet<>();

	public JsonType(Class<?> type)
	{
		this.type = type;
	}

	public Class<?> getType()
	{
		return type;
	}

	public boolean isAbstractType()
	{
		return abstractType;
	}

	public void setAbstractType(boolean abstractType)
	{
		this.abstractType = abstractType;
	}

	public Set<JsonType> getSubTypes()
	{
		return subTypes;
	}

	public void setSubTypes(Set<JsonType> subTypes)
	{
		this.subTypes = subTypes;
	}

	public void setType(Class<?> type)
	{
		this.type = type;
	}
}
