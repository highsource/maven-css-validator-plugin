package org.jvnet.mcvp.tests;

import java.net.URL;

import org.apache.velocity.app.Velocity;
import org.w3c.css.css.StyleSheetGenerator;

import junit.framework.TestCase;

public class StyleSheetGeneratorTest extends TestCase {

	public void testInit() throws Exception {

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

//		new StyleSheetGenerator(null, null, null, -1);

	}

}
