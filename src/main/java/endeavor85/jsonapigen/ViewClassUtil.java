package endeavor85.jsonapigen;

public class ViewClassUtil
{
	public static String getSanitizedViewClassName(Class<?> viewClass)
	{
		String view = viewClass.getName();
		int lastPeriod = view.lastIndexOf((int) '.');
		if(lastPeriod >= 0 && lastPeriod < view.length() - 1)
			view = view.substring(lastPeriod + 1);
		return view.replace('$', '.');
	}
}
