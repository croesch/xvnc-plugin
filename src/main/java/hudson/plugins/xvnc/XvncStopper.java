package hudson.plugins.xvnc;

import java.io.IOException;

import org.kohsuke.stapler.DataBoundConstructor;

import hudson.Extension;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Action;
import hudson.model.BuildListener;
import hudson.model.TaskListener;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.BuildWrapper;
import hudson.tasks.BuildWrapperDescriptor;
import hudson.tasks.Builder;
import hudson.tasks.Publisher;
import hudson.tasks.Recorder;

public class XvncStopper extends Builder {
	@Override
	public boolean perform(AbstractBuild<?, ?> build, Launcher launcher,
			BuildListener listener) throws InterruptedException, IOException {
		listener.getLogger().println("***Ende***" +build.isBuilding());
		for(Action action : build.getActions()) {
			if(action instanceof XvncAction) {
				listener.getLogger().println(((XvncAction) action).getDisplayNumber());
				return true;
			}
		}
		listener.getLogger().println("Could not determine display number.");
		return true;
	}

	@Extension
	public static final class DescriptorImpl extends
			BuildStepDescriptor<Builder> {

		/**
		 * This human readable name is used in the configuration screen.
		 */
		public String getDisplayName() {
			return "Ende vom xvnc";
		}

		@Override
		public boolean isApplicable(Class<? extends AbstractProject> jobType) {
			return true;
		}
	}

	@DataBoundConstructor
	public XvncStopper() {

	}
}
