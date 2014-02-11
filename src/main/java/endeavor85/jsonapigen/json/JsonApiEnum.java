package endeavor85.jsonapigen.json;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

public class JsonApiEnum extends JsonApiType
{
	List<String>	values;

	public JsonApiEnum(Class<?> enumType)
	{
		super(enumType);
		abstractType = false;

		values = new ArrayList<>();

		for(Object o : enumType.getEnumConstants())
			values.add(o.toString());
	}

	@Override
	public String toString()
	{
		StringBuilder result = new StringBuilder(super.toString());
		result.append("<tt>" + StringUtils.join(values, "</tt> | <tt>") + "</tt>");
		result.append("\n\n");
		return result.toString();
	}
}