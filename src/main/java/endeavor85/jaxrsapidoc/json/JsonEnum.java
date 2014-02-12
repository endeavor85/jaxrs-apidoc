package endeavor85.jaxrsapidoc.json;

import java.util.ArrayList;
import java.util.List;

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
}
