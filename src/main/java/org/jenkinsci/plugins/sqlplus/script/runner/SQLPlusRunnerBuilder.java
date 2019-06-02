package org.jenkinsci.plugins.sqlplus.script.runner;

import java.io.IOException;
import java.util.List;

import org.jenkinsci.Symbol;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.StaplerRequest;

import com.cloudbees.plugins.credentials.Credentials;
import com.cloudbees.plugins.credentials.CredentialsMatcher;
import com.cloudbees.plugins.credentials.CredentialsMatchers;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.StandardUsernameCredentials;
import com.cloudbees.plugins.credentials.common.StandardUsernameListBoxModel;
import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials;
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
import hudson.util.Secret;
import jenkins.model.Jenkins;
import jenkins.tasks.SimpleBuildStep;
import net.sf.json.JSONObject;

@SuppressFBWarnings
@Symbol("sqlplusrunner")
public class SQLPlusRunnerBuilder extends Builder implements SimpleBuildStep {

	private final String credentialsId;
	private final String user;
	private final String password;
	private final String instance;
	private final String scriptType;
	private final String script;
	private final String scriptContent;

	private String customOracleHome;
	private String customSQLPlusHome;
	private String customTNSAdmin;

	@DataBoundConstructor
	public SQLPlusRunnerBuilder(String credentialsId, String instance, String scriptType, String script,
			String scriptContent) {
		this.credentialsId = credentialsId;
		this.user = null;
		this.password = null;
		this.instance = instance;
		this.scriptType = scriptType;
		this.script = script;
		this.scriptContent = scriptContent;
	}

	public SQLPlusRunnerBuilder(String credentialsId, String instance, String scriptType, String script,
			String scriptContent, String customOracleHome, String customSQLPlusHome, String customTNSAdmin) {
		this.credentialsId = credentialsId;
		this.user = null;
		this.password = null;
		this.instance = instance;
		this.scriptType = scriptType;
		this.script = script;
		this.scriptContent = scriptContent;
		this.customOracleHome = customOracleHome;
		this.customSQLPlusHome = customSQLPlusHome;
		this.customTNSAdmin = customTNSAdmin;
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

		List<StandardUsernamePasswordCredentials> lookupCredentials = CredentialsProvider.lookupCredentials(
				StandardUsernamePasswordCredentials.class, Jenkins.getInstance(), ACL.SYSTEM, null, null);
		CredentialsMatcher credentialsMatcher = CredentialsMatchers.withId(credentialsId);
		StandardUsernamePasswordCredentials credentials = CredentialsMatchers.firstOrNull(lookupCredentials,
				credentialsMatcher);
		if (credentials == null && (this.user == null)) {
			throw new AbortException("Invalid credentials " + credentialsId
					+ ". Failed to initialize credentials or load user and pass");
		}
		final Secret password = credentials.getPassword();

		final String usr = credentials == null ? this.user : credentials.getUsername();
		final String pwd = credentials == null ? this.password : password.getPlainText();

		EnvVars env = build.getEnvironment(listener);

		SQLPlusRunner sqlPlusRunner = new SQLPlusRunner(build, listener, launcher, workspace,
				getDescriptor().isHideSQLPlusVersion(), usr, pwd, env.expand(instance), env.expand(sqlScript),
				getDescriptor().globalOracleHome, getDescriptor().globalSQLPlusHome, getDescriptor().globalTNSAdmin,
				scriptType, customOracleHome, customSQLPlusHome, customTNSAdmin, getDescriptor().tryToDetectOracleHome,
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
		private static final String HIDE_SQL_PLUS_VERSION = "hideSQLPlusVersion";
		private static final String TRY_TO_DETECT_ORACLE_HOME = "tryToDetectOracleHome";
		private static final String DEBUG = "debug";
		private boolean hideSQLPlusVersion;
		private boolean tryToDetectOracleHome;
		private boolean debug;
		private String globalOracleHome;
		private String globalSQLPlusHome;
		private String globalTNSAdmin;

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
