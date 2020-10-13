package de.syquel.maven.reactorstate.extension.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.maven.cli.internal.extension.model.CoreExtension;
import org.apache.maven.cli.internal.extension.model.CoreExtensions;
import org.apache.maven.cli.internal.extension.model.io.xpp3.CoreExtensionsXpp3Reader;
import org.apache.maven.cli.internal.extension.model.io.xpp3.CoreExtensionsXpp3Writer;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import io.takari.maven.testing.TestProperties;

public final class TestUtils {

	private static final TestProperties TEST_PROPERTIES = new TestProperties();
	private static final Path EXTENSIONS_PATH = Paths.get(".mvn", "extensions.xml");
	private static final Pattern PROPERTY_PATTERN = Pattern.compile("\\$\\{(?<property>[\\w_.\\-]+)}");

	private TestUtils() {
	}

	public static void installExtension(final Path basePath) throws IOException, XmlPullParserException {
		final List<Path> extensionsPaths;
		try (final Stream<Path> extensionsPathsStream = Files.find(basePath, 5, (path, basicFileAttributes) -> path.endsWith(EXTENSIONS_PATH))) {
			extensionsPaths = extensionsPathsStream.collect(Collectors.toList());
		}

		for (final Path extensionsPath : extensionsPaths) {
			final CoreExtensions extensions;
			try (final InputStream extensionsStream = Files.newInputStream(extensionsPath)) {
				extensions = new CoreExtensionsXpp3Reader().read(extensionsStream);
			}

			for (final CoreExtension extension : extensions.getExtensions()) {
				extension.setGroupId(evaluateExpression(extension.getGroupId()));
				extension.setArtifactId(evaluateExpression(extension.getArtifactId()));
				extension.setVersion(evaluateExpression(extension.getVersion()));
			}

			try (final OutputStream extensionsStream = Files.newOutputStream(extensionsPath)) {
				new CoreExtensionsXpp3Writer().write(extensionsStream, extensions);
			}
		}
	}

	public static String evaluateExpression(final String expression) {
		final Matcher expressionMatcher = PROPERTY_PATTERN.matcher(expression);
		if (!expressionMatcher.matches()) {
			return expression;
		}

		return TEST_PROPERTIES.get(expressionMatcher.group("property"));
	}

}
