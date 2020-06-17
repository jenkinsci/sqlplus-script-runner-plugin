package org.jenkinsci.plugins.sqlplus.script.runner.test;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Writer;
import java.util.Map;

import org.jenkinsci.plugins.sqlplus.script.runner.ScriptType;

public class ExternalProgramUtil {

	private static final String EOL = "\n";
	private static final String LIB_DIR = "lib";
	private static final String BIN_DIR = "bin";
	private static final String NET_DIR = "network" + File.separator + "admin";
	private static final String ENV_LD_LIBRARY_PATH = "LD_LIBRARY_PATH";
	private static final String ENV_ORACLE_HOME = "ORACLE_HOME";
	private static final String ENV_TNS_ADMIN = "TNS_ADMIN";

	private static final String SQLPLUS = "sqlplus";
	private static final String SQLPLUS_EXIT = EOL + "exit;" + EOL + "exit;";
	private static final String SQLPLUS_TRY_LOGIN_JUST_ONCE = "-L";
	private static final String SQLPLUS_VERSION = "-v";

	private static final String AT = "@";
	private static final String SLASH = "/";

	public static String run(String user,String password,String instance,String script,String sqlPath,String oracleHome,String scriptType) throws IOException,InterruptedException {

		String sqlplusOutput = "";
		String arg1 = user + SLASH + password;

		if (instance != null) {
			arg1 = arg1 + AT + instance;
		}
		String arg2 = script;
		if (ScriptType.userDefined.name().equals(scriptType)) {
			addExitOnFile(arg2);
		} else {
			addExitOnFile(sqlPath + File.separator + arg2);
		}

		System.out.println(arg2);
		String line;

		ProcessBuilder pb = new ProcessBuilder(oracleHome + File.separator + BIN_DIR + File.separator + SQLPLUS,SQLPLUS_TRY_LOGIN_JUST_ONCE,arg1,AT + arg2);

		Map<String,String> env = pb.environment();
		env.put(ENV_ORACLE_HOME,oracleHome);
		env.put(ENV_LD_LIBRARY_PATH,oracleHome + File.separator + LIB_DIR);
		env.put(ENV_TNS_ADMIN,oracleHome + File.separator + NET_DIR);

		pb.directory(new File(sqlPath));
		pb.redirectErrorStream(true);

		Process p = pb.start();
		p.waitFor();

		BufferedReader bri = new BufferedReader(new InputStreamReader(p.getInputStream()));

		while ( (line = bri.readLine()) != null) {
			sqlplusOutput += line + EOL;
		}

		return sqlplusOutput;
	}

	public static String getVersion(String sqlPath,String oracleHome) throws IOException,InterruptedException {

		String sqlplusOutput = "";
		String line;

		ProcessBuilder pb = new ProcessBuilder(oracleHome + File.separator + BIN_DIR + File.separator + SQLPLUS,SQLPLUS_VERSION);

		Map<String,String> env = pb.environment();
		env.put(ENV_ORACLE_HOME,oracleHome);
		env.put(ENV_LD_LIBRARY_PATH,oracleHome + File.separator + LIB_DIR);

		pb.directory(new File(sqlPath));
		pb.redirectErrorStream(true);

		Process p = pb.start();
		p.waitFor();

		BufferedReader bri = new BufferedReader(new InputStreamReader(p.getInputStream()));

		while ( (line = bri.readLine()) != null) {
			sqlplusOutput += line + EOL;
		}

		return sqlplusOutput;
	}

	private static void addExitOnFile(String filePath) {

		Writer output = null;
		try {
			output = new BufferedWriter(new FileWriter(filePath,true));
			output.append(SQLPLUS_EXIT);
			output.close();
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (output != null) {
				try {
					output.close();
				} catch (IOException e) {

				}
			}
		}
	}

}