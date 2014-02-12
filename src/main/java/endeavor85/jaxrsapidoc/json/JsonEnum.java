package endeavor85.jaxrsapidoc.json;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class JsonEnum extends JsonType
{
	List<String>	values;

	public JsonEnum(Class<?> enumType)
	{
		super(enumType);
		abstractType = false;

		values = new ArrayList<>();

		for(Object o : enumType.getEnumConstants())
			values.add(o.toString());
	}

	public List<String> getValues()
	{
		return values;
	}

	public void setValues(List<String> values)
	{
		this.values = values;
	}

	@Override
	public Set<Class<?>> getReferencedTypes()
	{
		return new HashSet<>();
	}
}
