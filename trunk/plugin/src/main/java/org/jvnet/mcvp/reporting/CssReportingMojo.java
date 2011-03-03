/**
 *
 */
package org.jvnet.mcvp.reporting;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Locale;

import org.apache.maven.doxia.sink.Sink;
import org.apache.maven.doxia.siterenderer.Renderer;
import org.apache.maven.project.MavenProject;
import org.apache.maven.reporting.AbstractMavenReport;
import org.apache.maven.reporting.MavenReportException;
import org.jfrog.maven.annomojo.annotations.MojoGoal;
import org.jfrog.maven.annomojo.annotations.MojoParameter;
import org.jfrog.maven.annomojo.annotations.MojoPhase;
import org.jvnet.mcvp.Constants;

@MojoGoal("report")
@MojoPhase("site")
public class CssReportingMojo extends AbstractMavenReport {

    /**
     * The location of the report file created by the CSS Validator.
     */
    @MojoParameter(expression = Constants.REPORT_LOCATION)
    private File reportFile;

    /**
     * Location where generated html will be created.
     * 
     */
    @MojoParameter(expression = "${project.reporting.outputDirectory}")
    private File outputDirectory;

    /**
     * <i>Maven Internal</i>: The Doxia Site Renderer.
     * 
     * @component
     */
    private Renderer siteRenderer;

    /**
     * <i>Maven Internal</i>: The Project descriptor.
     * 
     * @parameter expression="${project}"
     * @required
     * @readonly
     */
    private MavenProject project;

    protected String getOutputDirectory() {
	return outputDirectory.getAbsolutePath();
    }

    /**
     * @return Returns the siteRenderer.
     */
    public Renderer getSiteRenderer() {
	return siteRenderer;
    }

    protected MavenProject getProject() {
	return project;
    }

    protected void executeReport(Locale locale) throws MavenReportException {

	Sink sink = getSink();
	sink.body();
	sink.section1();
	sink.sectionTitle1();
	sink.text(getDescription(locale));

	sink.sectionTitle1_();
	sink.section1_();

	readReportToSink(sink);

	sink.body_();

	sink.flush();

	sink.close();

    }

    /**
     * Read the report from disk and write to the sink
     * 
     * @param Sink
     *            the {@link Sink}
     */
    private StringBuilder readReportToSink(Sink sink)
	    throws MavenReportException {
	StringBuilder sb = new StringBuilder();
	BufferedReader br = null;
	try {
	    br = new BufferedReader(new FileReader(reportFile));
	    // read in 8k blocks
	    char[] buf = new char[8192];
	    int numRead = 0;
	    while ((numRead = br.read(buf)) != -1) {
		String readData = String.valueOf(buf, 0, numRead);
		sink.rawText(readData);
	    }
	} catch (IOException e) {
	    throw new MavenReportException(
		    "Failure trying to get read report "
			    + reportFile.getAbsolutePath()
			    + " was <createReport>true</createReport> specified in the build section for the maven-css-validator-plugin?",
		    e);
	    // try and close the stream
	} finally {
	    if (br != null) {
		try {
		    br.close();
		} catch (IOException e) {
		    throw new MavenReportException("couldn't close the stream",
			    e);
		}
	    }
	}
	return sb;

    }

    public String getOutputName() {
	return "css-validation-report";
    }

    public String getName(Locale arg0) {
	return "CSS Validation";
    }

    public String getDescription(Locale arg0) {
	return "CSS Validaton Results";
    }

    /**
     * @param outputDirectory
     *            The outputDirectory to set.
     */
    public void setOutputDirectory(File outputDirectory) {
	this.outputDirectory = outputDirectory;
    }

    /**
     * @param siteRenderer
     *            The siteRenderer to set.
     */
    public void setSiteRenderer(Renderer siteRenderer) {
	this.siteRenderer = siteRenderer;
    }

    /**
     * For testing purpose only.
     * 
     * @param project
     *            The project to set.
     */
    public void setProject(MavenProject project) {
	this.project = project;
    }

}