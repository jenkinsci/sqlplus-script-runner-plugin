package org.jenkinsci.plugins.sqlplus.script.runner;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;

import hudson.FilePath;
import hudson.model.Run;

public class FileUtil {

	private static final int SQLPLUS_STR_LENGTH = 5;
	private static final String LAST_CMD_BEFORE_EXIT = "\n;\n";
	private static final String SQLPLUS_EXIT = "exit;";
	private static final String SQL_TEMP_SCRIPT = "temp-script-";
	private static final String SQL_PREFIX = ".sql";

	public static boolean hasExitCode(FilePath filePath) {

		boolean found = false;
		try {

			InputStream in = filePath.read();
			Reader reader = new InputStreamReader(in, StandardCharsets.UTF_8);
			BufferedReader br = new BufferedReader(reader);

			String lastLine = "";
			String line;
			while ((line = br.readLine()) != null) {
				if (line.length() >= SQLPLUS_STR_LENGTH)
					lastLine = line;
			}

			if (lastLine.trim().equalsIgnoreCase(SQLPLUS_EXIT))
				found = true;

			br.close();
			reader.close();
			in.close();
		} catch (IOException exc) {
			System.out.println(exc);
		} catch (InterruptedException ie) {
			System.out.println(ie);
		}
		return found;
	}



	public static void addExit(String content, FilePath filePath) throws IOException {

		try {

			if (content != null)
				filePath.write(content + LAST_CMD_BEFORE_EXIT + SQLPLUS_EXIT, StandardCharsets.UTF_8.name());

		} catch (IOException | InterruptedException e) {
			e.printStackTrace();
		}
	}

	public static void addExitInTheEnd(FilePath filePath) throws IOException {

		try {
			
            String content = filePath.readToString();                        
            filePath.write(content + LAST_CMD_BEFORE_EXIT + SQLPLUS_EXIT, StandardCharsets.UTF_8.name());
		} catch (IOException | InterruptedException e) {
			e.printStackTrace();
		}

	}

	@SuppressWarnings("static-access")
	public static FilePath createTempScript(Run<?, ?> build, String content) {

		FilePath filePath = null;
		try {

			filePath = new FilePath(build.getRootDir().createTempFile(SQL_TEMP_SCRIPT + System.currentTimeMillis(), SQL_PREFIX));
			
			filePath.write(content, StandardCharsets.UTF_8.name());

			if (!FileUtil.hasExitCode(filePath))
				addExit(content
						, filePath);

		} catch (IOException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		return filePath;

	}

	public static boolean findFile(String name, File file) {

		boolean found = false;
		File[] list = file.listFiles();
		if (list != null)
			for (File fil : list) {
				if (fil.isDirectory()) {
					findFile(name, fil);
				} else if (name.equalsIgnoreCase(fil.getName())) {
					found = true;
				}
			}
		return found;
	}

}
