package endeavor85.jsonapigen.json;

import java.util.HashSet;
import java.util.Set;

public class JsonApiType
{
	boolean				abstractType;
	Class<?>			type;
	Set<JsonApiType>	subTypes	= new HashSet<>();

	public JsonApiType(Class<?> type)
	{
		this.type = type;
	}

	public Class<?> getType()
	{
		return type;
	}

	protected String getHyperlinkFor(Class<?> clazz)
	{
		return "<a href='#" + clazz.getSimpleName().toLowerCase() + "'>" + clazz.getSimpleName() + "</a>";
	}

	@Override
	public String toString()
	{
		String abstractWrapper = abstractType ? "_" : "";
		return "### " + abstractWrapper + getType().getSimpleName() + abstractWrapper + "\n_(" + getType().getName() + ")_\n\n";
	}

	public boolean isAbstractType()
	{
		return abstractType;
	}

	public void setAbstractType(boolean abstractType)
	{
		this.abstractType = abstractType;
	}

	public Set<JsonApiType> getSubTypes()
	{
		return subTypes;
	}

	public void setSubTypes(Set<JsonApiType> subTypes)
	{
		this.subTypes = subTypes;
	}

	public void setType(Class<?> type)
	{
		this.type = type;
	}
}