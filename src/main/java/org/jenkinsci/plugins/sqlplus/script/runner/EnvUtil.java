package org.jenkinsci.plugins.sqlplus.script.runner;

import java.io.IOException;

import hudson.Launcher;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.remoting.LocalChannel;
import hudson.remoting.VirtualChannel;

public class EnvUtil {

	private static final String WINDOWS_OS = "win";

	private static final String OPERATION_SYSTEM = "os.name";

	private static final String OPERATION_SYSTEM_AGENT = "OS";
	
	public static boolean isAgentMachine(Launcher launcher) {
		VirtualChannel vc = launcher.getChannel();
		return !(vc instanceof LocalChannel);
	}

	public static boolean isWindowsOS(boolean agentMachine,TaskListener listener,Run<?, ?> build) throws IOException, InterruptedException {

		boolean isWindows = false;

		if (agentMachine) {
			String osAgent = build.getEnvironment(listener).get(OPERATION_SYSTEM_AGENT);
			if (osAgent != null) {
				isWindows = osAgent.toLowerCase().contains(WINDOWS_OS);
			}
		} else {
			String osMaster = System.getProperty(OPERATION_SYSTEM);
			if (osMaster != null) {
				isWindows = osMaster.toLowerCase().contains(WINDOWS_OS);
			}
		}

		return isWindows;

	}

}
