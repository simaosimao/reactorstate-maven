package de.syquel.maven.reactorstate.plugin;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

import javax.inject.Inject;

import org.apache.maven.cli.internal.extension.model.CoreExtension;
import org.apache.maven.cli.internal.extension.model.CoreExtensions;
import org.apache.maven.cli.internal.extension.model.io.xpp3.CoreExtensionsXpp3Reader;
import org.apache.maven.cli.internal.extension.model.io.xpp3.CoreExtensionsXpp3Writer;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.util.artifact.ArtifactIdUtils;

import de.syquel.maven.reactorstate.plugin.config.ReactorStatePluginProperties;

/**
 * Installs the reactorstate-maven-extension to the root of the current Maven project by inserting it into <code>.mvn/extensions.xml</code>.
 */
@Mojo(name = "install-local", requiresDirectInvocation = true, threadSafe = true, inheritByDefault = false)
public class ReactorStateInstallLocalMojo extends AbstractMojo {

	/**
	 * The location of the Maven core extension configuration file.
	 */
	private static final String EXTENSIONS_FILENAME = ".mvn/extensions.xml";

	/**
	 * The current Maven execution context.
	 */
	private final MavenSession mavenSession;

	/**
	 * Constructs a new instance based on the current maven execution context.
	 *
	 * @param mavenSession
	 */
	@Inject
	public ReactorStateInstallLocalMojo(final MavenSession mavenSession) {
		this.mavenSession = mavenSession;
	}

	/**
	 * Installs the reactorstate-maven-extension to the root of the current Maven project by inserting it into <code>.mvn/extensions.xml</code>.
	 *
	 * The file is automatically created if it does not exist; otherwise already registered extensions are preserved.
	 *
	 * @throws MojoExecutionException never.
	 * @throws MojoFailureException if an error occurred while registering the extension.
	 */
	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {
		final Artifact reactorStateExtensionArtifact;
		try {
			final ReactorStatePluginProperties properties = ReactorStatePluginProperties.create();
			reactorStateExtensionArtifact = properties.getExtensionArtifact();
		} catch (final IOException e) {
			throw new MojoFailureException("Cannot determine extension artifact", e);
		}

		final CoreExtensions coreExtensions = fetchCoreExtensions();
		final CoreExtension reactorStateExtension = new CoreExtension();
		reactorStateExtension.setGroupId(reactorStateExtensionArtifact.getGroupId());
		reactorStateExtension.setArtifactId(reactorStateExtensionArtifact.getArtifactId());
		reactorStateExtension.setVersion(reactorStateExtensionArtifact.getVersion());

		for (final CoreExtension coreExtension : coreExtensions.getExtensions()) {
			final boolean sameGroupId = Objects.equals(reactorStateExtension.getGroupId(), coreExtension.getGroupId());
			final boolean sameArtifactId = Objects.equals(reactorStateExtension.getGroupId(), coreExtension.getGroupId());

			if (sameGroupId && sameArtifactId) {
				getLog().info(ArtifactIdUtils.toId(reactorStateExtensionArtifact) + " already registered");
				return;
			}
		}

		coreExtensions.addExtension(reactorStateExtension);

		persistCoreExtensions(coreExtensions);
		getLog().info("Registered " + ArtifactIdUtils.toId(reactorStateExtensionArtifact));
	}

	/**
	 * Fetches the current Maven Core Extensions configuration from the root of the Maven project.
	 *
	 * @return The current extensions configuration or a new one if none exists.
	 * @throws MojoFailureException if an error occurred while reading the current extensions configuration.
	 */
	private CoreExtensions fetchCoreExtensions() throws MojoFailureException {
		final Path projectBaseDir = mavenSession.getTopLevelProject().getBasedir().toPath();
		final Path mavenExtensionsPath = projectBaseDir.resolve(EXTENSIONS_FILENAME);

		final CoreExtensions coreExtensions;
		if (Files.notExists(mavenExtensionsPath)) {
			try {
				Files.createDirectories(mavenExtensionsPath.getParent());
				Files.createFile(mavenExtensionsPath);
			} catch (final IOException e) {
				throw new MojoFailureException("Cannot create Maven core extensions", e);
			}

			coreExtensions = new CoreExtensions();
			coreExtensions.setModelEncoding(StandardCharsets.UTF_8.name());
		} else {
			try (final Reader mavenExtensionsReader = Files.newBufferedReader(mavenExtensionsPath)) {
				coreExtensions = new CoreExtensionsXpp3Reader().read(mavenExtensionsReader);
			} catch (final XmlPullParserException | IOException e) {
				throw new MojoFailureException("Cannot read Maven core extensions", e);
			}
		}

		return coreExtensions;
	}

	/**
	 * Persists the Maven Core Extensions configuration to <code>.mvn/extensions.xml</code>.
	 *
	 * @param coreExtensions The extensions configuration to persist.
	 * @throws MojoFailureException if an error occurred while persisting the extensions configuration.
	 */
	private void persistCoreExtensions(final CoreExtensions coreExtensions) throws MojoFailureException {
		final Path projectBaseDir = mavenSession.getTopLevelProject().getBasedir().toPath();
		final Path mavenExtensionsPath = projectBaseDir.resolve(EXTENSIONS_FILENAME);

		try (final Writer mavenExtensionsWriter = Files.newBufferedWriter(mavenExtensionsPath)) {
			new CoreExtensionsXpp3Writer().write(mavenExtensionsWriter, coreExtensions);
		} catch (final IOException e) {
			throw new MojoFailureException("Cannot write Maven core extensions", e);
		}
	}

}
