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
import hudson.util.ArgumentListBuilder;

/**
 * Run SQLPlus commands on the slave, or master of Jenkins.
 */
@SuppressFBWarnings
public class SQLPlusRunner implements Serializable {

	private static final long serialVersionUID = -2426963507463371935L;

	private static final int PROCESS_EXIT_CODE_SUCCESSFUL = 0;

	public SQLPlusRunner(Run<?, ?> build, TaskListener listener, Launcher launcher, FilePath workspace,
			boolean isHideSQLPlusVersion, String user, String password, String instance, String script,
			String globalOracleHome, String globalSQLPlusHome, String globalTNSAdmin, String scriptType,
			String customOracleHome, String customSQLPlusHome, String customTNSAdmin, String customNLSLang,
			String customSQLPath, boolean tryToDetectOracleHome, boolean debug) {
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
		this.customNLSLang = customNLSLang;
		this.customSQLPath = customSQLPath;
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

	private String globalNLSLang;

	private String globalSQLPath;

	private String customOracleHome;

	private String customSQLPlusHome;

	private String customTNSAdmin;

	private String customNLSLang;

	private String customSQLPath;

	private String scriptType;

	private final boolean tryToDetectOracleHome;

	private final boolean debug;

	/**
	 * Get SQL Plus version
	 *
	 * @param customSQLPlusHome - custom SQL*Plus home
	 * @param oracleHome        - Oracle Home
	 * @param listener          - Jenkins listener
	 * @param launcher          - Jenkins launcher
	 */
	public void runGetSQLPLusVersion(String customSQLPlusHome, String oracleHome, TaskListener listener,
			Launcher launcher) {

		if (oracleHome == null || oracleHome.length() < 1) {
			throw new RuntimeException(MessageUtil.MSG_ORACLE_HOME_MISSING);
		}

		boolean slaveMachine = EnvUtil.isSlaveMachine(launcher);
		if (debug) {
			listener.getLogger()
					.println(MessageUtil.MSG_DEBUG_DETECTED_HOST + MessageUtil.MSG_EQUALS + NetUtil.getHostName());
			listener.getLogger().println(MessageUtil.MSG_DEBUG_SLAVE_MACHINE + MessageUtil.MSG_COLON + slaveMachine);
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

		listener.getLogger().println(MessageUtil.LINE);
		listener.getLogger().println(MessageUtil.MSG_ORACLE_HOME + oracleHome);
		listener.getLogger().println(MessageUtil.LINE);
		listener.getLogger().println(MessageUtil.MSG_GET_SQL_PLUS_VERSION);
		try {
			String sqlplus = MessageUtil.SQLPLUS;
			String fileSeparator = File.separator;
			if (EnvUtil.isWindowsOS(slaveMachine, listener, build)) {
				sqlplus = MessageUtil.SQLPLUS_FOR_WINDOWS;
				fileSeparator = MessageUtil.WINDOWS_FILE_SEPARATOR;
			}

			EnvVars envVars = new EnvVars();
			envVars.put(MessageUtil.ENV_ORACLE_HOME, oracleHome);
			if (debug)
				listener.getLogger().println(MessageUtil.MSG_DEBUG + MessageUtil.MSG_DEBUG_ENV_ORACLE_HOME
						+ MessageUtil.MSG_EQUALS + oracleHome);
			envVars.put(MessageUtil.ENV_LD_LIBRARY_PATH, oracleHome + fileSeparator + MessageUtil.LIB_DIR);
			if (debug)
				listener.getLogger().println(MessageUtil.MSG_DEBUG + MessageUtil.MSG_DEBUG_ENV_LD_LIBRARY_PATH
						+ MessageUtil.MSG_EQUALS + oracleHome + fileSeparator + MessageUtil.LIB_DIR);

			// create command arguments
			ArgumentListBuilder args = new ArgumentListBuilder();

			if (debug)
				listener.getLogger().println(MessageUtil.MSG_DEBUG_EXEC_FILE + MessageUtil.MSG_EQUALS + sqlplus);

			if (hasCustomSQLPlusHome) {
				args.add(customSQLPlusHome);
			} else {

				boolean findSQLPlusOnOracleHomeBin = FileUtil.findFile(sqlplus,
						new File(oracleHome + fileSeparator + MessageUtil.BIN_DIR));

				boolean findSQLPlusOnOracleHome = FileUtil.findFile(sqlplus, new File(oracleHome));

				if (findSQLPlusOnOracleHomeBin) {
					listener.getLogger().println(MessageUtil.FOUND_SQL_PLUS_ON
							+ new File(oracleHome + fileSeparator + MessageUtil.BIN_DIR).getAbsolutePath());
					args.add(oracleHome + fileSeparator + MessageUtil.BIN_DIR + fileSeparator + sqlplus);
				} else if (findSQLPlusOnOracleHome) {
					listener.getLogger()
							.println(MessageUtil.FOUND_SQL_PLUS_ON + new File(oracleHome).getAbsolutePath());
					args.add(oracleHome + fileSeparator + sqlplus);
				} else if (slaveMachine) {
					listener.getLogger().println(MessageUtil.MSG_DEBUG_EXEC_DIR + MessageUtil.MSG_COLON + oracleHome
							+ fileSeparator + MessageUtil.BIN_DIR);
					args.add(oracleHome + fileSeparator + MessageUtil.BIN_DIR + fileSeparator + sqlplus);
				} else {
					throw new RuntimeException(Messages.SQLPlusRunner_missingSQLPlus());
				}
			}

			args.add(MessageUtil.SQLPLUS_VERSION);

			if (debug) {
				listener.getLogger().println(MessageUtil.LINE);
				listener.getLogger().println(MessageUtil.MSG_DEBUG_STATEMENT + MessageUtil.MSG_COLON);
				for (String a : args.toList()) {
					listener.getLogger().print(a + MessageUtil.MSG_SPACE);
				}
				listener.getLogger().println(MessageUtil.MSG_SPACE);
				listener.getLogger().println(MessageUtil.LINE);
			}

			int exitCode = 0;
			exitCode = launcher.launch().cmds(args).envs(build.getEnvironment(listener).overrideAll(envVars))
					.stdout(listener).pwd(workspace).join();

			listener.getLogger().printf(Messages.SQLPlusRunner_processEnd() + " %d%n", exitCode);

		} catch (RuntimeException e) {
			listener.getLogger().println(MessageUtil.MSG_ERROR + e.getMessage());
			throw e;
		} catch (Exception e) {
			listener.getLogger().println(MessageUtil.MSG_ERROR + e.getMessage());
			throw new RuntimeException(e);
		}
		listener.getLogger().println(MessageUtil.LINE);
	}

	/**
	 * Main process to run SQLPlus
	 */
	public void run() throws IOException, InterruptedException {

		String selectedOracleHome = null;
		String detectedOracleHome = null;
		boolean slaveMachine = EnvUtil.isSlaveMachine(launcher);

		listener.getLogger().println(MessageUtil.LINE);
		listener.getLogger().println(MessageUtil.MSG_GET_ORACLE_HOME);

		// custom SQLPLUS_HOME overrides file location
		if (customSQLPlusHome != null && customSQLPlusHome.length() > 0) {
			listener.getLogger().println(MessageUtil.MSG_CUSTOM_SQLPLUS_HOME);
			listener.getLogger().println(MessageUtil.MSG_DEBUG_EXEC_DIR + MessageUtil.MSG_COLON + customSQLPlusHome);
		} else if (globalSQLPlusHome != null && globalSQLPlusHome.length() > 0) {
			if (debug)
				listener.getLogger().println(MessageUtil.MSG_DEBUG + MessageUtil.MSG_GLOBAL_SQLPLUS_HOME_SELECTED);
			listener.getLogger().println(MessageUtil.LINE);
			listener.getLogger().println(MessageUtil.MSG_GLOBAL_SQLPLUS_HOME);
			customSQLPlusHome = globalSQLPlusHome;
		}

		// custom TNS_ADMIN
		boolean hasCustomTNSAdmin = false;
		if (customTNSAdmin != null && customTNSAdmin.length() > 0) {
			listener.getLogger().println(MessageUtil.MSG_CUSTOM_TNS_ADMIN);
			listener.getLogger().println(MessageUtil.MSG_DEBUG_ENV_TNS_ADMIN + MessageUtil.MSG_COLON + customTNSAdmin);
			hasCustomTNSAdmin = true;
		} else if (globalTNSAdmin != null && globalTNSAdmin.length() > 0) {
			if (debug)
				listener.getLogger().println(MessageUtil.MSG_DEBUG + MessageUtil.MSG_GLOBAL_TNS_ADMIN_SELECTED);
			listener.getLogger().println(MessageUtil.LINE);
			listener.getLogger().println(MessageUtil.MSG_GLOBAL_TNS_ADMIN);
			customTNSAdmin = globalTNSAdmin;
			hasCustomTNSAdmin = true;
			listener.getLogger().println(MessageUtil.MSG_DEBUG_ENV_TNS_ADMIN + MessageUtil.MSG_COLON + customTNSAdmin);
		}

		// custom NLS_LANG
		boolean hasCustomNLSLang = false;
		if (customNLSLang != null && customNLSLang.length() > 0) {
			listener.getLogger().println(MessageUtil.MSG_CUSTOM_NLS_LANG);
			listener.getLogger().println(MessageUtil.MSG_DEBUG_ENV_NLS_LANG + MessageUtil.MSG_COLON + customNLSLang);
			hasCustomNLSLang = true;
		} else if (globalNLSLang != null && globalNLSLang.length() > 0) {
			if (debug)
				listener.getLogger().println(MessageUtil.MSG_DEBUG + MessageUtil.MSG_GLOBAL_NLS_LANG_SELECTED);
			listener.getLogger().println(MessageUtil.LINE);
			listener.getLogger().println(MessageUtil.MSG_GLOBAL_NLS_LANG);
			customNLSLang = globalNLSLang;
			hasCustomNLSLang = true;
			listener.getLogger().println(MessageUtil.MSG_DEBUG_ENV_NLS_LANG + MessageUtil.MSG_COLON + customNLSLang);
		}

		// custom SQLPATH
		boolean hasCustomSQLPath = false;
		if (customSQLPath != null && customSQLPath.length() > 0) {
			listener.getLogger().println(MessageUtil.MSG_CUSTOM_SQLPATH);
			listener.getLogger().println(MessageUtil.MSG_DEBUG_ENV_SQLPATH + MessageUtil.MSG_COLON + customSQLPath);
			hasCustomSQLPath = true;
		} else if (globalSQLPath != null && globalSQLPath.length() > 0) {
			if (debug)
				listener.getLogger().println(MessageUtil.MSG_DEBUG + MessageUtil.MSG_GLOBAL_SQLPATH_SELECTED);
			listener.getLogger().println(MessageUtil.LINE);
			listener.getLogger().println(MessageUtil.MSG_GLOBAL_SQLPATH);
			customSQLPath = globalSQLPath;
			hasCustomSQLPath = true;
			listener.getLogger().println(MessageUtil.MSG_DEBUG_ENV_SQLPATH + MessageUtil.MSG_COLON + customSQLPath);
		}

		// custom ORACLE_HOME overrides everything
		detectedOracleHome = build.getEnvironment(listener).get(MessageUtil.ENV_ORACLE_HOME);

		if (customOracleHome != null && customOracleHome.length() > 0) {
			if (debug)
				listener.getLogger().println(MessageUtil.MSG_DEBUG + MessageUtil.MSG_CUSTOM_ORACLE_HOME);
			listener.getLogger().println(MessageUtil.LINE);
			listener.getLogger().println(MessageUtil.MSG_CUSTOM_ORACLE_HOME);
			selectedOracleHome = customOracleHome;
			// global ORACLE_HOME comes next
		} else if (globalOracleHome != null && globalOracleHome.length() > 0) {
			if (debug)
				listener.getLogger().println(MessageUtil.MSG_DEBUG + MessageUtil.MSG_GLOBAL_ORACLE_HOME_SELECTED);
			listener.getLogger().println(MessageUtil.LINE);
			listener.getLogger().println(MessageUtil.MSG_GLOBAL_ORACLE_HOME);
			selectedOracleHome = globalOracleHome;
			// now try to detect ORACLE_HOME
		} else if (tryToDetectOracleHome && detectedOracleHome != null) {
			if (debug)
				listener.getLogger().println(MessageUtil.MSG_DEBUG + MessageUtil.MSG_TRY_DETECTED_ORACLE_HOME);
			listener.getLogger().println(MessageUtil.LINE);
			listener.getLogger().println(MessageUtil.MSG_USING_DETECTED_ORACLE_HOME);
			selectedOracleHome = detectedOracleHome;
		} else {
			// nothing works, get global ORACLE_HOME
			if (debug)
				listener.getLogger()
						.println(MessageUtil.MSG_DEBUG + MessageUtil.MSG_GLOBAL_ORACLE_HOME_SELECTED_ANYWAY);
			selectedOracleHome = globalOracleHome;
		}

		if (!isHideSQLPlusVersion) {
			runGetSQLPLusVersion(customSQLPlusHome, selectedOracleHome, listener, launcher);
		}

		if (debug)
			listener.getLogger()
					.println(MessageUtil.MSG_DEBUG_DETECTED_HOST + MessageUtil.MSG_EQUALS + NetUtil.getHostName());

		// can't find Oracle Home!
		if (selectedOracleHome == null || selectedOracleHome.length() < 1) {
			throw new RuntimeException(MessageUtil.MSG_ORACLE_HOME_MISSING);
		}

		// finding SQL*Plus
		boolean hasCustomSQLPlusHome = false;
		if (customSQLPlusHome != null && customSQLPlusHome.length() > 0) {
			hasCustomSQLPlusHome = true;
		}

		if (!slaveMachine && !hasCustomSQLPlusHome) {
			File directoryAccessTest = new File(selectedOracleHome);
			if (debug)
				listener.getLogger().println(MessageUtil.MSG_DEBUG + MessageUtil.MSG_DEBUG_TEST_DIR
						+ MessageUtil.MSG_COLON + directoryAccessTest.getAbsolutePath());
			if (!directoryAccessTest.exists()) {
				throw new RuntimeException(Messages.SQLPlusRunner_wrongOracleHome(selectedOracleHome));
			}
		}

		// finding SQL script
		if (script == null || script.length() < 1) {
			listener.getLogger().println(MessageUtil.LINE);
			listener.getLogger().println(MessageUtil.MSG_WARNING + Messages.SQLPlusRunner_missingScript(workspace));
			listener.getLogger().println(MessageUtil.LINE);
		}

		String instanceStr = MessageUtil.LOCAL_DATABASE_MSG;
		if (instance != null) {
			instanceStr = instance;
		}

		listener.getLogger().println(MessageUtil.LINE);
		listener.getLogger().println(MessageUtil.MSG_ORACLE_HOME + selectedOracleHome);
		listener.getLogger().println(MessageUtil.LINE);

		String sqlplus = MessageUtil.SQLPLUS;
		String fileSeparator = File.separator;
		if (EnvUtil.isWindowsOS(slaveMachine, listener, build)) {
			sqlplus = MessageUtil.SQLPLUS_FOR_WINDOWS;
			fileSeparator = MessageUtil.WINDOWS_FILE_SEPARATOR;
		}

		FilePath tempScript = null;
		FilePath scriptFilePath = null;
		if (ScriptType.userDefined.name().equals(scriptType)) {
			listener.getLogger().println(MessageUtil.MSG_DEFINED_SCRIPT + MessageUtil.MSG_SPACE + user
					+ MessageUtil.SLASH + MessageUtil.HIDDEN_PASSWORD + MessageUtil.AT + instanceStr);
			scriptFilePath = FileUtil.createTempScript(build, workspace, script, slaveMachine);
			tempScript = scriptFilePath;
			listener.getLogger()
					.println(MessageUtil.MSG_TEMP_SCRIPT + MessageUtil.MSG_SPACE + scriptFilePath.absolutize().toURI());
		} else {

			String strScript = null;
			if (slaveMachine) {
				scriptFilePath = new FilePath(new File(workspace.getRemote() + fileSeparator + script));
			} else {
				if (workspace != null) {
					strScript = workspace + fileSeparator + script;
					if (strScript != null)
						scriptFilePath = new FilePath(new File(strScript));
				}
			}

			if (scriptFilePath != null)
				listener.getLogger()
						.println(MessageUtil.MSG_SCRIPT + MessageUtil.MSG_SPACE + scriptFilePath.getRemote()
								+ MessageUtil.MSG_SPACE + MessageUtil.ON + MessageUtil.MSG_SPACE + user
								+ MessageUtil.SLASH + MessageUtil.HIDDEN_PASSWORD + MessageUtil.AT + instanceStr);
			if (debug)
				listener.getLogger().println(MessageUtil.MSG_DEBUG + MessageUtil.MSG_DEBUG_TEST_SCRIPT
						+ MessageUtil.MSG_COLON + scriptFilePath.getRemote());
			if (!slaveMachine && scriptFilePath != null && !scriptFilePath.exists()) {
				listener.getLogger().println(MessageUtil.LINE);
				listener.getLogger().println(MessageUtil.MSG_WARNING + Messages.SQLPlusRunner_missingScript(scriptFilePath.getRemote()));
				listener.getLogger().println(MessageUtil.LINE);
			} else {
				if (!slaveMachine && scriptFilePath != null && scriptFilePath.exists() && !FileUtil.hasExitCode(scriptFilePath))
					FileUtil.addExitInTheEnd(scriptFilePath);
			}
		}

		listener.getLogger().println(MessageUtil.LINE);

		// running script
		int exitCode = 0;
		try {
			// calculating environment variables
			EnvVars envVars = new EnvVars();
			if (hasCustomNLSLang)
				envVars.put(MessageUtil.ENV_NLS_LANG, customNLSLang);
			if (hasCustomSQLPath)
				envVars.put(MessageUtil.ENV_SQLPATH, customSQLPath);

			envVars.put(MessageUtil.ENV_ORACLE_HOME, selectedOracleHome);
			if (debug)
				listener.getLogger().println(MessageUtil.MSG_DEBUG + MessageUtil.MSG_DEBUG_ENV_ORACLE_HOME
						+ MessageUtil.MSG_EQUALS + selectedOracleHome);
			envVars.put(MessageUtil.ENV_LD_LIBRARY_PATH,
					selectedOracleHome + fileSeparator + MessageUtil.LIB_DIR + File.pathSeparator + selectedOracleHome);
			if (debug)
				listener.getLogger()
						.println(MessageUtil.MSG_DEBUG + MessageUtil.MSG_DEBUG_ENV_LD_LIBRARY_PATH
								+ MessageUtil.MSG_EQUALS + selectedOracleHome + fileSeparator + MessageUtil.LIB_DIR
								+ File.pathSeparator + selectedOracleHome);

			if (hasCustomTNSAdmin && !slaveMachine) {
				envVars.put(MessageUtil.ENV_TNS_ADMIN, customTNSAdmin);
				boolean findTNSNAMES = FileUtil.findFile(MessageUtil.TNSNAMES_ORA, new File(customTNSAdmin));
				if (findTNSNAMES) {
					if (debug)
						listener.getLogger().println(MessageUtil.MSG_DEBUG + MessageUtil.MSG_DEBUG_FOUND_TNSNAMES
								+ MessageUtil.MSG_COLON + new File(customTNSAdmin).getAbsolutePath());
				} else {
					throw new RuntimeException(Messages.SQLPlusRunner_missingTNSNAMES());
				}
			} else if (slaveMachine) {
				if (hasCustomTNSAdmin)
					envVars.put(MessageUtil.ENV_TNS_ADMIN, customTNSAdmin);
				else
					envVars.put(MessageUtil.ENV_TNS_ADMIN, selectedOracleHome);
				if (debug) {
					listener.getLogger().println(MessageUtil.MSG_DEBUG + MessageUtil.MSG_DEBUG_ENV_TNS_ADMIN
							+ MessageUtil.MSG_EQUALS + selectedOracleHome);
				}
			} else {
				boolean findTNSNAMESOracleHome = FileUtil.findFile(MessageUtil.TNSNAMES_ORA,
						new File(selectedOracleHome));
				boolean findTNSNAMESOracleHomeNetworkAdmin = FileUtil.findFile(MessageUtil.TNSNAMES_ORA,
						new File(selectedOracleHome + fileSeparator + MessageUtil.NET_DIR + fileSeparator
								+ MessageUtil.NET_ADM_DIR));
				if (findTNSNAMESOracleHomeNetworkAdmin) {
					envVars.put(MessageUtil.ENV_TNS_ADMIN, selectedOracleHome + fileSeparator + MessageUtil.NET_DIR
							+ fileSeparator + MessageUtil.NET_ADM_DIR);
					if (debug) {
						listener.getLogger()
								.println(MessageUtil.MSG_DEBUG + MessageUtil.MSG_DEBUG_FOUND_TNSNAMES
										+ MessageUtil.MSG_COLON
										+ new File(selectedOracleHome + fileSeparator + MessageUtil.NET_DIR
												+ fileSeparator + MessageUtil.NET_ADM_DIR).getAbsolutePath());
						listener.getLogger()
								.println(MessageUtil.MSG_DEBUG + MessageUtil.MSG_DEBUG_ENV_TNS_ADMIN
										+ MessageUtil.MSG_EQUALS + selectedOracleHome + fileSeparator
										+ MessageUtil.NET_DIR + fileSeparator + MessageUtil.NET_ADM_DIR);
					}
				} else if (findTNSNAMESOracleHome) {
					envVars.put(MessageUtil.ENV_TNS_ADMIN, selectedOracleHome);
					if (debug) {
						listener.getLogger().println(MessageUtil.MSG_DEBUG + MessageUtil.MSG_DEBUG_FOUND_TNSNAMES
								+ MessageUtil.MSG_COLON + new File(selectedOracleHome).getAbsolutePath());
						listener.getLogger().println(MessageUtil.MSG_DEBUG + MessageUtil.MSG_DEBUG_ENV_TNS_ADMIN
								+ MessageUtil.MSG_EQUALS + selectedOracleHome);
					}
				} else {
					throw new RuntimeException(Messages.SQLPlusRunner_missingTNSNAMES());
				}
			}

			// create command arguments
			ArgumentListBuilder args = new ArgumentListBuilder();

			String argUserPasswordInstance = user + MessageUtil.SLASH + MessageUtil.DOUBLE_QUOTES + password
					+ MessageUtil.DOUBLE_QUOTES;

			if (instance != null && instance.trim().length() > 0) {
				argUserPasswordInstance = argUserPasswordInstance + MessageUtil.AT + instance.trim();
			}

			String argSQLscript = scriptFilePath.getRemote();

			if (debug)
				listener.getLogger().println(MessageUtil.MSG_DEBUG_WORK_DIR + MessageUtil.MSG_EQUALS + workspace);

			if (hasCustomSQLPlusHome) {
				args.add(customSQLPlusHome);
			} else {

				listener.getLogger().println(MessageUtil.MSG_DEBUG_EXEC_FILE + MessageUtil.MSG_EQUALS + sqlplus);

				boolean findSQLPlusOnOracleHomeBin = FileUtil.findFile(sqlplus,
						new File(selectedOracleHome + fileSeparator + MessageUtil.BIN_DIR));

				boolean findSQLPlusOnOracleHome = FileUtil.findFile(sqlplus, new File(selectedOracleHome));

				if (findSQLPlusOnOracleHomeBin) {
					if (debug)
						listener.getLogger().println(MessageUtil.MSG_DEBUG + MessageUtil.FOUND_SQL_PLUS_ON
								+ new File(selectedOracleHome + fileSeparator + MessageUtil.BIN_DIR).getAbsolutePath());
					args.add(selectedOracleHome + fileSeparator + MessageUtil.BIN_DIR + fileSeparator + sqlplus);
				} else if (findSQLPlusOnOracleHome) {
					if (debug)
						listener.getLogger().println(MessageUtil.MSG_DEBUG + MessageUtil.FOUND_SQL_PLUS_ON
								+ new File(selectedOracleHome).getAbsolutePath());
					args.add(selectedOracleHome + fileSeparator + sqlplus);
				} else if (slaveMachine) {
					listener.getLogger().println(MessageUtil.MSG_DEBUG_EXEC_DIR + MessageUtil.MSG_COLON
							+ selectedOracleHome + fileSeparator + MessageUtil.BIN_DIR);
					args.add(selectedOracleHome + fileSeparator + MessageUtil.BIN_DIR + fileSeparator + sqlplus);
				} else {
					throw new RuntimeException(Messages.SQLPlusRunner_missingSQLPlus());
				}
			}

			args.add(MessageUtil.SQLPLUS_TRY_LOGIN_JUST_ONCE);
			args.addMasked(argUserPasswordInstance);
			args.add(MessageUtil.AT + argSQLscript);

			// launch SQL*Plus with arguments
			exitCode = launcher.launch().cmds(args).envs(build.getEnvironment(listener).overrideAll(envVars))
					.stdout(listener).pwd(workspace).join();

			listener.getLogger().printf(Messages.SQLPlusRunner_processEnd() + " %d%n", exitCode);

		} catch (RuntimeException e) {
			throw e;
		} catch (Exception e) {
			listener.getLogger().println(MessageUtil.MSG_ERROR + e.getMessage());
			throw new RuntimeException(e);
		} finally {
			if (tempScript != null) {
				try {
					boolean removed = tempScript.delete();
					if (!removed)
						listener.getLogger().printf(Messages.SQLPlusRunner_tempFileNotRemoved());
				} catch (Exception e) {
					listener.getLogger().println(MessageUtil.MSG_ERROR + e.getMessage());
				}
			}
		}

		if (exitCode != PROCESS_EXIT_CODE_SUCCESSFUL) {
			listener.getLogger().println(MessageUtil.LINE);
			listener.getLogger().println(MessageUtil.MSG_EXIT_CODE + MessageUtil.MSG_COLON + exitCode);
			listener.getLogger().println(MessageUtil.LINE);
			throw new RuntimeException(Messages.SQLPlusRunner_processErrorEnd());
		}

		listener.getLogger().println(MessageUtil.LINE);
	}

}
