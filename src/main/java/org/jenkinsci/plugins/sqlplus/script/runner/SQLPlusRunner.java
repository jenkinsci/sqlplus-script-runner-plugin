package org.jenkinsci.plugins.sqlplus.script.runner;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import hudson.EnvVars;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.remoting.LocalChannel;
import hudson.remoting.VirtualChannel;
import hudson.util.ArgumentListBuilder;

/**
 * Run SQLPlus commands on the slave, or master of Jenkins.
 */
@SuppressFBWarnings
public class SQLPlusRunner implements Serializable {

	private static final String FOUND_SQL_PLUS_ON = "found SQL*Plus on ";

	private static final String WINDOWS_FILE_SEPARATOR = "\\";

	private static final long serialVersionUID = -310945626014565712L;

	private static final String WINDOWS_OS = "win";

	private static final String OPERATION_SYSTEM = "os.name";

	private static final String OPERATION_SYSTEM_SLAVE = "OS";

	private static final String MSG_TEMP_SCRIPT = Messages.SQLPlusRunner_tempScript();

	private static final String ON = Messages.SQLPlusRunner_on();

	private static final String MSG_ORACLE_HOME = Messages.SQLPlusRunner_usingOracleHome();

	private static final String MSG_SCRIPT = Messages.SQLPlusRunner_runningScript();

	private static final String MSG_DEFINED_SCRIPT = Messages.SQLPlusRunner_runningDefinedScript();

	private static final String AT = "@";

	private static final String SLASH = "/";

	private static final String MSG_ERROR = Messages.SQLPlusRunner_error();

	private static final String MSG_GET_SQL_PLUS_VERSION = Messages.SQLPlusRunner_gettingSQLPlusVersion();

	private static final String MSG_ORACLE_HOME_MISSING = Messages.SQLPlusRunner_missingOracleHome();

	private static final String MSG_GET_ORACLE_HOME = Messages.SQLPlusRunner_gettingOracleHome();
	private static final String MSG_CUSTOM_ORACLE_HOME = Messages.SQLPlusRunner_usingCustomOracleHome();
	private static final String MSG_CUSTOM_SQLPLUS_HOME = Messages.SQLPlusRunner_usingCustomSQLPlusHome();
	private static final String MSG_CUSTOM_TNS_ADMIN = Messages.SQLPlusRunner_usingCustomTNSAdmin();
	private static final String MSG_GLOBAL_ORACLE_HOME = Messages.SQLPlusRunner_usingGlobalOracleHome();
	private static final String MSG_GLOBAL_SQLPLUS_HOME = Messages.SQLPlusRunner_usingGlobalSQLPlusHome();
	private static final String MSG_GLOBAL_TNS_ADMIN = Messages.SQLPlusRunner_usingGlobalTNSAdmin();
	private static final String MSG_USING_DETECTED_ORACLE_HOME = Messages.SQLPlusRunner_usingDetectedOracleHome();
	private static final String MSG_GLOBAL_ORACLE_HOME_SELECTED = Messages.SQLPlusRunner_globalOracleHomeSelected();
	private static final String MSG_GLOBAL_SQLPLUS_HOME_SELECTED = Messages.SQLPlusRunner_globalSQLPlusHomeSelected();
	private static final String MSG_GLOBAL_TNS_ADMIN_SELECTED = Messages.SQLPlusRunner_globalTNSAdminSelected();

	private static final String MSG_TRY_DETECTED_ORACLE_HOME = Messages.SQLPlusRunner_tryToDetectOracleHome();
	private static final String MSG_GLOBAL_ORACLE_HOME_SELECTED_ANYWAY = Messages
			.SQLPlusRunner_globalOracleHomeSelectedAnyway();

	private static final String LOCAL_DATABASE_MSG = "local";

	private static final String DEBUG_MSG = "[DEBUG] ";

	private static final String HIDDEN_PASSWORD = "********";

	private static final String LINE = Messages.SQLPlusRunner_line();

	// For executing commands
	private static final String LIB_DIR = "lib";
	private static final String BIN_DIR = "bin";
	private static final String NET_DIR = "network";
	private static final String NET_ADM_DIR = "admin";

	private static final String ENV_LD_LIBRARY_PATH = "LD_LIBRARY_PATH";
	private static final String ENV_ORACLE_HOME = "ORACLE_HOME";
	private static final String ENV_TNS_ADMIN = "TNS_ADMIN";

	private static final String SQLPLUS_TRY_LOGIN_JUST_ONCE = "-L";
	private static final String SQLPLUS_VERSION = "-v";
	private static final String SQLPLUS = "sqlplus";
	private static final String SQLPLUS_FOR_WINDOWS = "sqlplus.exe";

	private static final String TNSNAMES_ORA = "tnsnames.ora";

	private static final int PROCESS_EXIT_CODE_SUCCESSFUL = 0;

	public SQLPlusRunner(Run<?, ?> build, TaskListener listener, Launcher launcher, FilePath workspace,
			boolean isHideSQLPlusVersion, String user, String password, String instance, String script,
			String globalOracleHome, String globalSQLPlusHome, String globalTNSAdmin, String scriptType,
			String customOracleHome, String customSQLPlusHome, String customTNSAdmin, boolean tryToDetectOracleHome,
			boolean debug) {
		this.build = build;
		this.listener = listener;
		this.launcher = launcher;
		this.workspace = workspace;
		this.isHideSQLPlusVersion = isHideSQLPlusVersion;
		this.user = user;
		this.password = password;
		this.instance = instance;
		this.script = script;
		this.globalOracleHome = globalOracleHome;
		this.globalSQLPlusHome = globalSQLPlusHome;
		this.globalTNSAdmin = globalTNSAdmin;
		this.scriptType = scriptType;
		this.customOracleHome = customOracleHome;
		this.customSQLPlusHome = customSQLPlusHome;
		this.customTNSAdmin = customTNSAdmin;
		this.tryToDetectOracleHome = tryToDetectOracleHome;
		this.debug = debug;
	}

	private final Run<?, ?> build;

	private final TaskListener listener;

	private final Launcher launcher;

	private final FilePath workspace;

	private final boolean isHideSQLPlusVersion;

	private final String user;

	private final String password;

	private final String instance;

	private String script;

	private String globalOracleHome;

	private String globalSQLPlusHome;

	private String globalTNSAdmin;

	private String customOracleHome;

	private String customSQLPlusHome;

	private String customTNSAdmin;

	private String scriptType;

	private final boolean tryToDetectOracleHome;

	private final boolean debug;

	/**
	 * Main process to run SQLPlus
	 *
	 * @throws IOException
	 * @throws InterruptedException
	 */
	public void run() throws IOException, InterruptedException {

		String selectedOracleHome = null;
		String detectedOracleHome = null;
		boolean slaveMachine = isSlaveMachine(launcher);

		listener.getLogger().println(LINE);
		listener.getLogger().println(MSG_GET_ORACLE_HOME);

		// custom SQLPLUS_HOME overrides file location
		if (customSQLPlusHome != null && customSQLPlusHome.length() > 0) {
			listener.getLogger().println(MSG_CUSTOM_SQLPLUS_HOME);
			listener.getLogger().println("SQL*Plus >>> " + customSQLPlusHome);
		} else if (globalSQLPlusHome != null && globalSQLPlusHome.length() > 0) {
			if (debug)
				listener.getLogger().println(DEBUG_MSG + MSG_GLOBAL_SQLPLUS_HOME_SELECTED);
			listener.getLogger().println(LINE);
			listener.getLogger().println(MSG_GLOBAL_SQLPLUS_HOME);
			customSQLPlusHome = globalSQLPlusHome;
		}

		// custom TNS_ADMIN
		boolean hasCustomTNSAdmin = false;
		if (customTNSAdmin != null && customTNSAdmin.length() > 0) {
			listener.getLogger().println(MSG_CUSTOM_TNS_ADMIN);
			listener.getLogger().println("TNS_ADMIN >>> " + customTNSAdmin);
			hasCustomTNSAdmin = true;
		} else if (globalTNSAdmin != null && globalTNSAdmin.length() > 0) {
			if (debug)
				listener.getLogger().println(DEBUG_MSG + MSG_GLOBAL_TNS_ADMIN_SELECTED);
			listener.getLogger().println(LINE);
			listener.getLogger().println(MSG_GLOBAL_TNS_ADMIN);
			hasCustomTNSAdmin = true;
			customTNSAdmin = globalTNSAdmin;
		}

		// custom ORACLE_HOME overrides everything
		 
		detectedOracleHome = build.getEnvironment(listener).get(ENV_ORACLE_HOME);
		
		if (customOracleHome != null && customOracleHome.length() > 0) {
			if (debug)
				listener.getLogger().println(DEBUG_MSG + MSG_CUSTOM_ORACLE_HOME);
			listener.getLogger().println(LINE);
			listener.getLogger().println(MSG_CUSTOM_ORACLE_HOME);
			selectedOracleHome = customOracleHome;
			// global ORACLE_HOME comes next
		} else if (globalOracleHome != null && globalOracleHome.length() > 0) {
			if (debug)
				listener.getLogger().println(DEBUG_MSG + MSG_GLOBAL_ORACLE_HOME_SELECTED);
			listener.getLogger().println(LINE);
			listener.getLogger().println(MSG_GLOBAL_ORACLE_HOME);
			selectedOracleHome = globalOracleHome;
			// now try to detect ORACLE_HOME
		} else if (tryToDetectOracleHome && detectedOracleHome != null) {
			if (debug)
				listener.getLogger().println(DEBUG_MSG + MSG_TRY_DETECTED_ORACLE_HOME);
			listener.getLogger().println(LINE);
			listener.getLogger().println(MSG_USING_DETECTED_ORACLE_HOME);
			selectedOracleHome = detectedOracleHome;
		} else {
			// nothing works, get global ORACLE_HOME
			if (debug)
				listener.getLogger().println(DEBUG_MSG + MSG_GLOBAL_ORACLE_HOME_SELECTED_ANYWAY);
			selectedOracleHome = globalOracleHome;
		}

		if (!isHideSQLPlusVersion) {
			runGetSQLPLusVersion(customSQLPlusHome, selectedOracleHome, listener, launcher);
		}

		if (debug)
			listener.getLogger().println(" detected host = " + NetUtil.getHostName());

		if (selectedOracleHome == null || selectedOracleHome.length() < 1) {
			throw new RuntimeException(MSG_ORACLE_HOME_MISSING);
		}

		boolean hasCustomSQLPlusHome = false;
		if (customSQLPlusHome != null && customSQLPlusHome.length() > 0) {
			hasCustomSQLPlusHome = true;
		}

		if (!slaveMachine && !hasCustomSQLPlusHome) {
			File directoryAccessTest = new File(selectedOracleHome);
			if (debug)
				listener.getLogger().println(DEBUG_MSG + "testing directory " + directoryAccessTest.getAbsolutePath());
			if (!directoryAccessTest.exists()) {
				throw new RuntimeException(Messages.SQLPlusRunner_wrongOracleHome(selectedOracleHome));
			}
		}

		if (script == null || script.length() < 1) {
			throw new RuntimeException(Messages.SQLPlusRunner_missingScript(workspace));
		}

		String instanceStr = LOCAL_DATABASE_MSG;
		if (instance != null) {
			instanceStr = instance;
		}

		listener.getLogger().println(LINE);
		listener.getLogger().println(MSG_ORACLE_HOME + selectedOracleHome);
		listener.getLogger().println(LINE);

		String sqlplus = SQLPLUS;
		String fileSeparator = File.separator;
		if (isWindowsOS(slaveMachine)) {
			sqlplus = SQLPLUS_FOR_WINDOWS;
			fileSeparator = WINDOWS_FILE_SEPARATOR;
		}

		FilePath tempScript = null;
		FilePath scriptFilePath = null;
		if (ScriptType.userDefined.name().equals(scriptType)) {
			listener.getLogger().println(MSG_DEFINED_SCRIPT + " " + user + SLASH + HIDDEN_PASSWORD + AT + instanceStr);
			scriptFilePath = FileUtil.createTempScript(build, workspace, script, slaveMachine);
			tempScript = scriptFilePath;
			listener.getLogger().println(MSG_TEMP_SCRIPT + " " + scriptFilePath.absolutize().toURI());
		} else {

			String strScript = null;
			if (slaveMachine) {
				scriptFilePath = new FilePath(new File(workspace.getRemote() + fileSeparator + script));
			} else {
				if (workspace!= null) {
					strScript = workspace + fileSeparator + script;
					if (strScript != null)
						scriptFilePath = new FilePath(new File(strScript));
				}
			}

			if (scriptFilePath != null)
				listener.getLogger().println(MSG_SCRIPT + " " + scriptFilePath.getRemote() + " " + ON + " " + user
						+ SLASH + HIDDEN_PASSWORD + AT + instanceStr);
			if (debug)
				listener.getLogger().println(DEBUG_MSG + "testing script " + scriptFilePath.getRemote());
			if (!slaveMachine && scriptFilePath != null && !scriptFilePath.exists()) {
				throw new RuntimeException(
						Messages.SQLPlusRunner_missingScript(scriptFilePath.getRemote()));
			}
			if (!slaveMachine && scriptFilePath != null && !FileUtil.hasExitCode(scriptFilePath))
				FileUtil.addExitInTheEnd(scriptFilePath);
		}

		listener.getLogger().println(LINE);

		int exitCode = 0;
		try {
			// and the extra ones for the plugin
			EnvVars envVars = new EnvVars();
			envVars.put(ENV_ORACLE_HOME, selectedOracleHome);
			if (debug)
				listener.getLogger().println(DEBUG_MSG + "ORACLE_HOME = " + selectedOracleHome);
			envVars.put(ENV_LD_LIBRARY_PATH,
					selectedOracleHome + fileSeparator + LIB_DIR + File.pathSeparator + selectedOracleHome);
			if (debug)
				listener.getLogger().println(DEBUG_MSG + "LD_LIBRARY_PATH = " + selectedOracleHome + fileSeparator
						+ LIB_DIR + File.pathSeparator + selectedOracleHome);

			if (hasCustomTNSAdmin) {
				envVars.put(ENV_TNS_ADMIN, customTNSAdmin);
			} else if (slaveMachine) {
				envVars.put(ENV_TNS_ADMIN, selectedOracleHome);
			} else {
				boolean findTNSNAMESOracleHome = FileUtil.findFile(TNSNAMES_ORA, new File(selectedOracleHome));
				boolean findTNSNAMESOracleHomeNetworkAdmin = FileUtil.findFile(TNSNAMES_ORA,
						new File(selectedOracleHome + fileSeparator + NET_DIR + fileSeparator + NET_ADM_DIR));
				if (findTNSNAMESOracleHomeNetworkAdmin) {
					envVars.put(ENV_TNS_ADMIN,
							selectedOracleHome + fileSeparator + NET_DIR + fileSeparator + NET_ADM_DIR);
					if (debug) {
						listener.getLogger().println(DEBUG_MSG + "found TNSNAMES.ORA on "
								+ new File(selectedOracleHome + fileSeparator + NET_DIR + fileSeparator + NET_ADM_DIR)
										.getAbsolutePath());
						listener.getLogger().println(DEBUG_MSG + "TNS_ADMIN = " + selectedOracleHome + fileSeparator
								+ NET_DIR + fileSeparator + NET_ADM_DIR);
					}
				} else if (findTNSNAMESOracleHome) {
					envVars.put(ENV_TNS_ADMIN, selectedOracleHome);
					if (debug) {
						listener.getLogger().println(
								DEBUG_MSG + "found TNSNAMES.ORA on " + new File(selectedOracleHome).getAbsolutePath());
						listener.getLogger().println(DEBUG_MSG + "TNS_ADMIN = " + selectedOracleHome);
					}
				} else {
					throw new RuntimeException(Messages.SQLPlusRunner_missingTNSNAMES());
				}
			}

			// create command arguments
			ArgumentListBuilder args = new ArgumentListBuilder();

			String arg1 = user + SLASH + password;
			if (instance != null) {
				arg1 = arg1 + AT + instance;
			}

			String arg2 = scriptFilePath.getRemote();

			if (debug)
				listener.getLogger().println("Master Work Directory = " + workspace);

			if (hasCustomSQLPlusHome) {
				args.add(customSQLPlusHome);
			} else {

				listener.getLogger().println("SQL*Plus exec file = " + sqlplus);

				boolean findSQLPlusOnOracleHomeBin = FileUtil.findFile(sqlplus,
						new File(selectedOracleHome + fileSeparator + BIN_DIR));

				boolean findSQLPlusOnOracleHome = FileUtil.findFile(sqlplus, new File(selectedOracleHome));

				if (findSQLPlusOnOracleHomeBin) {
					if (debug)
						listener.getLogger().println(DEBUG_MSG + FOUND_SQL_PLUS_ON
								+ new File(selectedOracleHome + fileSeparator + BIN_DIR).getAbsolutePath());
					args.add(selectedOracleHome + fileSeparator + BIN_DIR + fileSeparator + sqlplus);
				} else if (findSQLPlusOnOracleHome) {
					if (debug)
						listener.getLogger().println(
								DEBUG_MSG + FOUND_SQL_PLUS_ON + new File(selectedOracleHome).getAbsolutePath());
					args.add(selectedOracleHome + fileSeparator + sqlplus);
				} else if (slaveMachine) {
					listener.getLogger().println("SQL*Plus directory: " + selectedOracleHome + fileSeparator + BIN_DIR);
					args.add(selectedOracleHome + fileSeparator + BIN_DIR + fileSeparator + sqlplus);
				} else {
					throw new RuntimeException(Messages.SQLPlusRunner_missingSQLPlus());
				}
			}

			args.add(SQLPLUS_TRY_LOGIN_JUST_ONCE);
			args.add(arg1);
			args.add(AT + arg2);

			if (debug) {
				listener.getLogger().println(DEBUG_MSG + " Statement: ");
				listener.getLogger().println(LINE);
				for (String a : args.toList()) {
					listener.getLogger().print(a + " ");
				}
				listener.getLogger().println(" ");
				listener.getLogger().println(LINE);
				listener.getLogger().println(" ");
			}

			if (slaveMachine) {
				FilePath pwdDir = workspace;
				exitCode = launcher.launch().cmds(args).envs(build.getEnvironment(listener)).stdout(listener)
						.pwd(pwdDir).join();
			} else {
				exitCode = launcher.launch().cmds(args).envs(build.getEnvironment(listener)).stdout(listener)
						.pwd(workspace).join();
			}

			listener.getLogger().printf(Messages.SQLPlusRunner_processEnd() + " %d%n", exitCode);

		} catch (RuntimeException e) {
			throw e;
		} catch (Exception e) {
			listener.getLogger().println(MSG_ERROR + e.getMessage());
			throw new RuntimeException(e);
		} finally {
			if (tempScript != null) {
				try {
					boolean removed = tempScript.delete();
					if (!removed)
						listener.getLogger().printf(Messages.SQLPlusRunner_tempFileNotRemoved());
				} catch (Exception e) {
					listener.getLogger().println(MSG_ERROR + e.getMessage());
				}
			}
		}

		if (exitCode != PROCESS_EXIT_CODE_SUCCESSFUL) {
			listener.getLogger().println(LINE);
			listener.getLogger().println("Exit code: " + exitCode);
			listener.getLogger().println(LINE);
			throw new RuntimeException(Messages.SQLPlusRunner_processErrorEnd());
		}

		listener.getLogger().println(LINE);
	}

	/**
	 * Get SQL Plus version
	 *
	 * @param customSQLPlusHome
	 * @param oracleHome
	 * @param listener
	 * @param launcher
	 */
	public void runGetSQLPLusVersion(String customSQLPlusHome, String oracleHome, TaskListener listener,
			Launcher launcher) {

		if (oracleHome == null || oracleHome.length() < 1) {
			throw new RuntimeException(MSG_ORACLE_HOME_MISSING);
		}

		boolean slaveMachine = isSlaveMachine(launcher);
		if (debug) {
			listener.getLogger().println(" detected host = " + NetUtil.getHostName());
			listener.getLogger().println(" slave machine ? " + slaveMachine);
		}

		boolean hasCustomSQLPlusHome = false;
		if (customSQLPlusHome != null && customSQLPlusHome.length() > 0) {
			hasCustomSQLPlusHome = true;
		}

		if (!slaveMachine && !hasCustomSQLPlusHome) {
			File directoryAccessTest = new File(oracleHome);
			if (!directoryAccessTest.exists()) {
				throw new RuntimeException(Messages.SQLPlusRunner_wrongOracleHome(oracleHome));
			}
		}

		listener.getLogger().println(LINE);
		listener.getLogger().println(MSG_ORACLE_HOME + oracleHome);
		listener.getLogger().println(LINE);
		listener.getLogger().println(MSG_GET_SQL_PLUS_VERSION);
		try {
			String sqlplus = SQLPLUS;
			String fileSeparator = File.separator;
			if (isWindowsOS(slaveMachine)) {
				sqlplus = SQLPLUS_FOR_WINDOWS;
				fileSeparator = WINDOWS_FILE_SEPARATOR;
			}

			EnvVars envVars = new EnvVars();
			envVars.put(ENV_ORACLE_HOME, oracleHome);
			if (debug)
				listener.getLogger().println(DEBUG_MSG + "ORACLE_HOME = " + oracleHome);
			envVars.put(ENV_LD_LIBRARY_PATH, oracleHome + fileSeparator + LIB_DIR);
			if (debug)
				listener.getLogger().println(DEBUG_MSG + "LD_LIBRARY_PATH = " + oracleHome + fileSeparator + LIB_DIR);

			// create command arguments
			ArgumentListBuilder args = new ArgumentListBuilder();

			if (debug)
				listener.getLogger().println("SQL*Plus exec file = " + sqlplus);

			if (hasCustomSQLPlusHome) {
				args.add(customSQLPlusHome);
			} else {

				boolean findSQLPlusOnOracleHomeBin = FileUtil.findFile(sqlplus,
						new File(oracleHome + fileSeparator + BIN_DIR));

				boolean findSQLPlusOnOracleHome = FileUtil.findFile(sqlplus, new File(oracleHome));

				if (findSQLPlusOnOracleHomeBin) {
					listener.getLogger().println(
							FOUND_SQL_PLUS_ON + new File(oracleHome + fileSeparator + BIN_DIR).getAbsolutePath());
					args.add(oracleHome + fileSeparator + BIN_DIR + fileSeparator + sqlplus);
				} else if (findSQLPlusOnOracleHome) {
					listener.getLogger().println(FOUND_SQL_PLUS_ON + new File(oracleHome).getAbsolutePath());
					args.add(oracleHome + fileSeparator + sqlplus);
				} else if (slaveMachine) {
					listener.getLogger().println("SQL*Plus directory: " + oracleHome + fileSeparator + BIN_DIR);
					args.add(oracleHome + fileSeparator + BIN_DIR + fileSeparator + sqlplus);
				} else {
					throw new RuntimeException(Messages.SQLPlusRunner_missingSQLPlus());
				}
			}

			args.add(SQLPLUS_VERSION);

			if (debug) {
				listener.getLogger().println(LINE);
				listener.getLogger().println(DEBUG_MSG + "Statement:");
				for (String a : args.toList()) {
					listener.getLogger().print(a + " ");
				}
				listener.getLogger().println(" ");
				listener.getLogger().println(LINE);
			}

			int exitCode = 0;
			if (slaveMachine) {
				FilePath pwdDir = workspace;
				exitCode = launcher.launch().cmds(args).envs(build.getEnvironment(listener)).stdout(listener)
						.pwd(pwdDir).join();
			} else {
				exitCode = launcher.launch().cmds(args).envs(build.getEnvironment(listener)).stdout(listener)
						.pwd(workspace).join();
			}

			listener.getLogger().printf(Messages.SQLPlusRunner_processEnd() + " %d%n", exitCode);

		} catch (RuntimeException e) {
			listener.getLogger().println(MSG_ERROR + e.getMessage());
			throw e;
		} catch (Exception e) {
			listener.getLogger().println(MSG_ERROR + e.getMessage());
			throw new RuntimeException(e);
		}
		listener.getLogger().println(LINE);
	}

	private boolean isSlaveMachine(Launcher launcher) {
		VirtualChannel vc = launcher.getChannel();
		boolean slaveMachine = true;
		if (vc instanceof LocalChannel) {
			slaveMachine = false;
		}
		return slaveMachine;
	}

	private boolean isWindowsOS(boolean slaveMachine) throws IOException, InterruptedException {

		boolean isWindows = false;

		if (slaveMachine) {
			String osSlave = build.getEnvironment(listener).get(OPERATION_SYSTEM_SLAVE);
			if (osSlave != null) {
				isWindows = osSlave.toLowerCase().indexOf(WINDOWS_OS) >= 0;
			}
		} else {
			String osMaster = System.getProperty(OPERATION_SYSTEM);
			if (osMaster != null) {
				isWindows = osMaster.toLowerCase().indexOf(WINDOWS_OS) >= 0;
			}
		}

		return isWindows;

	}

}
