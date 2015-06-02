package hudson.plugins.xvnc;

import hudson.Extension;
import hudson.Launcher;
import hudson.model.BuildListener;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;

import java.io.IOException;

import org.kohsuke.stapler.DataBoundConstructor;

public class XvncStarter extends Builder {

	@Override
	public boolean perform(AbstractBuild<?, ?> build, Launcher launcher,
			BuildListener listener) throws InterruptedException, IOException {
		listener.getLogger().println("***Start***" + build.isBuilding());
		build.addAction(new XvncAction(12));
		return true;
	}

	@Extension
	public static final class DescriptorImpl extends
			BuildStepDescriptor<Builder> {

		/**
		 * This human readable name is used in the configuration screen.
		 */
		public String getDisplayName() {
			return "Start vom xvnc";
		}

		@Override
		public boolean isApplicable(Class<? extends AbstractProject> jobType) {
			return true;
		}
	}

	@DataBoundConstructor
	public XvncStarter() {

	}
}
