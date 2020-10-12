package de.syquel.maven.reactorstate.extension.stub.module2;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;

@Mojo(name = "test", threadSafe = true)
public class TestMojo extends AbstractMojo {

	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {

	}

}
