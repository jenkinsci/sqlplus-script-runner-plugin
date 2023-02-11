package org.jenkinsci.plugins.sqlplus.script.runner;

import java.io.IOException;

import org.jenkinsci.Symbol;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.StaplerRequest;

import com.cloudbees.plugins.credentials.Credentials;
import com.cloudbees.plugins.credentials.CredentialsMatcher;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.StandardUsernameCredentials;
import com.cloudbees.plugins.credentials.common.StandardUsernameListBoxModel;
import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials;
import com.cloudbees.plugins.credentials.common.UsernamePasswordCredentials;
import com.cloudbees.plugins.credentials.impl.UsernamePasswordCredentialsImpl;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import hudson.AbortException;
import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractProject;
import hudson.model.Computer;
import hudson.model.ItemGroup;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.security.ACL;
import hudson.security.AccessControlled;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.util.ListBoxModel;
import jenkins.model.Jenkins;
import jenkins.tasks.SimpleBuildStep;
import net.sf.json.JSONObject;

@SuppressFBWarnings
@Symbol("sqlplusrunner")
public class SQLPlusRunnerBuilder extends Builder implements SimpleBuildStep {

	private final String credentialsId;
	private final String user;
	private final String password;
	private final String isSysdba;
	private final String instance;
	private final String scriptType;
	private final String script;
	private final String scriptContent;

	private String customOracleHome;
	private String customSQLPlusHome;
	private String customTNSAdmin;
	private String customNLSLang;
	private String customSQLPath;

	@DataBoundConstructor
	public SQLPlusRunnerBuilder(String credentialsId, String user, String password,String isSysdba, String instance, String scriptType, String script,
			String scriptContent) {
		this.credentialsId = credentialsId;
		this.user = user;
		this.password = password;
		this.isSysdba = isSysdba;
		this.instance = instance;
		this.scriptType = scriptType;
		this.script = script;
		this.scriptContent = scriptContent;
	}

	public SQLPlusRunnerBuilder(String credentialsId, String user, String password, String isSysdba, String instance, String scriptType, String script,
			String scriptContent, String customOracleHome, String customSQLPlusHome, String customTNSAdmin,String customNLSLang,String customSQLPath) {
		this.credentialsId = credentialsId;
		this.user = user;
		this.password = password;
		this.isSysdba = isSysdba;
		this.instance = instance;
		this.scriptType = scriptType;
		this.script = script;
		this.scriptContent = scriptContent;
		this.customOracleHome = customOracleHome;
		this.customSQLPlusHome = customSQLPlusHome;
		this.customTNSAdmin = customTNSAdmin;
		this.customNLSLang = customNLSLang;
		this.customSQLPath = customSQLPath;
	}

	@DataBoundSetter
	public void setCustomOracleHome(String customOracleHome) {
		this.customOracleHome = customOracleHome;
	}

	@DataBoundSetter
	public void setCustomSQLPlusHome(String customSQLPlusHome) {
		this.customSQLPlusHome = customSQLPlusHome;
	}

	@DataBoundSetter
	public void setCustomTNSAdmin(String customTNSAdmin) {
		this.customTNSAdmin = customTNSAdmin;
	}

	public String getUser() {
		return user;
	}

	public String getPassword() {
		return password;
	}

	public String isSysdba() {
		return isSysdba;
	}

	public String getInstance() {
		return instance;
	}

	public String getScriptType() {
		return scriptType;
	}

	public String getScript() {
		return script;
	}

	public String getScriptContent() {
		return scriptContent;
	}

	public String getCustomOracleHome() {
		return customOracleHome;
	}

	public String getCustomSQLPlusHome() {
		return customSQLPlusHome;
	}

	public String getCustomTNSAdmin() {
		return customTNSAdmin;
	}

	public String getCustomNLSLang() {
		return customNLSLang;
	}

	@DataBoundSetter
	public void setCustomNLSLang(String customNLSLang) {
		this.customNLSLang = customNLSLang;
	}

	public String getCustomSQLPath() {
		return customSQLPath;
	}

	@DataBoundSetter
	public void setCustomSQLPath(String customSQLPath) {
		this.customSQLPath = customSQLPath;
	}

	public String getCredentialsId() {
		return credentialsId;
	}

	@Override
	public void perform(Run<?, ?> build, FilePath workspace, Launcher launcher, TaskListener listener)
			throws InterruptedException, IOException {

		String sqlScript;
		if (ScriptType.userDefined.name().equals(scriptType)) {
			sqlScript = scriptContent;
		} else {
			sqlScript = script;
		}

		String usr = this.user;
		String pwd = this.password;

		if(credentialsId != null){
			final UsernamePasswordCredentials credentials =  CredentialsProvider.findCredentialById(credentialsId,
																	 StandardUsernamePasswordCredentials.class,
																	 build, null, null);
			if (credentials != null){
				usr = credentials.getUsername();
				pwd = credentials.getPassword().getPlainText();
			}
		}

		if (usr == null || pwd == null) {
			throw new AbortException(Messages.SQLPlusRunner_errorInvalidCredentials(credentialsId));
		}

		boolean isConnectAsSysdba = "true".equalsIgnoreCase(isSysdba);
		
		EnvVars env = build.getEnvironment(listener);

		SQLPlusRunner sqlPlusRunner = new SQLPlusRunner(build, listener, launcher, workspace,
				getDescriptor().isHideSQLPlusVersion(), usr, pwd, isConnectAsSysdba, env.expand(instance), env.expand(sqlScript),
				getDescriptor().globalOracleHome, getDescriptor().globalSQLPlusHome, getDescriptor().globalTNSAdmin,
				scriptType, customOracleHome, customSQLPlusHome, customTNSAdmin, customNLSLang, customSQLPath, getDescriptor().tryToDetectOracleHome,
				getDescriptor().isDebug());

		try {

			sqlPlusRunner.run();

		} catch (Exception e) {

			e.printStackTrace(listener.getLogger());
			throw new AbortException(e.getMessage());
		}

	}

	@Override
	public DescriptorImpl getDescriptor() {
		return (DescriptorImpl) super.getDescriptor();
	}

	@Extension
	public static final class DescriptorImpl extends BuildStepDescriptor<Builder> {

		private static final String DISPLAY_MESSAGE = "SQLPlus Script Runner";
		private static final String GLOBAL_ORACLE_HOME = "globalOracleHome";
		private static final String GLOBAL_SQLPLUS_HOME = "globalSQLPlusHome";
		private static final String GLOBAL_TNS_ADMIN = "globalTNSAdmin";
		private static final String GLOBAL_NLS_LANG = "globalNLSLang";
		private static final String GLOBAL_SQL_PATH = "globalSQLPath";
		private static final String HIDE_SQL_PLUS_VERSION = "hideSQLPlusVersion";
		private static final String TRY_TO_DETECT_ORACLE_HOME = "tryToDetectOracleHome";
		private static final String DEBUG = "debug";
		private boolean hideSQLPlusVersion;
		private boolean tryToDetectOracleHome;
		private boolean debug;
		private String globalOracleHome;
		private String globalSQLPlusHome;
		private String globalTNSAdmin;
		private String globalNLSLang;
		private String globalSQLPath;
		

		public DescriptorImpl() {
			load();
		}

		@Override
		public boolean isApplicable(@SuppressWarnings("rawtypes") Class<? extends AbstractProject> aClass) {
			return true;
		}

		@Override
		public String getDisplayName() {
			return DISPLAY_MESSAGE;
		}

		@Override
		public boolean configure(StaplerRequest req, JSONObject formData) throws FormException {
			hideSQLPlusVersion = formData.getBoolean(HIDE_SQL_PLUS_VERSION);
			globalOracleHome = formData.getString(GLOBAL_ORACLE_HOME);
			globalSQLPlusHome = formData.getString(GLOBAL_SQLPLUS_HOME);
			globalTNSAdmin = formData.getString(GLOBAL_TNS_ADMIN);
			globalNLSLang = formData.getString(GLOBAL_NLS_LANG);
			globalSQLPath = formData.getString(GLOBAL_SQL_PATH);
			tryToDetectOracleHome = formData.getBoolean(TRY_TO_DETECT_ORACLE_HOME);
			debug = formData.getBoolean(DEBUG);
			save();
			return super.configure(req, formData);
		}

		public boolean isHideSQLPlusVersion() {
			return hideSQLPlusVersion;
		}

		public void setHideSQLPlusVersion(boolean hideSQLPlusVersion) {
			this.hideSQLPlusVersion = hideSQLPlusVersion;
		}

		public boolean isTryToDetectOracleHome() {
			return tryToDetectOracleHome;
		}

		public void setTryToDetectOracleHome(boolean tryToDetectOracleHome) {
			this.tryToDetectOracleHome = tryToDetectOracleHome;
		}

		public boolean isDebug() {
			return debug;
		}

		public void setDebug(boolean debug) {
			this.debug = debug;
		}

		public String getOracleHome() {
			return globalOracleHome;
		}

		public void setOracleHome(String globalOracleHome) {
			this.globalOracleHome = globalOracleHome;
		}

		public String getGlobalSQLPlusHome() {
			return globalSQLPlusHome;
		}

		public void setGlobalSQLPlusHome(String globalSQLPlusHome) {
			this.globalSQLPlusHome = globalSQLPlusHome;
		}

		public String getGlobalTNSAdmin() {
			return globalTNSAdmin;
		}

		public void setGlobalTNSAdmin(String globalTNSAdmin) {
			this.globalTNSAdmin = globalTNSAdmin;
		}

		public String getGlobalNLSLang() {
			return globalNLSLang;
		}

		public void setGlobalNLSLang(String globalNLSLang) {
			this.globalNLSLang = globalNLSLang;
		}

		public String getGlobalSQLPath() {
			return globalSQLPath;
		}

		public void setGlobalSQLPath(String globalSQLPath) {
			this.globalSQLPath = globalSQLPath;
		}

		public String getGlobalOracleHome() {
			return globalOracleHome;
		}

		public void setGlobalOracleHome(String globalOracleHome) {
			this.globalOracleHome = globalOracleHome;
		}

		@SuppressWarnings("deprecation")
		public ListBoxModel doFillCredentialsIdItems(@AncestorInPath ItemGroup<?> context) {
			if (!(context instanceof AccessControlled ? (AccessControlled) context : Jenkins.getInstance())
					.hasPermission(Computer.CONFIGURE)) {
				return new ListBoxModel();
			}
			return new StandardUsernameListBoxModel().withMatching(new CredentialsMatcher() {
				private static final long serialVersionUID = 1L;

				@Override
				public boolean matches(Credentials item) {
					return item instanceof UsernamePasswordCredentialsImpl;
				}
			}, CredentialsProvider.lookupCredentials(StandardUsernameCredentials.class, context, ACL.SYSTEM, null,
					null));
		}
	}

}
