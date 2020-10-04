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

@Mojo(name = "install-local", requiresDirectInvocation = true, threadSafe = true, inheritByDefault = false)
public class ReactorStateInstallLocalMojo extends AbstractMojo {

	private static final String EXTENSIONS_FILENAME = ".mvn/extensions.xml";

	private final MavenSession mavenSession;

	@Inject
	public ReactorStateInstallLocalMojo(final MavenSession mavenSession) {
		this.mavenSession = mavenSession;
	}

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
