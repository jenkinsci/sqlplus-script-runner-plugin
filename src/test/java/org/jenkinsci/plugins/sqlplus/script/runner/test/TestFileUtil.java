package org.jenkinsci.plugins.sqlplus.script.runner.test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;

import org.jenkinsci.plugins.sqlplus.script.runner.FileUtil;
import org.junit.Test;

import hudson.FilePath;

public class TestFileUtil {

	private static final String SCRIPT_SQL = "src/test/resources/script.sql";
	private static final String SCRIPT2_SQL = "src/test/resources/script2.sql";

	static final String WORK_DIR = System.getProperty("user.dir");

	@Test
	public void testExitInScriptFile() throws IOException,InterruptedException {


		File file = new File(WORK_DIR+File.separator+SCRIPT_SQL);

		boolean hasExit = FileUtil.hasExitCode(new FilePath(file));

		System.out.println("hasExit = " + hasExit);

		assertTrue(hasExit);
	}

	@Test
	public void testNoExitInScriptFile() throws IOException,InterruptedException {


		File file = new File(WORK_DIR+File.separator+SCRIPT2_SQL);

		boolean hasExit = FileUtil.hasExitCode(new FilePath(file));

		System.out.println("hasNoExit = " + hasExit);

		assertFalse(hasExit);
	}



}
