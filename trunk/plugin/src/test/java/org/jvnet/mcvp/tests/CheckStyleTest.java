package org.jvnet.mcvp.tests;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URL;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Map.Entry;

import junit.framework.TestCase;

import org.apache.velocity.app.Velocity;
import org.w3c.css.css.DocumentParser;
import org.w3c.css.css.StyleReport;
import org.w3c.css.css.StyleReportFactory;
import org.w3c.css.css.StyleSheet;
import org.w3c.css.css.StyleSheetGenerator;
import org.w3c.css.parser.CssError;
import org.w3c.css.parser.CssErrorToken;
import org.w3c.css.parser.CssParseException;
import org.w3c.css.properties.PropertiesLoader;
import org.w3c.css.util.ApplContext;
import org.w3c.css.util.InvalidParamException;
import org.w3c.css.util.Utf8Properties;

public class CheckStyleTest extends TestCase {

	private static Utf8Properties availablePropertiesURL;

	static {

		availablePropertiesURL = new Utf8Properties();
		try {
			java.io.InputStream f;
			f = StyleSheetGenerator.class
					.getResourceAsStream("urls.properties");
			availablePropertiesURL.load(f);
			f.close();
		} catch (Exception e) {
			System.err.println("org.w3c.css.css.StyleSheetGeneratorHTML: "
					+ "couldn't load URLs properties ");
			System.err.println("  " + e.toString());
		}
	}

	public void testStyle0() throws Exception {

		initVelocity();

		ApplContext ac = new ApplContext("en");

		ac.setCssVersion("css21");

		ac.setMedium("all");

		final URL url = getClass().getResource("Style[0].css");

		DocumentParser parser = new DocumentParser(ac, url.toString());

		StyleSheet stylesheet = parser.getStyleSheet();

		stylesheet.findConflicts(ac);

		if (stylesheet.getErrors().getErrorCount() > 0) {
			for (CssError error : stylesheet.getErrors().getErrors()) {

				final Hashtable<String, Object> hashtable = produceError(error);

				for (Entry he : hashtable.entrySet()) {
					System.out.println(he.getKey() + ">>" + he.getValue());
				}
				System.out.println("-----------------------");
			}
		}

		StyleReport style = StyleReportFactory.getStyleReport(ac, "test",
				stylesheet, "text", 2);

		// if (!errorReport) {
		// style.desactivateError();
		// }

		style.print(new PrintWriter(System.out));

	}

	private void initVelocity() {
		try {
			Velocity.setProperty(Velocity.RESOURCE_LOADER, "file");
			Velocity.addProperty(Velocity.RESOURCE_LOADER, "jar");
			Velocity
					.setProperty("jar." + Velocity.RESOURCE_LOADER + ".class",
							"org.apache.velocity.runtime.resource.loader.JarResourceLoader");

			final URL url = StyleSheetGenerator.class
					.getResource("StyleSheetGenerator.class");
			final String path = url.toString();

			if (path.startsWith("jar:") && path.indexOf("!/") >= 0) {
				Velocity.setProperty("jar." + Velocity.RESOURCE_LOADER
						+ ".path", path.substring(0, path.indexOf("!/")));
			} else {
				Velocity.addProperty("file." + Velocity.RESOURCE_LOADER
						+ ".path", url.getFile());
			}
			Velocity.init();
		} catch (Exception e) {
			System.err.println("Failed to initialize Velocity. "
					+ "Validator might not work as expected.");
		}
	}

	private Hashtable<String, Object> produceError(CssError csserror) {
		Hashtable<String, Object> h = new Hashtable<String, Object>();
		try {
			Throwable ex = csserror.getException();
			h.put("Error", csserror);
			h.put("CtxName", "nocontext");
			h.put("CtxMsg", "");
			h.put("ErrorMsg",
					((ex.getMessage() == null) ? "" : ex.getMessage()));
			h.put("ClassName", "unkownerror");
			if (ex instanceof FileNotFoundException) {
				h.put("ClassName", "notfound");
				h.put("ErrorMsg", "File not found" + ": " + ex.getMessage());
			} else if (ex instanceof CssParseException) {
				produceParseException((CssParseException) ex, h);
			} else if (ex instanceof InvalidParamException) {
				h.put("ClassName", "invalidparam");
			} else if (ex instanceof IOException) {
				String stringError = ex.toString();
				// int index = stringError.indexOf(':');
				// The Exception name 'StringError' was previously
				// displayed
				// between </td> and <td class='nocontext' ...
				// It's now displayed inside the <td class='nocontext'>
				// tag
				// because we would need a new variable to put it there
				// for
				// just one rare case
				// TODO: why not using ex.toString()?
				h.put("CtxMsg", stringError);// .substring(0,
				// index));
				h.put("ClassName", "io");

			} else if (csserror instanceof CssErrorToken) {
				CssErrorToken terror = (CssErrorToken) csserror;
				h.put("ClassName", "errortoken");
				h.put("ErrorMsg", terror.getErrorDescription() + " : "
						+ terror.getSkippedString());
			} else {
				h.put("ClassName", "unkownerror");
				h.put("ErrorMsg", "Unknown Error");
				if (ex instanceof NullPointerException) {
					// ohoh, a bug
					ex.printStackTrace();
				}
			}
			return h;

		} catch (Exception e) {
			// context.put("request",
			// "An error occured during the output of your style sheet. Please correct your request or send a mail to plh@w3.org.");
			System.err.println(e.getMessage());
			e.printStackTrace();
			return h;
		}
	}

	private void produceParseException(CssParseException error,
			Hashtable<String, Object> ht_error) {
		if (error.getContexts() != null && error.getContexts().size() != 0) {
			ht_error.put("CtxName", "codeContext");
			StringBuilder buf = new StringBuilder();
			// Loop on the list of contexts for errors
			Enumeration e;
			for (e = error.getContexts().elements(); e.hasMoreElements();) {
				Object t = e.nextElement();
				// if the list is not null, add a comma
				if (t != null) {
					buf.append(t);
					if (e.hasMoreElements()) {
						buf.append(", ");
					}
				}
			}
			if (buf.length() != 0) {
				ht_error.put("CtxMsg", String.valueOf(buf));
			}
		} else {
			ht_error.put("CtxName", "nocontext");
		}
		ht_error.put("ClassName", "parse-error");
		String name = error.getProperty();
		String ret;
		if ((name != null) && (getURLProperty(name) != null)
				&& PropertiesLoader.getProfile("css21").containsKey(name)) {
			// we add a link information
			// we check if the property doesn't exist in this css version
			ht_error.put("link_before_parse_error", "Value Error");
			// Since CSS3 is only a working draft, the links don't exist yet
			// in CSS3...
			// And this is the same with CSS1 because the links are not working
			// the same way...
			// This can be removed as soon as the CSS3 specifications are made
			// and CSS1 use the links
			// and the link is changed in urls.properties
			String lnk;
			if ("css21".equals("css3")) {
				lnk = getURLProperty("@url-base_css2.1");
			} else if ("css21".equals("css1")) {
				lnk = getURLProperty("@url-base_css2");
			} else {
				lnk = getURLProperty("@url-base_" + "css21");
			}
			// this would be replaced by :
			// ht_error.put("link_value_parse_error",
			// context.get("css_link") + getURLProperty(name));
			ht_error.put("link_value_parse_error", lnk + getURLProperty(name));
			ht_error.put("link_name_parse_error", name);
		}
		if ((error.getException() != null) && (error.getMessage() != null)) {
			if (error.isParseException()) {
				ret = queryReplace(error.getMessage());
			} else {
				Exception ex = error.getException();
				if (ex instanceof NumberFormatException) {
					ret = "Invalid number";
				} else {
					ret = queryReplace(ex.getMessage());
				}
			}
			if (error.getSkippedString() != null) {
				ht_error.put("span_class_parse_error", "skippedString");
				ht_error.put("span_value_parse_error", queryReplace(error
						.getSkippedString()));
			} else if (error.getExp() != null) {
				ret += " : " + queryReplace(error.getExp().toStringFromStart());
				ht_error.put("span_class_parse_error", "exp");
				ht_error.put("span_value_parse_error", queryReplace(error
						.getExp().toString()));
			}
		} else {
			ret = "Parse Error";
			ht_error.put("span_class_parse_error", "unrecognized");
			ht_error.put("span_value_parse_error", queryReplace(error
					.getSkippedString()));
		}
		ht_error.put("ErrorMsg", ret);
	}

	/**
	 * 
	 * @param s
	 *            , the string to convert
	 * @return the string s with html character escaped
	 */
	private String queryReplace(String s) {
		if (s != null) {
			int len = s.length();
			StringBuilder ret = new StringBuilder(len + 16);
			char c;

			for (int i = 0; i < len; i++) {
				switch (c = s.charAt(i)) {
				case '&':
					ret.append("&amp;");
					break;
				case '\'':
					ret.append("&#39;");
					break;
				case '"':
					ret.append("&quot;");
					break;
				case '<':
					ret.append("&lt;");
					break;
				case '>':
					ret.append("&gt;");
					break;
				default:
					ret.append(c);
				}
			}
			return ret.toString();
		}
		return "[empty string]";
	}

	private final static String getURLProperty(String name) {
		return availablePropertiesURL.getProperty(name);
	}

}
