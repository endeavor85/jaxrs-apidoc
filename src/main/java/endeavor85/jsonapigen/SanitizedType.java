package endeavor85.jsonapigen;

public class SanitizedType
{
	Class<?>	type;
	String		typeStr;
	boolean		isArray	= false;

	@Override
	public String toString()
	{
		if(type != null)
			return type.getSimpleName() + (isArray ? "[]" : "");
		else
			return typeStr + (isArray ? "[]" : "");
	}

	public Class<?> getType()
	{
		return type;
	}

	public void setType(Class<?> type)
	{
		this.type = type;
	}

	public boolean isArray()
	{
		return isArray;
	}

	public void setArray(boolean isArray)
	{
		this.isArray = isArray;
	}

	public String getTypeStr()
	{
		return typeStr;
	}

	public void setTypeStr(String typeStr)
	{
		this.typeStr = typeStr;
	}
}
