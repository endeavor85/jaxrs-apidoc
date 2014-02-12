package endeavor85.jaxrsapidoc.json;

import java.util.HashSet;
import java.util.Set;

public abstract class JsonType
{
	protected boolean		abstractType;
	private Class<?>		type;
	private Set<JsonType>	innerTypes	= new HashSet<>();

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

	public Set<JsonType> getInnerTypes()
	{
		return innerTypes;
	}

	public abstract Set<Class<?>> getReferencedTypes();
}
