package org.jvnet.mcvp;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Enumeration;
import java.util.Hashtable;

import org.apache.maven.plugin.logging.Log;
import org.w3c.css.css.StyleSheet;
import org.w3c.css.css.StyleSheetGenerator;
import org.w3c.css.parser.CssError;
import org.w3c.css.parser.CssErrorToken;
import org.w3c.css.parser.CssParseException;
import org.w3c.css.properties.PropertiesLoader;
import org.w3c.css.util.InvalidParamException;
import org.w3c.css.util.Utf8Properties;
import org.w3c.css.util.Warning;

class LogStyleSheetReport implements StyleSheetReport {

	private static Utf8Properties<String, String> availablePropertiesURL;

	static {

		availablePropertiesURL = new Utf8Properties<String, String>();
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

	private final Log log;

	public LogStyleSheetReport(Log log) {
		super();
		this.log = log;
	}

	public Log getLog() {
		return log;
	}

	public void report(StyleSheet stylesheet) {

		// TODO
		// System.out.println("W3C CSS Validator results for " +
		// url.toString());

		if (stylesheet.getErrors().getErrorCount() == 0) {
			// getLog().info("No CSS validation failures.");

			// System.out.println("This document validates as  "
			// + ac.getMsg().getString(ac.getCssVersion()) + "!");

		} else {
			getLog().error(
					"Found [" + stylesheet.getErrors().getErrorCount()
							+ "] errors.");
			String sf = "";
			for (CssError error : stylesheet.getErrors().getErrors()) {

				final Hashtable<String, Object> hashtable = produceError(error);

				String error_msg = (String) hashtable.get("ErrorMsg");
				String context_msg = (String) hashtable.get("CtxMsg");

				String before_link = null;
				String link_value = null;
				if (hashtable.containsKey("link_value_parse_error")) {
					before_link = (String) hashtable
							.get("link_before_parse_error");
					link_value = (String) hashtable
							.get("link_value_parse_error");
				} else {
					link_value = null;
				}
				String span_value = null;
				if (link_value == null
						&& hashtable.containsKey("span_value_parse_error"))
					span_value = (String) hashtable
							.get("span_value_parse_error");
				else {
					span_value = null;
				}
				if (!sf.equals(error.getSourceFile())) {
					sf = error.getSourceFile();
					getLog().error("Location: " + sf);
				}

				getLog().error("Line: " + error.getLine() + context_msg);

				if (link_value != null) {
					final String link_name = (String) hashtable
							.get("link_name_parse_error");
					getLog().error(
							"      " + before_link + ": " + link_name + " ("
									+ link_value + ")");
				}

				getLog().error("      " + error_msg);

				if (span_value != null) {
					final String span_class = (String) hashtable
							.get("span_class_parse_error");
					;
					// getLog().todo("      " + span_class);
					getLog().error("      " + span_value);
				}

			}
		}
		int warningLevel = 2;

		if (stylesheet.getWarnings().getWarningCount() > 0) {
			getLog().warn(
					"Found [" + stylesheet.getWarnings().getWarningCount()
							+ "] warnings.");
			stylesheet.getWarnings().sort();
			String sf = "";
			for (Warning warning : stylesheet.getWarnings().getWarnings()) {
				if (sf != warning.getSourceFile()) {
					sf = warning.getSourceFile();
					getLog().warn("Location: " + sf);
					if (warning.getLevel() <= warningLevel) {
						getLog()
								.warn(
										"Line: "
												+ warning.getLine()
												+ " - "
												+ (warning.getContext() != null ? warning
														.getContext()
														.toString()
														+ " - "
														: "")
												+ warning.getWarningMessage());
					}
				}

			}
		}

	}

	private Hashtable<String, Object> produceError(CssError csserror) {
		Hashtable<String, Object> h = new Hashtable<String, Object>();
		try {
			Throwable ex = csserror.getException();
			// h.put("Error", csserror);
			// h.put("CtxName", "nocontext");
			h.put("CtxMsg", "");
			h.put("ErrorMsg",
					((ex.getMessage() == null) ? "" : ex.getMessage()));
			// h.put("ClassName", "unkownerror");
			if (ex instanceof FileNotFoundException) {
				// h.put("ClassName", "notfound");
				h.put("ErrorMsg", "File not found" + ": " + ex.getMessage());
			} else if (ex instanceof CssParseException) {
				produceParseException((CssParseException) ex, h);
			} else if (ex instanceof InvalidParamException) {
				// h.put("ClassName", "invalidparam");
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
				// h.put("ClassName", "io");

			} else if (csserror instanceof CssErrorToken) {
				CssErrorToken terror = (CssErrorToken) csserror;
				// h.put("ClassName", "errortoken");
				h.put("ErrorMsg", terror.getErrorDescription() + " : "
						+ terror.getSkippedString());
			} else {
				// h.put("ClassName", "unkownerror");
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
		// ht_error.put("ClassName", "parse-error");
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
