package hudson.plugins.xvnc;

import java.io.PrintStream;
import java.util.Map;

import hudson.FilePath;
import hudson.Launcher;
import hudson.Launcher.ProcStarter;
import hudson.Proc;
import hudson.model.AbstractBuild;
import hudson.model.InvisibleAction;

public class XvncEnvironment extends InvisibleAction {
    private boolean takeScreenshot;
    private AbstractBuild<?, ?> build;
    private PrintStream logger;
    private Map<String, String> xauthorityEnv;
    private int displayNumber;
    private String vncserverCommand;
    private Proc proc;
    private DisplayAllocator allocator;
    private FilePath xauthority;

    public XvncEnvironment(boolean takeScreenshot, AbstractBuild<?, ?> build, PrintStream logger, 
            Map<String, String> xauthorityEnv, int displayNumber, String vncserverCommand, Proc proc, DisplayAllocator allocator,
            FilePath xauthority) {
        this.takeScreenshot = takeScreenshot;
        this.build = build;
        this.logger = logger;
        this.xauthorityEnv = xauthorityEnv;
        this.displayNumber = displayNumber;
        this.vncserverCommand = vncserverCommand;
        this.proc = proc;
        this.allocator = allocator;
        this.xauthority = xauthority;
    }

    public boolean takeScreenshot() {
        return takeScreenshot;
    }

    public AbstractBuild<?, ?> build() {
        return build;
    }

    public PrintStream logger() {
        return logger;
    }

    public Map<String, String> xauthorityEnv() {
        return xauthorityEnv;
    }

    public int displayNumber() {
        return displayNumber;
    }

    public String vncServerCommand() {
        return vncserverCommand;
    }

    public Proc proc() {
        return proc;
    }

    public DisplayAllocator allocator() {
        return allocator; 
    }

    public FilePath xauthority() {
        return xauthority;
    }
}
