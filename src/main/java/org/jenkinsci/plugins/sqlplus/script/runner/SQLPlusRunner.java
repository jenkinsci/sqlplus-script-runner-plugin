package org.jenkinsci.plugins.sqlplus.script.runner;

import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Objects;

import hudson.EnvVars;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.util.ArgumentListBuilder;

/**
 * Run SQLPlus commands on the agent, or master of Jenkins.
 */
public class SQLPlusRunner implements Serializable {

	private static final long serialVersionUID = -2426963507463371935L;

	private static final int PROCESS_EXIT_CODE_SUCCESSFUL = 0;

	private static final String LOGON_AS_SYSDBA = "AS  SYSDBA";

	public SQLPlusRunner(Run<?, ?> build, TaskListener listener, Launcher launcher, FilePath workspace,
			boolean isHideSQLPlusVersion, String user, String password, boolean isSysdba, String instance, String script,
			String globalOracleHome, String globalSQLPlusHome, String globalTNSAdmin, String globalNLSLang, String globalSQLPath,  String scriptType,
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
		this.isSysdba= isSysdba;
		this.script = script;
		this.globalOracleHome = globalOracleHome;
		this.globalSQLPlusHome = globalSQLPlusHome;
		this.globalTNSAdmin = globalTNSAdmin;
		this.globalNLSLang = globalNLSLang;
		this.globalSQLPath = globalSQLPath;
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
	
	private final boolean isSysdba;

	private final String script;

	private final String globalOracleHome;

	private final String globalSQLPlusHome;

	private final String globalTNSAdmin;

	private final String globalNLSLang;

	private final String globalSQLPath;

	private final String customOracleHome;

	private String customSQLPlusHome;

	private String customTNSAdmin;

	private String customNLSLang;

	private String customSQLPath;

	private final String scriptType;

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
	public void runGetSQLPLusVersion(String customSQLPlusHome, String oracleHome, TaskListener listener,Launcher launcher) {

		if (Objects. isNull(oracleHome ) || oracleHome.isEmpty()) {
			throw new RuntimeException(MessageUtil.MSG_ORACLE_HOME_MISSING);
		}

		boolean agentMachine = EnvUtil.isAgentMachine(launcher);
		if (debug) {
			log(MessageUtil.MSG_DEBUG_DETECTED_HOST + MessageUtil.MSG_EQUALS + NetUtil.getHostName());
			log(MessageUtil.MSG_DEBUG_AGENT_MACHINE + MessageUtil.MSG_COLON + agentMachine);
		}

		boolean hasCustomSQLPlusHome = customSQLPlusHome != null && customSQLPlusHome.length() > 0;

		if (!agentMachine && !hasCustomSQLPlusHome) {
			File directoryAccessTest = new File(oracleHome);
			if (!directoryAccessTest.exists()) {
				throw new RuntimeException(Messages.SQLPlusRunner_wrongOracleHome(oracleHome));
			}
		}

		line();
		log(MessageUtil.MSG_ORACLE_HOME + oracleHome);
		line();
		log(MessageUtil.MSG_GET_SQL_PLUS_VERSION);
		try {
			String sqlplus = MessageUtil.SQLPLUS;
			String fileSeparator = File.separator;
			if (EnvUtil.isWindowsOS(agentMachine, listener, build)) {
				sqlplus = MessageUtil.SQLPLUS_FOR_WINDOWS;
				fileSeparator = MessageUtil.WINDOWS_FILE_SEPARATOR;
			}

			EnvVars envVars = new EnvVars();
			envVars.put(MessageUtil.ENV_ORACLE_HOME, oracleHome);
			logDebug(MessageUtil.MSG_DEBUG + MessageUtil.MSG_DEBUG_ENV_ORACLE_HOME+ MessageUtil.MSG_EQUALS + oracleHome);
			envVars.put(MessageUtil.ENV_LD_LIBRARY_PATH, oracleHome + fileSeparator + MessageUtil.LIB_DIR);
			logDebug(MessageUtil.MSG_DEBUG + MessageUtil.MSG_DEBUG_ENV_LD_LIBRARY_PATH+ MessageUtil.MSG_EQUALS + oracleHome + fileSeparator + MessageUtil.LIB_DIR);

			// create command arguments
			ArgumentListBuilder args = new ArgumentListBuilder();

			logDebug(MessageUtil.MSG_DEBUG_EXEC_FILE + MessageUtil.MSG_EQUALS + sqlplus);

			if (hasCustomSQLPlusHome) {
				args.add(customSQLPlusHome);
			} else {
				boolean findSQLPlusOnOracleHomeBin = FileUtil.findFile(sqlplus,new File(oracleHome + fileSeparator + MessageUtil.BIN_DIR));
				boolean findSQLPlusOnOracleHome = FileUtil.findFile(sqlplus, new File(oracleHome));
				if (findSQLPlusOnOracleHomeBin) {
					log(MessageUtil.FOUND_SQL_PLUS_ON	+ new File(oracleHome + fileSeparator + MessageUtil.BIN_DIR).getAbsolutePath());
					args.add(oracleHome + fileSeparator + MessageUtil.BIN_DIR + fileSeparator + sqlplus);
				} else if (findSQLPlusOnOracleHome) {
					log(MessageUtil.FOUND_SQL_PLUS_ON + new File(oracleHome).getAbsolutePath());
					args.add(oracleHome + fileSeparator + sqlplus);
				} else if (agentMachine) {
					log(MessageUtil.MSG_DEBUG_EXEC_DIR + MessageUtil.MSG_COLON + oracleHome+ fileSeparator + MessageUtil.BIN_DIR);
					args.add(oracleHome + fileSeparator + MessageUtil.BIN_DIR + fileSeparator + sqlplus);
				} else {
					throw new RuntimeException(Messages.SQLPlusRunner_missingSQLPlus());
				}
			}

			args.add(MessageUtil.SQLPLUS_VERSION);

			if (debug) {
				line();
				log(MessageUtil.MSG_DEBUG_STATEMENT + MessageUtil.MSG_COLON);
				for (String a : args.toList()) {
					listener.getLogger().print(a + MessageUtil.MSG_SPACE);
				}
				log(MessageUtil.MSG_SPACE);
				line();
			}

			int exitCode;
			exitCode = launcher.launch().cmds(args).envs(build.getEnvironment(listener).overrideAll(envVars)).stdout(listener).pwd(workspace).join();
			listener.getLogger().printf(Messages.SQLPlusRunner_processEnd() + " %d%n", exitCode);

		} catch (RuntimeException e) {
			log(MessageUtil.MSG_ERROR + e.getMessage());
			throw e;
		} catch (Exception e) {
			log(MessageUtil.MSG_ERROR + e.getMessage());
			throw new RuntimeException(e);
		}
		line();
	}

	/**
	 * Main process to run SQLPlus
	 */
	public void run() throws IOException, InterruptedException {

		String selectedOracleHome;
		String detectedOracleHome;
		boolean agentMachine = EnvUtil.isAgentMachine(launcher);

		line();
		log(MessageUtil.MSG_GET_ORACLE_HOME);

		// custom SQLPLUS_HOME overrides file location
		if (Objects.nonNull(customSQLPlusHome ) && !customSQLPlusHome.isEmpty()) {
			log(MessageUtil.MSG_CUSTOM_SQLPLUS_HOME);
			log(MessageUtil.MSG_DEBUG_EXEC_DIR + MessageUtil.MSG_COLON + customSQLPlusHome);
		} else if ( Objects.nonNull(globalSQLPlusHome ) && !globalSQLPlusHome.isEmpty() ) {
			logDebug(MessageUtil.MSG_DEBUG + MessageUtil.MSG_GLOBAL_SQLPLUS_HOME_SELECTED);
			line();
			log(MessageUtil.MSG_GLOBAL_SQLPLUS_HOME);
			customSQLPlusHome = globalSQLPlusHome;
		}

		// custom TNS_ADMIN
		boolean hasCustomTNSAdmin = false;
		if (Objects.nonNull(customTNSAdmin ) && !customTNSAdmin.isEmpty()) {
			log(MessageUtil.MSG_CUSTOM_TNS_ADMIN);
			log(MessageUtil.MSG_DEBUG_ENV_TNS_ADMIN + MessageUtil.MSG_COLON + customTNSAdmin);
			hasCustomTNSAdmin = true;
		} else if (Objects.nonNull(globalTNSAdmin ) && globalTNSAdmin.isEmpty()) {
			logDebug(MessageUtil.MSG_DEBUG + MessageUtil.MSG_GLOBAL_TNS_ADMIN_SELECTED);
			line();
			log(MessageUtil.MSG_GLOBAL_TNS_ADMIN);
			customTNSAdmin = globalTNSAdmin;
			hasCustomTNSAdmin = true;
			log(MessageUtil.MSG_DEBUG_ENV_TNS_ADMIN + MessageUtil.MSG_COLON + customTNSAdmin);
		}

		// custom NLS_LANG
		boolean hasCustomNLSLang = false;
		if (Objects.nonNull(customNLSLang ) && customNLSLang.isEmpty()) {
			log(MessageUtil.MSG_CUSTOM_NLS_LANG);
			log(MessageUtil.MSG_DEBUG_ENV_NLS_LANG + MessageUtil.MSG_COLON + customNLSLang);
			hasCustomNLSLang = true;
		} else if (Objects.nonNull(globalNLSLang ) && !globalNLSLang.isEmpty()) {
			logDebug(MessageUtil.MSG_DEBUG + MessageUtil.MSG_GLOBAL_NLS_LANG_SELECTED);
			line();
			log(MessageUtil.MSG_GLOBAL_NLS_LANG);
			customNLSLang = globalNLSLang;
			hasCustomNLSLang = true;
			log(MessageUtil.MSG_DEBUG_ENV_NLS_LANG + MessageUtil.MSG_COLON + customNLSLang);
		}

		// custom SQLPATH
		boolean hasCustomSQLPath = false;
		if (Objects.nonNull(customSQLPath) && !customSQLPath.isEmpty()) {
			log(MessageUtil.MSG_CUSTOM_SQLPATH);
			log(MessageUtil.MSG_DEBUG_ENV_SQLPATH + MessageUtil.MSG_COLON + customSQLPath);
			hasCustomSQLPath = true;
		} else if (Objects.nonNull(globalSQLPath ) && !globalSQLPath.isEmpty()) {
			logDebug(MessageUtil.MSG_DEBUG + MessageUtil.MSG_GLOBAL_SQLPATH_SELECTED);
			line();
			log(MessageUtil.MSG_GLOBAL_SQLPATH);
			customSQLPath = globalSQLPath;
			hasCustomSQLPath = true;
			log(MessageUtil.MSG_DEBUG_ENV_SQLPATH + MessageUtil.MSG_COLON + customSQLPath);
		}

		// custom ORACLE_HOME overrides everything
		detectedOracleHome = build.getEnvironment(listener).get(MessageUtil.ENV_ORACLE_HOME);
		if (Objects.nonNull(customOracleHome )&& !customOracleHome.isEmpty()) {
			logDebug(MessageUtil.MSG_DEBUG + MessageUtil.MSG_CUSTOM_ORACLE_HOME);
			line();
			log(MessageUtil.MSG_CUSTOM_ORACLE_HOME);
			selectedOracleHome = customOracleHome;
			// global ORACLE_HOME comes next
		} else if (Objects.nonNull(globalOracleHome ) && !globalOracleHome.isEmpty()) {
			logDebug(MessageUtil.MSG_DEBUG + MessageUtil.MSG_GLOBAL_ORACLE_HOME_SELECTED);
			line();
			log(MessageUtil.MSG_GLOBAL_ORACLE_HOME);
			selectedOracleHome = globalOracleHome;
			// now try to detect ORACLE_HOME
		} else if (tryToDetectOracleHome && Objects.nonNull(detectedOracleHome)) {
			logDebug(MessageUtil.MSG_DEBUG + MessageUtil.MSG_TRY_DETECTED_ORACLE_HOME);
			line();
			log(MessageUtil.MSG_USING_DETECTED_ORACLE_HOME);
			selectedOracleHome = detectedOracleHome;
		} else {
			// nothing works, get global ORACLE_HOME
			logDebug(MessageUtil.MSG_DEBUG + MessageUtil.MSG_GLOBAL_ORACLE_HOME_SELECTED_ANYWAY);
			selectedOracleHome = globalOracleHome;
		}

		if (!isHideSQLPlusVersion) {
			runGetSQLPLusVersion(customSQLPlusHome, selectedOracleHome, listener, launcher);
		}

		logDebug(MessageUtil.MSG_DEBUG_DETECTED_HOST + MessageUtil.MSG_EQUALS + NetUtil.getHostName());

		// can't find Oracle Home!
		if (Objects. isNull(selectedOracleHome) || selectedOracleHome.isEmpty()) {
			throw new RuntimeException(MessageUtil.MSG_ORACLE_HOME_MISSING);
		}

		// finding SQL*Plus
		boolean hasCustomSQLPlusHome = Objects.nonNull(customSQLPlusHome) && !customSQLPlusHome.isEmpty();

		if (!agentMachine && !hasCustomSQLPlusHome) {
			File directoryAccessTest = new File(selectedOracleHome);
			logDebug(MessageUtil.MSG_DEBUG + MessageUtil.MSG_DEBUG_TEST_DIR+ MessageUtil.MSG_COLON + directoryAccessTest.getAbsolutePath());
			if (!directoryAccessTest.exists()) {
				throw new RuntimeException(Messages.SQLPlusRunner_wrongOracleHome(selectedOracleHome));
			}
		}

		// validating SQL script name
		if (Objects. isNull(script) || script.isEmpty()) {
			line();
			log(MessageUtil.MSG_WARNING + Messages.SQLPlusRunner_missingScript(workspace));
			line();
		}

		String instanceStr = MessageUtil.LOCAL_DATABASE_MSG;
		if (Objects.nonNull(instance )) {
			instanceStr = instance;
		}

		line();
		log(MessageUtil.MSG_ORACLE_HOME + selectedOracleHome);
		line();

		String sqlplus = MessageUtil.SQLPLUS;
		String fileSeparator = File.separator;
		if (EnvUtil.isWindowsOS(agentMachine, listener, build)) {
			sqlplus = MessageUtil.SQLPLUS_FOR_WINDOWS;
			fileSeparator = MessageUtil.WINDOWS_FILE_SEPARATOR;
		}

		FilePath tempScript = null;
		FilePath scriptFilePath = null;
		// user defined SQL
		if (ScriptType.userDefined.name().equals(scriptType)) {
			log(MessageUtil.MSG_DEFINED_SCRIPT + MessageUtil.MSG_SPACE + user+ MessageUtil.SLASH + MessageUtil.HIDDEN_PASSWORD + MessageUtil.AT + instanceStr);
			scriptFilePath = FileUtil.createTempScript(build, workspace, script, agentMachine);
			tempScript = scriptFilePath;
			log(MessageUtil.MSG_TEMP_SCRIPT + MessageUtil.MSG_SPACE + scriptFilePath.absolutize().toURI());
		} else {
			// file script
			String strScript = null;
			if (agentMachine) {
				if (hasCustomSQLPath) {
   				  scriptFilePath = new FilePath(new File(customSQLPath + fileSeparator + script));
				} else  {
				  scriptFilePath = new FilePath(new File(workspace.getRemote() + fileSeparator + script));
				}
			} else {
				if (hasCustomSQLPath) {
					strScript = customSQLPath + fileSeparator + script;
				} else if (Objects.nonNull(workspace )) {
					strScript = workspace + fileSeparator + script;
				}
				scriptFilePath = new FilePath(new File(strScript));
			}

			if (Objects.nonNull(scriptFilePath))
				log(MessageUtil.MSG_SCRIPT + MessageUtil.MSG_SPACE + scriptFilePath.getRemote()+ MessageUtil.MSG_SPACE + MessageUtil.ON + MessageUtil.MSG_SPACE + user+ MessageUtil.SLASH + MessageUtil.HIDDEN_PASSWORD + MessageUtil.AT + instanceStr);

			logDebug(MessageUtil.MSG_DEBUG + MessageUtil.MSG_DEBUG_TEST_SCRIPT+ MessageUtil.MSG_COLON + Objects.requireNonNull(scriptFilePath).getRemote());
			if (!agentMachine && !scriptFilePath.exists()) {
				line();
				log(MessageUtil.MSG_WARNING + Messages.SQLPlusRunner_missingScript(scriptFilePath.getRemote()));
				line();
			} else {
				if (!agentMachine && scriptFilePath.exists() && !FileUtil.hasExitCode(scriptFilePath))
					FileUtil.addExitInTheEnd(scriptFilePath);
			}
		}

		line();

		// running script
		int exitCode;
		try {
			// calculating environment variables
			EnvVars envVars = new EnvVars();
			if (hasCustomNLSLang)
				envVars.put(MessageUtil.ENV_NLS_LANG, customNLSLang);
			if (hasCustomSQLPath)
				envVars.put(MessageUtil.ENV_SQLPATH, customSQLPath);

			envVars.put(MessageUtil.ENV_ORACLE_HOME, selectedOracleHome);
			logDebug(MessageUtil.MSG_DEBUG + MessageUtil.MSG_DEBUG_ENV_ORACLE_HOME+ MessageUtil.MSG_EQUALS + selectedOracleHome);
			envVars.put(MessageUtil.ENV_LD_LIBRARY_PATH,	selectedOracleHome + fileSeparator + MessageUtil.LIB_DIR + File.pathSeparator + selectedOracleHome);
			logDebug(MessageUtil.MSG_DEBUG + MessageUtil.MSG_DEBUG_ENV_LD_LIBRARY_PATH+ MessageUtil.MSG_EQUALS + selectedOracleHome + fileSeparator + MessageUtil.LIB_DIR+ File.pathSeparator + selectedOracleHome);

			if (hasCustomTNSAdmin && !agentMachine) {
				envVars.put(MessageUtil.ENV_TNS_ADMIN, customTNSAdmin);
				boolean findTNSNAMES = FileUtil.findFile(MessageUtil.TNSNAMES_ORA, new File(customTNSAdmin));
				if (findTNSNAMES) {
					logDebug(MessageUtil.MSG_DEBUG + MessageUtil.MSG_DEBUG_FOUND_TNSNAMES+ MessageUtil.MSG_COLON + new File(customTNSAdmin).getAbsolutePath());
				} else {
					throw new RuntimeException(Messages.SQLPlusRunner_missingTNSNAMES());
				}
			} else if (agentMachine) {
				if (hasCustomTNSAdmin)
					envVars.put(MessageUtil.ENV_TNS_ADMIN, customTNSAdmin);
				else
					envVars.put(MessageUtil.ENV_TNS_ADMIN, selectedOracleHome);
				logDebug(MessageUtil.MSG_DEBUG + MessageUtil.MSG_DEBUG_ENV_TNS_ADMIN	+ MessageUtil.MSG_EQUALS + selectedOracleHome);

			} else {
				boolean findTNSNAMESOracleHome = FileUtil.findFile(MessageUtil.TNSNAMES_ORA,new File(selectedOracleHome));
				final var tnsnames = new File(selectedOracleHome + fileSeparator + MessageUtil.NET_DIR + fileSeparator+ MessageUtil.NET_ADM_DIR);
				boolean findTNSNAMESOracleHomeNetworkAdmin = FileUtil.findFile(MessageUtil.TNSNAMES_ORA,	tnsnames);
				if (findTNSNAMESOracleHomeNetworkAdmin) {
					envVars.put(MessageUtil.ENV_TNS_ADMIN, selectedOracleHome + fileSeparator + MessageUtil.NET_DIR+ fileSeparator + MessageUtil.NET_ADM_DIR);
					logDebug(MessageUtil.MSG_DEBUG + MessageUtil.MSG_DEBUG_FOUND_TNSNAMES+ MessageUtil.MSG_COLON+ tnsnames.getAbsolutePath());
					logDebug(MessageUtil.MSG_DEBUG + MessageUtil.MSG_DEBUG_ENV_TNS_ADMIN+ MessageUtil.MSG_EQUALS + selectedOracleHome + fileSeparator+ MessageUtil.NET_DIR + fileSeparator + MessageUtil.NET_ADM_DIR);
				} else if (findTNSNAMESOracleHome) {
					envVars.put(MessageUtil.ENV_TNS_ADMIN, selectedOracleHome);
					logDebug(MessageUtil.MSG_DEBUG + MessageUtil.MSG_DEBUG_FOUND_TNSNAMES+ MessageUtil.MSG_COLON + new File(selectedOracleHome).getAbsolutePath());
					logDebug(MessageUtil.MSG_DEBUG + MessageUtil.MSG_DEBUG_ENV_TNS_ADMIN	+ MessageUtil.MSG_EQUALS + selectedOracleHome);
				} else {
					throw new RuntimeException(Messages.SQLPlusRunner_missingTNSNAMES());
				}
			}

			// create command arguments
			ArgumentListBuilder args = new ArgumentListBuilder();
			String argUserPasswordInstance = user + MessageUtil.SLASH + MessageUtil.DOUBLE_QUOTES + password+ MessageUtil.DOUBLE_QUOTES;

			if (Objects.nonNull(instance) && !instance.trim().isEmpty()) {
				argUserPasswordInstance = argUserPasswordInstance + MessageUtil.AT + instance.trim();
			}

			String argSQLscript = scriptFilePath.getRemote();

			logDebug(MessageUtil.MSG_DEBUG_WORK_DIR + MessageUtil.MSG_EQUALS + workspace);

			if (hasCustomSQLPlusHome) {
				args.add(customSQLPlusHome);
			} else {

				log(MessageUtil.MSG_DEBUG_EXEC_FILE + MessageUtil.MSG_EQUALS + sqlplus);

				boolean findSQLPlusOnOracleHomeBin = FileUtil.findFile(sqlplus,new File(selectedOracleHome + fileSeparator + MessageUtil.BIN_DIR));
				boolean findSQLPlusOnOracleHome = FileUtil.findFile(sqlplus, new File(selectedOracleHome));

				if (findSQLPlusOnOracleHomeBin) {
					logDebug(MessageUtil.MSG_DEBUG + MessageUtil.FOUND_SQL_PLUS_ON+ new File(selectedOracleHome + fileSeparator + MessageUtil.BIN_DIR).getAbsolutePath());
					args.add(selectedOracleHome + fileSeparator + MessageUtil.BIN_DIR + fileSeparator + sqlplus);
				} else if (findSQLPlusOnOracleHome) {
					logDebug(MessageUtil.MSG_DEBUG + MessageUtil.FOUND_SQL_PLUS_ON+ new File(selectedOracleHome).getAbsolutePath());
					args.add(selectedOracleHome + fileSeparator + sqlplus);
				} else if (agentMachine) {
					log(MessageUtil.MSG_DEBUG_EXEC_DIR + MessageUtil.MSG_COLON	+ selectedOracleHome + fileSeparator + MessageUtil.BIN_DIR);
					args.add(selectedOracleHome + fileSeparator + MessageUtil.BIN_DIR + fileSeparator + sqlplus);
				} else {
					throw new RuntimeException(Messages.SQLPlusRunner_missingSQLPlus());
				}
			}

			args.add(MessageUtil.SQLPLUS_TRY_LOGIN_JUST_ONCE);
			args.addMasked(argUserPasswordInstance);
			if (isSysdba) {
				args.add(LOGON_AS_SYSDBA);	
			}
			args.add(MessageUtil.AT + argSQLscript);

			// launch SQL*Plus with arguments
			exitCode = launcher.launch().cmds(args).envs(build.getEnvironment(listener).overrideAll(envVars)).stdout(listener).pwd(workspace).join();

			listener.getLogger().printf(Messages.SQLPlusRunner_processEnd() + " %d%n", exitCode);

		} catch (RuntimeException e) {
			throw e;
		} catch (Exception e) {
			log(MessageUtil.MSG_ERROR + e.getMessage());
			throw new RuntimeException(e);
		} finally {
			if (tempScript != null) {
				try {
					boolean removed = tempScript.delete();
					if (!removed)
						listener.getLogger().printf(Messages.SQLPlusRunner_tempFileNotRemoved());
				} catch (Exception e) {
					log(MessageUtil.MSG_ERROR + e.getMessage());
				}
			}
		}

		if (exitCode != PROCESS_EXIT_CODE_SUCCESSFUL) {
			line();
			log(MessageUtil.MSG_EXIT_CODE + MessageUtil.MSG_COLON + exitCode);
			line();
			throw new RuntimeException(Messages.SQLPlusRunner_processErrorEnd());
		}

		line();
	}

	private void writeObject(ObjectOutputStream stream) throws IOException {
	    stream.defaultWriteObject();
	}

	private void readObject(ObjectInputStream stream) throws IOException, ClassNotFoundException {
	    stream.defaultReadObject();
	}
	private void log(String message) {
			listener.getLogger().println(message);
	}
	private void logDebug(String message) {
		if (debug)
			log(message);
	}

	private void line() {
		log(MessageUtil.LINE);
	}

}