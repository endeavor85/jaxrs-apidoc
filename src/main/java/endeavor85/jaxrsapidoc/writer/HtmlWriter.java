package endeavor85.jaxrsapidoc.writer;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import endeavor85.jaxrsapidoc.SanitizedType;
import endeavor85.jaxrsapidoc.ViewClassUtil;
import endeavor85.jaxrsapidoc.json.JsonClass;
import endeavor85.jaxrsapidoc.json.JsonEnum;
import endeavor85.jaxrsapidoc.json.JsonProperty;
import endeavor85.jaxrsapidoc.json.JsonType;
import endeavor85.jaxrsapidoc.rest.RestMethod;
import endeavor85.jaxrsapidoc.rest.RestParam;
import endeavor85.jaxrsapidoc.rest.RestResource;

public class HtmlWriter extends ApiDocWriter
{
	public HtmlWriter(OutputStream os)
	{
		super(os);
	}

	@Override
	protected void writeResourceApiHeader() throws IOException
	{
		writer.write("<h2 id='restresources'>REST Resources</h2>\n\n");
	}

	@Override
	protected void writeResource(RestResource resource) throws IOException
	{
		writer.write("<h3 id='" + escapeAnchorId(resource.getResourceClass().getName()) + "'>" + resource.getResourceClass().getSimpleName() + "</h3>\n\n");
		writer.write("<table>\n");
		writer.write("  <tr><th>Method</th><th>URL</th><th>Consumes</th><th>Produces</th></tr>\n");
		for(RestMethod apiMethod : resource.getApiMethods())
			writeResourceMethod(apiMethod);
		writer.write("</table>\n\n");
	}

	@Override
	protected void writeResourceMethod(RestMethod method) throws IOException
	{
		writer.write("  <tr>");
		writer.write("<th><tt>" + method.getHttpMethod() + "</tt></th>");
		writer.write("<th><tt>" + method.getUrl() + "</tt></th>");
		writer.write("<td>" + (method.getConsumes() == null ? "" : ("<tt>" + StringUtils.join(method.getConsumes(), "</tt><br/><tt>") + "</tt>")) + "</td>");
		writer.write("<td>" + (method.getProduces() == null ? "" : ("<tt>" + StringUtils.join(method.getProduces(), "</tt><br/><tt>") + "</tt>")) + "</td>");
		writer.write("</tr>\n  <tr>\n    <td colspan=\"4\">\n");
		
		// store whether any params are written so we can draw a line between parameters and return type if both are present
		boolean wroteParams = false;
		
		if(!method.getPathParams().isEmpty())
		{
			writer.write("      Path Parameters:\n");
			writeResourceMethodParameters(method.getPathParams());
			wroteParams = true;
		}
		if(!method.getQueryParams().isEmpty())
		{
			writer.write("      Query Parameters:\n");
			writeResourceMethodParameters(method.getQueryParams());
			wroteParams = true;
		}
		if(!method.getFormParams().isEmpty())
		{
			writer.write("      Form Parameters:\n");
			writeResourceMethodParameters(method.getFormParams());
			wroteParams = true;
		}
		if(method.getReturnType() != null)
		{
			// draw horizontal line between params and return
			if(wroteParams)
				writer.write("      <hr/>\n");

			writer.write("      Returns:\n      <ul>\n");
			if(method.getReturnType() != null)
			{
				writer.write("        <li><tt>" + escapeHTML(method.getReturnType().toString()) + "</tt>");
				if(method.getJsonView() != null)
					writer.write(" (view: <tt>" + ViewClassUtil.getSanitizedViewClassName(method.getJsonView()) + "</tt>)");
				writer.write("</li>\n");
			}
			writer.write("      </ul>\n");
		}
		writer.write("    </td>\n  </tr>\n");
	}

	@Override
	protected void writeResourceMethodParameters(List<RestParam> params) throws IOException
	{
		writer.write("      <ul>\n");
		for(RestParam param : params)
			writer.write("        <li>" + (param.getValue() == null ? "" : "<tt>" + param.getValue() + "</tt> : ") + "<tt>" + escapeHTML(param.getType().toString()) + "</tt></li>\n");
		writer.write("      </ul>\n");
	}

	@Override
	protected void writeTypeApiHeader() throws IOException
	{
		writer.write("<h2 id='jsontypes'>JSON Types</h2>\n\n");
	}

	@Override
	protected void writeType(JsonType type) throws IOException
	{
		writer.write("<h3 id='" + escapeAnchorId(type.getType().getName()) + "'> " + (type.isAbstractType() ? emphasis(type.getType().getSimpleName()) : type.getType().getSimpleName()) + "</h3>\n");
		writer.write(emphasis("(" + type.getType().getName() + ")") + "\n\n");

		if(type instanceof JsonClass)
		{
			JsonClass apiClass = (JsonClass) type;

			if(!apiClass.getProperties().isEmpty())
			{
				List<String> viewNames = new ArrayList<String>(apiClass.getViewClasses().size());
				for(Class<?> viewClass : apiClass.getViewClasses())
					viewNames.add(ViewClassUtil.getSanitizedViewClassName(viewClass));

				writer.write("<table>\n");
				writer.write("  <tr><th rowspan='2'>Property name</th><th rowspan='2'>Type</th>" + (viewNames.isEmpty() ? "" : ("<th colspan='" + viewNames.size() + "'>Views</th>")) + "</tr>\n");
				writer.write("  <tr>");
				for(String viewName : viewNames)
					writer.write("<th>" + viewName + "</th>");
				writer.write("</tr>\n");

				// add type property if necessary
				if(apiClass.getJsonTypeProperty() != null && !apiClass.getJsonTypeProperty().isEmpty())
				{
					writer.write("  <tr><td><tt>" + apiClass.getJsonTypeProperty() + "</tt></td>");

					// if this is the parent type
					if(apiClass.isAbstractType())
						writer.write("<td><tt>String</tt> " + emphasis("(implementor's type)") + "</td>");
					else
						writer.write("<td><tt>String</tt> = <tt>\"" + apiClass.getJsonTypeName() + "\"</tt></td>");

					for(int i = 0; i < apiClass.getViewClasses().size(); i++)
						writer.write("<td>&#x2713;</td>");

					writer.write("</tr>\n");
				}

				for(JsonProperty property : apiClass.getProperties().values())
					writeTypeProperty(apiClass, property);

				writer.write("</table>\n\n");
			}

			// for abstract types, check for implementors and list them
			if(apiClass.isAbstractType() && !apiClass.getSubTypes().isEmpty())
			{
				writer.write("<h5>Implementors</h5>\n\n");
				writer.write("<ul>\n");
				for(JsonType subType : apiClass.getSubTypes())
					writer.write("  <li>" + getHyperlinkFor(subType.getType()) + "</li>\n");
				writer.write("</ul>\n");
			}
		}
		else
		{
			writer.write("<tt>" + StringUtils.join(((JsonEnum) type).getValues(), "</tt> | <tt>") + "</tt>\n\n");
		}
	}

	@Override
	protected void writeTypeProperty(JsonClass apiClass, JsonProperty property) throws IOException
	{
		writer.write("  <tr><td><tt>" + property.getName() + "</tt></td>");
		SanitizedType returnType = property.getType();
		if(returnType != null && getTypes().containsKey(returnType.getBaseType().getName()))
			writer.write("<td><tt>" + getHyperlinkFor(returnType.getBaseType()) + (returnType.isArray() ? "[]" : "") + "</tt></td>");
		else
			writer.write("<td><tt>" + escapeHTML(returnType.toString()) + "</tt></td>");
		for(Class<?> viewClass : apiClass.getViewClasses())
			writer.write("<td>" + (property.getViews().contains(viewClass) ? "&#x2713;" : "&nbsp;") + "</td>");
		writer.write("</tr>\n");
	}

	protected String getHyperlinkFor(Class<?> clazz)
	{
		return "<a href='#" + clazz.getName() + "'>" + escapeHTML(clazz.getSimpleName()) + "</a>";
	}

	protected String escapeHTML(String str)
	{
		return str.replaceAll("<", "&lt;").replaceAll(">", "&gt;");
	}

	protected String escapeAnchorId(String str)
	{
		// $ is not a valid id character, replace it with -
		return str.replaceAll("\\$", "-");
	}

	protected String emphasis(String str)
	{
		return "<em>" + str + "</em>";
	}
}
