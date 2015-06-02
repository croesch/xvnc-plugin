package hudson.plugins.xvnc;

import java.io.IOException;
import java.io.PrintStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;

import hudson.FilePath;
import hudson.Launcher;
import hudson.Proc;
import hudson.Util;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.Computer;
import hudson.model.Node;
import hudson.tasks.BuildWrapper.Environment;
import jenkins.model.Jenkins;

public class XvncUtil {
	static boolean shouldSkipXvncExecution(AbstractBuild build,
			final Launcher launcher, Xvnc.DescriptorImpl DESCRIPTOR) {
		if (build.getBuiltOn().getAssignedLabels()
				.contains(Jenkins.getInstance().getLabelAtom("noxvnc"))
				|| build.getBuiltOn().getNodeProperties()
						.get(NodePropertyImpl.class) != null) {
			return true;
		}

		if (DESCRIPTOR.skipOnWindows && !launcher.isUnix()) {
			return true;
		}
		return false;
	}
	
	static void start(AbstractBuild build,
			final Launcher launcher, BuildListener listener,Xvnc.DescriptorImpl DESCRIPTOR, Boolean useXauthority) throws IOException, InterruptedException {
		PrintStream logger=listener.getLogger();
		
        if (DESCRIPTOR.cleanUp) {
            maybeCleanUp(launcher, listener);
        }

        String cmd = Util.nullify(DESCRIPTOR.xvnc);
        if (cmd == null) {
            cmd = "vncserver :$DISPLAY_NUMBER -localhost -nolisten tcp";
        }

		doSetUp(build, launcher, logger, cmd, 10, DESCRIPTOR.minDisplayNumber,
                DESCRIPTOR.maxDisplayNumber,useXauthority);
	}

    private static void doSetUp(AbstractBuild build, final Launcher launcher, final PrintStream logger,
            String cmd, int retries, int minDisplayNumber, int maxDisplayNumber, Boolean useXauthority)
                    throws IOException, InterruptedException {

        final DisplayAllocator allocator = getAllocator(build);

        final int displayNumber = allocator.allocate(minDisplayNumber, maxDisplayNumber);
        final String actualCmd = Util.replaceMacro(cmd, Collections.singletonMap("DISPLAY_NUMBER",String.valueOf(displayNumber)));

        logger.println(Messages.Xvnc_STARTING());

        String[] cmds = Util.tokenize(actualCmd);

        final FilePath xauthority = build.getWorkspace().createTempFile(".Xauthority-", "");
        final Map<String,String> xauthorityEnv = new HashMap<String, String>();
        if (useXauthority) {
            xauthorityEnv.put("XAUTHORITY", xauthority.getRemote());
        }

        final Proc proc = launcher.launch().cmds(cmds).envs(xauthorityEnv).stdout(logger).pwd(build.getWorkspace()).start();
        final String vncserverCommand;
        if (cmds[0].endsWith("vncserver") && cmd.contains(":$DISPLAY_NUMBER")) {
            // Command just started the server; -kill will stop it.
            vncserverCommand = cmds[0];
            int exit = proc.join();
            if (exit != 0) {
                // XXX I18N
                String message = "Failed to run \'" + actualCmd + "\' (exit code " + exit + "), blacklisting display #" + displayNumber +
                        "; consider checking the \"Clean up before start\" option";
                // Do not release it; it may be "stuck" until cleaned up by an administrator.
                //allocator.free(displayNumber);
                allocator.blacklist(displayNumber);
                if (retries > 0) {
                     doSetUp(build, launcher, logger, cmd, retries - 1,
                            minDisplayNumber, maxDisplayNumber,useXauthority);
                     return;
                } else {
                    throw new IOException(message);
                }
            }
        } else {
            vncserverCommand = null;
        }
    }
    
    private  static DisplayAllocator getAllocator(AbstractBuild<?, ?> build) throws IOException {
        DisplayAllocator.Property property = build.getBuiltOn().getNodeProperties().get(DisplayAllocator.Property.class);
        if (property == null) {
            property = new DisplayAllocator.Property();
            build.getBuiltOn().getNodeProperties().add(property);
        }
        return property.getAllocator();
    }

    /**
     * Whether {@link #maybeCleanUp} has already been run on a given node.
     */
    private static final Map<Node,Boolean> cleanedUpOn = new WeakHashMap<Node,Boolean>();

    // XXX I18N
    private static synchronized void maybeCleanUp(Launcher launcher, BuildListener listener) throws IOException, InterruptedException {
        Node node = Computer.currentComputer().getNode();
        if (cleanedUpOn.put(node, true) != null) {
            return;
        }
        if (!launcher.isUnix()) {
            listener.error("Clean up not currently implemented for non-Unix nodes; skipping");
            return;
        }
        PrintStream logger = listener.getLogger();
        // ignore any error return codes
        launcher.launch().stdout(logger).cmds("pkill", "Xvnc").join();
        launcher.launch().stdout(logger).cmds("pkill", "Xrealvnc").join();
        launcher.launch().stdout(logger).cmds("sh", "-c", "rm -f /tmp/.X*-lock /tmp/.X11-unix/X*").join();
    }
}
