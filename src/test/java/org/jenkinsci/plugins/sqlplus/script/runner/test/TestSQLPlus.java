package org.jenkinsci.plugins.sqlplus.script.runner.test;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;

import org.jenkinsci.plugins.sqlplus.script.runner.ScriptType;
import org.junit.Ignore;
import org.junit.Test;

@Ignore
/*
 * In order to run this test Oracle SQL*Plus is needed 
 */
public class TestSQLPlus {

	private static final String SCRIPT_SQL = "script.sql";
	private static final String ORACLE_PASSWORD = "dimdim";
	private static final String ORACLE_USER = "minhasmoedas";
	static final String ORACLE_HOME = "/oracle/app/oracle/product/12.1.0/dbhome_1/";
	static final String ORACLE_INSTANCE = "ora";
	static final String WORK_DIR = System.getProperty("user.dir");

	@Test
	public void testVersion() throws IOException,InterruptedException {

		String detectedVersion = ExternalProgramUtil.getVersion(WORK_DIR,ORACLE_HOME);

		System.out.println("SQLPlus detected version = " + detectedVersion);

		assertTrue(detectedVersion.contains("SQL*Plus: Release 12.1.0.1.0 Production"));
	}

	@Test
	public void testUserDefinedScriptFile() throws IOException,InterruptedException {

		ClassLoader classLoader = getClass().getClassLoader();
		File file = new File(classLoader.getResource(SCRIPT_SQL).getFile());

		String output = ExternalProgramUtil.run(ORACLE_USER,ORACLE_PASSWORD,ORACLE_INSTANCE,file.getCanonicalPath(),WORK_DIR,ORACLE_HOME,ScriptType.userDefined.name());

		System.out.println("output = " + output);

		assertTrue(output.contains("BANNER"));
	}

	@Test
	public void testRunningScriptFile() throws IOException,InterruptedException {

		String output = ExternalProgramUtil.run(ORACLE_USER,ORACLE_PASSWORD,ORACLE_INSTANCE,"src/test/resources/script.sql",WORK_DIR,ORACLE_HOME,ScriptType.file.name());

		System.out.println("output = " + output);

		assertTrue(output.contains("BANNER"));
	}

}
