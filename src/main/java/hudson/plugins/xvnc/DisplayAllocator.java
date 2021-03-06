package hudson.plugins.xvnc;

import hudson.Extension;
import hudson.model.Node;
import hudson.slaves.NodeProperty;
import hudson.slaves.NodePropertyDescriptor;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;

/**
 * Manages the display numbers in use.
 *
 * @author Kohsuke Kawaguchi
 */
final class DisplayAllocator {
    /**
     * Display numbers in use.
     */
    private final Set<Integer> allocatedNumbers = new HashSet<Integer>();
    private final Set<Integer> blacklistedNumbers = new HashSet<Integer>();

    public DisplayAllocator() {
    }

    private int getRandomValue(final int min, final int max) {
        return min + (new Random().nextInt(getRange(min, max)));
    }

    private int getRange(final int min, final int max) {
        return (max + 1) - min;
    }

    public synchronized int allocate(final int minDisplayNumber, final int maxDisplayNumber) {
        if (noDisplayNumbersLeft(minDisplayNumber, maxDisplayNumber)) {
            if (!blacklistedNumbers.isEmpty()) {
                blacklistedNumbers.clear();
            } else {
                throw new RuntimeException("All available display numbers are allocated or " +
                        "blacklisted.\nallocated: " + allocatedNumbers.toString() +
                        "\nblacklisted: " + blacklistedNumbers.toString());
            }
        }
        int displayNumber;
        do {
            displayNumber = getRandomValue(minDisplayNumber, maxDisplayNumber);
        } while(isNotAvailable(displayNumber));
        allocatedNumbers.add(displayNumber);
        return displayNumber;
    }

    private boolean isNotAvailable(int number) {
        return allocatedNumbers.contains(number) || blacklistedNumbers.contains(number);
    }

    private boolean noDisplayNumbersLeft(final int min, final int max) {
        return allocatedNumbers.size() + blacklistedNumbers.size() >= getRange(min, max);
    }

    public synchronized void free(int n) {
        allocatedNumbers.remove(n);
    }

    public void blacklist(int badDisplay) {
        free(badDisplay);
        blacklistedNumbers.add(badDisplay);
    }

    @Restricted(NoExternalUse.class)
    /*package*/ static final class Property extends NodeProperty<Node> {

        private transient /*final*/ DisplayAllocator allocator = new DisplayAllocator();

        /*package*/ DisplayAllocator getAllocator() {
            return allocator;
        }

        private Object readResolve() {
            allocator = new DisplayAllocator();
            return this;
        }

        @Extension
        public static class DescriptorImpl extends NodePropertyDescriptor {

            @Override
            public String getDisplayName() { return null; }
        }
    }
}
