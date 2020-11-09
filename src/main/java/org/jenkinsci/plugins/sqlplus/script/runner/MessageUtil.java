package org.jenkinsci.plugins.sqlplus.script.runner;

public class MessageUtil {

	// console messages
	public static final String MSG_TEMP_SCRIPT = Messages.SQLPlusRunner_tempScript();
	public static final String ON = Messages.SQLPlusRunner_on();
	public static final String FOUND_SQL_PLUS_ON = "found SQL*Plus on ";
	public static final String WINDOWS_FILE_SEPARATOR = "\\";	
	public static final String MSG_ORACLE_HOME = Messages.SQLPlusRunner_usingOracleHome();
	public static final String MSG_SCRIPT = Messages.SQLPlusRunner_runningScript();
	public static final String MSG_DEFINED_SCRIPT = Messages.SQLPlusRunner_runningDefinedScript();
	public static final String MSG_GET_ORACLE_HOME = Messages.SQLPlusRunner_gettingOracleHome();
	public static final String MSG_CUSTOM_ORACLE_HOME = Messages.SQLPlusRunner_usingCustomOracleHome();
	public static final String MSG_CUSTOM_SQLPLUS_HOME = Messages.SQLPlusRunner_usingCustomSQLPlusHome();
	public static final String MSG_CUSTOM_TNS_ADMIN = Messages.SQLPlusRunner_usingCustomTNSAdmin();
	public static final String MSG_GLOBAL_ORACLE_HOME = Messages.SQLPlusRunner_usingGlobalOracleHome();
	public static final String MSG_GLOBAL_SQLPLUS_HOME = Messages.SQLPlusRunner_usingGlobalSQLPlusHome();
	public static final String MSG_GLOBAL_TNS_ADMIN = Messages.SQLPlusRunner_usingGlobalTNSAdmin();
	public static final String MSG_USING_DETECTED_ORACLE_HOME = Messages.SQLPlusRunner_usingDetectedOracleHome();
	public static final String MSG_GLOBAL_ORACLE_HOME_SELECTED = Messages.SQLPlusRunner_globalOracleHomeSelected();
	public static final String MSG_GLOBAL_SQLPLUS_HOME_SELECTED = Messages.SQLPlusRunner_globalSQLPlusHomeSelected();
	public static final String MSG_GLOBAL_TNS_ADMIN_SELECTED = Messages.SQLPlusRunner_globalTNSAdminSelected();
	public static final String MSG_ERROR = Messages.SQLPlusRunner_error();
	public static final String MSG_GET_SQL_PLUS_VERSION = Messages.SQLPlusRunner_gettingSQLPlusVersion();
	public static final String MSG_ORACLE_HOME_MISSING = Messages.SQLPlusRunner_missingOracleHome();
	public static final String MSG_TRY_DETECTED_ORACLE_HOME = Messages.SQLPlusRunner_tryToDetectOracleHome();
	public static final String MSG_GLOBAL_ORACLE_HOME_SELECTED_ANYWAY = Messages.SQLPlusRunner_globalOracleHomeSelectedAnyway();
	public static final String MSG_DEBUG = Messages.SQLPlusRunner_debugMsg();	        
	public static final String MSG_DEBUG_DETECTED_HOST = Messages.SQLPlusRunner_debugDetectedHost(); 
	public static final String MSG_DEBUG_SLAVE_MACHINE = Messages.SQLPlusRunner_debugSlaveMachine();
	public static final String MSG_DEBUG_EXEC_FILE = Messages.SQLPlusRunner_debugExecFile(); 
	public static final String MSG_DEBUG_EXEC_DIR = Messages.SQLPlusRunner_debugExecDir();
	public static final String MSG_DEBUG_TEST_DIR = Messages.SQLPlusRunner_debugTestDir();
	public static final String MSG_DEBUG_TEST_SCRIPT = Messages.SQLPlusRunner_debugTestScript();
	public static final String MSG_DEBUG_WORK_DIR = Messages.SQLPlusRunner_debugWorkDir(); 
	public static final String MSG_DEBUG_STATEMENT = Messages.SQLPlusRunner_debugStatement();
	public static final String MSG_DEBUG_ENV_ORACLE_HOME = Messages.SQLPlusRunner_debugEnvOracleHome();
	public static final String MSG_DEBUG_ENV_LD_LIBRARY_PATH= Messages.SQLPlusRunner_debugEnvLDLibraryPath(); 
	public static final String MSG_DEBUG_ENV_TNS_ADMIN = Messages.SQLPlusRunner_debugEnvTNSAdmin();
	public static final String MSG_DEBUG_FOUND_TNSNAMES = Messages.SQLPlusRunner_debugFoundTnsNames();  
	public static final String MSG_EXIT_CODE = Messages.SQLPlusRunner_exitCode(); 
	public static final String MSG_EQUALS = " = ";
	public static final String MSG_SPACE = " ";
	public static final String MSG_COLON = ": ";
	public static final String LOCAL_DATABASE_MSG = "local";
	public static final String HIDDEN_PASSWORD = "********";
	public static final String LINE = Messages.SQLPlusRunner_line();

	// for executing commands
	public static final String AT = "@";
	public static final String SLASH = "/";
	public static final String DOUBLE_QUOTES = "\"";

	// for variables
	public static final String ENV_LD_LIBRARY_PATH = "LD_LIBRARY_PATH";
	public static final String ENV_ORACLE_HOME = "ORACLE_HOME";
	public static final String ENV_TNS_ADMIN = "TNS_ADMIN";

	// for SQL*Plus
	public static final String SQLPLUS_TRY_LOGIN_JUST_ONCE = "-L";
	public static final String SQLPLUS_VERSION = "-v";
	public static final String SQLPLUS = "sqlplus";
	public static final String SQLPLUS_FOR_WINDOWS = "sqlplus.exe";
	public static final String LIB_DIR = "lib";
	public static final String BIN_DIR = "bin";
	public static final String NET_DIR = "network";
	public static final String NET_ADM_DIR = "admin";
	public static final String TNSNAMES_ORA = "tnsnames.ora";

}
