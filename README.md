# Oficial plugin page (basic documentation + stats)

https://plugins.jenkins.io/sqlplus-script-runner

# Oficial wiki (basic documentation + user comments)

https://wiki.jenkins.io/display/JENKINS/SQLPlus+Script+Runner+Plugin

# Jenkins plugin - SQL*Plus Script Runner

This plugin enables you run Oracle SQL\*Plus scripts on your Jenkins jobs ( _SQL\*Plus installation required!_ ).

All you have to do is provide a valid *ORACLE_HOME* and you are in business:

![Main Setup](https://github.com/boaglio/jenkins-plugin-sqlplus-script-runner/raw/master/shots/main-setup.png)

You can run a script inside your workspace or a user defined for every job:

![Job Setup](https://github.com/boaglio/jenkins-plugin-sqlplus-script-runner/raw/master/shots/setup-by-project.png)

You can check later all SQL*Plus output inside your build output:

![Script running inside a job](https://github.com/boaglio/jenkins-plugin-sqlplus-script-runner/raw/master/shots/script-running.png)

Download the last release and give it a try!

https://github.com/boaglio/jenkins-plugin-sqlplus-script-runner/releases

# Developer guide

1. Fork repository
2. Code code code
3. Run it with:

* mvn clean
* mvn  -DskipTests package
* mvn generate-sources  (convert Message*.properties into Messages.java)
* mvn compiler:compile
* mvn  -DskipTests package hpi:run

4. Try it at http://localhost:8080
5. Commit and submit pull request

# Translator guide

1. Fork repository
2. Copy config.properties to config_<lang>.properties (example: config_pt_BR.properties)
3. Copy global.properties to global_<lang>.properties
4. Copy Messages.properties to Messages_<lang>.properties
5. Copy all HTML files too
6. Translate it
7. Run it with:

* mvn clean -DskipTests package hpi:run

8. Try it at http://localhost:8080
9. Commit and submit pull request

# Pipeline

## user defined script
 
node {
   echo 'SQLPlusRunner running user define script for system@xe'
   step([$class: 'SQLPlusRunnerBuilder',credentialsId:'system', instance:'xe',scriptType:'userDefined', script: '',scriptContent: 'select * from v$version'])
}

## file script

node {
   echo 'SQLPlusRunner running file script for system@xe'
   step([$class: 'SQLPlusRunnerBuilder',credentialsId:'system', instance:'xe',scriptType:'file', script: 'start.sql',scriptContent: ''])
}

### Optional parameters

* customOracleHome
* customSQLPlusHome
* customTNSAdmin
* customNLSLang
* customSQLPath

# Having problems?

Please [open a new issue](https://github.com/jenkinsci/sqlplus-script-runner-plugin/issues/new)  and inform:

- Jenkins server Operation System;
- Jenkins version;
- Where SQLPlus Script Runner is running (local machine or agent machine);
- Agent machine Operation System (if applicable);
- Oracle Database version;
- Oracle SQL*Plus version;
- Build log with debug info enabled.

