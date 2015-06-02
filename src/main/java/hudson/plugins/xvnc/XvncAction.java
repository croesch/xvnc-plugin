package hudson.plugins.xvnc;

import hudson.model.Action;
import hudson.model.InvisibleAction;

public class XvncAction extends InvisibleAction {
	private int displayNumber;

	public XvncAction(int n) {
		displayNumber = n;
	}

	public int getDisplayNumber() {
		return displayNumber;
	}
}
