package endeavor85.jsonapigen.writer;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import endeavor85.jsonapigen.json.JsonClass;
import endeavor85.jsonapigen.json.JsonProperty;
import endeavor85.jsonapigen.json.JsonType;
import endeavor85.jsonapigen.rest.RestMethod;
import endeavor85.jsonapigen.rest.RestParam;
import endeavor85.jsonapigen.rest.RestResource;

public abstract class ApiDocWriter
{
	Map<String, RestResource>		resources	= new TreeMap<>();
	Map<String, JsonType>			types		= new TreeMap<>();

	protected OutputStreamWriter	writer;

	public ApiDocWriter(OutputStream os)
	{
		writer = new OutputStreamWriter(os);
	}

	public Map<String, RestResource> getResources()
	{
		return resources;
	}

	public Map<String, JsonType> getTypes()
	{
		return types;
	}

	public void writeFullApi() throws IOException
	{
		writeResourceApi();
		writeTypeApi();
	}

	public void writeResourceApi() throws IOException
	{
		writeResourceApiHeader();

		if(resources == null)
			return;

		for(RestResource resource : resources.values())
			writeResource(resource);

		writer.flush();
	};

	public void writeTypeApi() throws IOException
	{
		writeTypeApiHeader();

		if(types == null)
			return;

		for(JsonType type : types.values())
			writeType(type);

		writer.flush();
	};

	protected abstract void writeResourceApiHeader() throws IOException;

	protected abstract void writeResource(RestResource resource) throws IOException;

	protected abstract void writeResourceMethod(RestMethod method) throws IOException;

	protected abstract void writeResourceMethodParameters(List<RestParam> params) throws IOException;

	protected abstract void writeTypeApiHeader() throws IOException;

	protected abstract void writeType(JsonType type) throws IOException;

	protected abstract void writeTypeProperty(JsonClass apiClass, JsonProperty property) throws IOException;
}
