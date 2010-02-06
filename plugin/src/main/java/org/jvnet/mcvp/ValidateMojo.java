package org.jvnet.mcvp;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.codehaus.plexus.util.DirectoryScanner;
import org.jfrog.maven.annomojo.annotations.MojoGoal;
import org.jfrog.maven.annomojo.annotations.MojoParameter;
import org.jfrog.maven.annomojo.annotations.MojoPhase;
import org.w3c.css.css.DocumentParser;
import org.w3c.css.css.StyleSheet;
import org.w3c.css.properties.PropertiesLoader;
import org.w3c.css.util.ApplContext;

@MojoGoal("validate")
@MojoPhase("test")
public class ValidateMojo extends AbstractMojo {

	private boolean cssValidationFailureIgnore = false;

	@MojoParameter(expression = "${maven.cssValidation.failure.ignore}")
	public boolean getCssValidationFailureIgnore() {
		return cssValidationFailureIgnore;
	}

	public void setCssValidationFailureIgnore(boolean cssValidationFailureIgnore) {
		this.cssValidationFailureIgnore = cssValidationFailureIgnore;
	}

	// private boolean printCSS = false;
	//
	// @MojoParameter(defaultValue = "false", required = false, description =
	// "Prints the validated CSS (only with text output, the CSS is printed with other outputs). "
	// + "Possible values are true or false (default).")
	// public boolean getPrintCSS() {
	// return printCSS;
	// }
	//
	// public void setPrintCSS(boolean printCSS) {
	// this.printCSS = printCSS;
	// }

	private String profile = "css21";

	@MojoParameter(defaultValue = "css21", required = false, description = "Checks the stylesheet against the specified profile. "
			+ "Possible values are css1, css2, css21 (default), css3, svg, svgbasic, svgtiny, atsc-tv, mobile, tv.")
	public String getProfile() {
		return profile;
	}

	public void setProfile(String profile) {
		this.profile = profile;
	}

	private String medium = "all";

	@MojoParameter(defaultValue = "all", required = false, description = "Checks the stylesheet using the specified medium. Possible values are all (default), aural, braille, embossed, handheld, print, projection, screen, tty, tv, presentation.")
	public String getMedium() {
		return medium;
	}

	public void setMedium(String medium) {
		this.medium = medium;
	}

	// private String output = "text";
	//
	// @MojoParameter(defaultValue = "text", required = false, description =
	// "Prints the result in the selected format. Possible values are text (default), xhtml, html (same result as xhtml), soap12.")
	// public String getOutput() {
	// return output;
	// }
	//
	// public void setOutput(String output) {
	// this.output = output;
	// }

	// private String lang;
	//
	// @MojoParameter(defaultValue = "en", required = false, description =
	// "Prints the result in the specified language"
	// +
	// "Possible values are de, en (default), es, fr, ja, ko, nl, zh-cn, pl, it.")
	// public String getLang() {
	// return lang;
	// }
	//
	// public void setLang(String lang) {
	// this.lang = lang;
	// }

	private int warning;

	@MojoParameter(defaultValue = "", required = false, description = "Warnings verbosity level. "
			+ "Possible values for WARN are -1 (no warning), 0, 1, 2 (default, all the warnings).")
	public int getWarning() {
		return warning;
	}

	public void setWarning(int warning) {
		this.warning = warning;
	}

	private String[] includes = new String[] { "src/main/webapp/**/*.css" };

	private File directory;

	@MojoParameter(defaultValue = "${project.basedir}", required = true)
	public File getDirectory() {
		return directory;
	}

	public void setDirectory(File directory) {
		this.directory = directory;
	}

	@MojoParameter
	public String[] getIncludes() {
		return includes;
	}

	public void setIncludes(String[] includes) {
		this.includes = includes;
	}

	private String[] excludes = null;

	@MojoParameter
	public String[] getExcludes() {
		return excludes;
	}

	public void setExcludes(String[] excludes) {
		this.excludes = excludes;
	}

	private boolean defaultExcludes = true;

	public boolean getDefaultExcludes() {
		return defaultExcludes;
	}

	public void setDefaultExcludes(boolean defaultExcludes) {
		this.defaultExcludes = defaultExcludes;
	}

	public void execute() throws MojoExecutionException, MojoFailureException {

		final List<File> files;
		try {
			files = getFiles();
		} catch (IOException ioex) {
			throw new MojoExecutionException("Could not list files.", ioex);
		}

		ApplContext ac = new ApplContext("en");

		final String profile = getProfile();

		if (profile != null && !"none".equals(profile)) {
			if ("css1".equals(profile) || "css2".equals(profile)
					|| "css21".equals(profile) || "css3".equals(profile)
					|| "svg".equals(profile) || "svgbasic".equals(profile)
					|| "svgtiny".equals(profile)) {
				ac.setCssVersion(profile);
			} else {
				ac.setProfile(profile);
				ac.setCssVersion(PropertiesLoader.config
						.getProperty("defaultProfile"));
			}
		} else {
			ac.setProfile(profile);
			ac.setCssVersion(PropertiesLoader.config
					.getProperty("defaultProfile"));
		}

		// medium to use
		ac.setMedium(getMedium());

		final StyleSheetReport report = new LogStyleSheetReport(getLog());

		int errors = 0;
		int warnings = 0;
		for (File file : files) {
			try {
				final String url = file.toURI().toURL().toString();

				DocumentParser parser = new DocumentParser(ac, url);

				StyleSheet stylesheet = parser.getStyleSheet();
				stylesheet.findConflicts(ac);

				errors = errors + stylesheet.getErrors().getErrorCount();
				warnings = warnings
						+ stylesheet.getWarnings().getWarningCount();

				report.report(stylesheet);

				// handleRequest(ac, url, stylesheet, getOutput(), getWarning(),
				// true, out);
			} catch (MalformedURLException murlex) {
				throw new MojoExecutionException("Malformed file URI.", murlex);
			} catch (Exception ex) {
				throw new MojoExecutionException("Error parsing stylesheet.",
						ex);
			}
		}

		if (errors == 0) {
			if (warnings != 0) {
				getLog().warn(
						"CSS files were validated with [" + warnings
								+ "] warnings.");
			}
		} else {
			final String message;
			if (warnings != 0) {
				message = "CSS files were validated with [" + errors
						+ "] errors and [" + warnings + "] warnings.";
			} else {
				message = "CSS files were validated with [" + errors
						+ "] errors.";

			}

			if (getCssValidationFailureIgnore()) {
				getLog().error(message);
			} else {
				throw new MojoFailureException(message);
			}
		}

	}

	private List<File> getFiles() throws IOException {
		final DirectoryScanner scanner = new DirectoryScanner();
		scanner.setBasedir(getDirectory().getAbsoluteFile());
		scanner.setIncludes(getIncludes());
		scanner.setExcludes(getExcludes());
		if (getDefaultExcludes()) {
			scanner.addDefaultExcludes();
		}

		scanner.scan();

		final List<File> files = new ArrayList<File>();
		for (final String name : scanner.getIncludedFiles()) {
			files.add(new File(directory, name).getCanonicalFile());
		}
		return files;
	}

}
