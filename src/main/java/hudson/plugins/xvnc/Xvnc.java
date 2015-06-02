package hudson.plugins.xvnc;

import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.Proc;
import hudson.Util;
import hudson.model.BuildListener;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Computer;
import hudson.model.Hudson;
import hudson.model.Node;
import hudson.tasks.BuildWrapper;
import hudson.tasks.BuildWrapperDescriptor;
import hudson.util.FormValidation;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;

import jenkins.model.Jenkins;
import net.sf.json.JSONObject;

import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

/**
 * {@link BuildWrapper} that runs <tt>xvnc</tt>.
 *
 * @author Kohsuke Kawaguchi
 */
public class Xvnc extends BuildWrapper {
    /**
     * Whether or not to take a screenshot upon completion of the build.
     */
    public boolean takeScreenshot;

    public Boolean useXauthority;

    private static final String FILENAME_SCREENSHOT = "screenshot.jpg";

    @DataBoundConstructor
    public Xvnc(boolean takeScreenshot, boolean useXauthority) {
        this.takeScreenshot = takeScreenshot;
        this.useXauthority = useXauthority;
    }

    @Override
    public Environment setUp(AbstractBuild build, final Launcher launcher, BuildListener listener)
            throws IOException, InterruptedException {
        DescriptorImpl DESCRIPTOR = Hudson.getInstance().getDescriptorByType(DescriptorImpl.class);

        // skip xvnc execution
        if(XvncUtil.shouldSkipXvncExecution(build, launcher, DESCRIPTOR)) {
            return new Environment(){};
        }

        XvncUtil.start(build, launcher, listener, DESCRIPTOR,useXauthority);

        return new Environment() {

                @Override
            public void buildEnvVars(Map<String, String> env) {
                env.put("DISPLAY",":"+displayNumber);
                env.putAll(xauthorityEnv);
            }

            @Override
            public boolean tearDown(AbstractBuild build, BuildListener listener) throws IOException, InterruptedException {
                if (takeScreenshot) {
                    FilePath ws = build.getWorkspace();
                    File artifactsDir = build.getArtifactsDir();
                    artifactsDir.mkdirs();
                    logger.println(Messages.Xvnc_TAKING_SCREENSHOT());
                    launcher.launch().cmds("echo", "$XAUTHORITY").envs(xauthorityEnv).stdout(logger).pwd(ws).join();
                    launcher.launch().cmds("ls", "-l", "$XAUTHORITY").envs(xauthorityEnv).stdout(logger).pwd(ws).join();
                    launcher.launch().cmds("import", "-window", "root", "-display", ":" + displayNumber, FILENAME_SCREENSHOT).
                            envs(xauthorityEnv).stdout(logger).pwd(ws).join();
                    ws.child(FILENAME_SCREENSHOT).copyTo(new FilePath(artifactsDir).child(FILENAME_SCREENSHOT));
                }
                logger.println(Messages.Xvnc_TERMINATING());
                if (vncserverCommand != null) {
                    // #173: stopping the wrapper script will accomplish nothing. It has already exited, in fact.
                    launcher.launch().cmds(vncserverCommand, "-kill", ":" + displayNumber).envs(xauthorityEnv).stdout(logger).join();
                } else {
                    // Assume it can be shut down by being killed.
                    proc.kill();
                }
                allocator.free(displayNumber);
                xauthority.delete();
                return true;
            }
        };
    }



    @Extension
    public static final class DescriptorImpl extends BuildWrapperDescriptor {

        /**
         * xvnc command line. This can include macro.
         *
         * If null, the default will kick in.
         */
        public String xvnc;

        /*
         * Base X display number.
         */
        public int minDisplayNumber = 10;

        /*
         * Maximum X display number.
         */
        public int maxDisplayNumber = 99;

        /**
         * If true, skip xvnc launch on all Windows slaves.
         */
        public boolean skipOnWindows = true;

        /**
         * If true, try to clean up old processes and locks when first run.
         */
        public boolean cleanUp = false;

        public DescriptorImpl() {
            super(Xvnc.class);
            load();
        }

        @Override
        public String getDisplayName() {
            return Messages.description();
        }

        @Override
        public boolean configure(StaplerRequest req, JSONObject json) throws FormException {
            // XXX is this now the right style?
            req.bindJSON(this,json);
            save();
            return true;
        }

        @Override
        public boolean isApplicable(AbstractProject<?, ?> item) {
            return true;
        }

        public String getCommandline() {
            return xvnc;
        }

        public void setCommandline(String value) {
            this.xvnc = value;
        }

        public FormValidation doCheckCommandline(@QueryParameter String value) {
            if (Util.nullify(value) == null || value.contains("$DISPLAY_NUMBER")) {
                return FormValidation.ok();
            } else {
                return FormValidation.warningWithMarkup(Messages.Xvnc_SHOULD_INCLUDE_DISPLAY_NUMBER());
            }
        }
    }

    public Object readResolve() {
        if (useXauthority == null) useXauthority = true;
        return this;
    }
}
