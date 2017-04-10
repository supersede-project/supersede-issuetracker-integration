JIRA SDK INSTRUCTIONS
=========================
20170410
matteo.pedrotti@deltinformatica.eu

REQUIREMENTS
-------------------------
1. download and install JIRA SDK
  * JVM 1.8
  * https://developer.atlassian.com/docs/getting-started
  * https://developer.atlassian.com/docs/getting-started/set-up-the-atlassian-plugin-sdk-and-build-a-project

ATLAS SDK
-------------------------
2. `atlas-run` installs this plugin into the product and starts it on localhost (http://localhost:2990/jira)
  * `atlas-debug` same as atlas-run, but allows a debugger to attach at port 5005
3. enable Quick-Reload and deploy on the fly:
  * `atlas-cli` after atlas-run or atlas-debug, opens a Maven command line window: `'pi'` reinstalls the plugin into the running product instance
  * or `atlas-mvn package` compiles and deploys the plugin - check quickreload is enabled
  * `atlas-help` prints description for all commands in the SDK
https://developer.atlassian.com/display/DOCS/Introduction+to+the+Atlassian+Plugin+SDK

SETUP PLUGIN
-------------------------
4. `atlas-mvn eclipse:eclipse` creates/updated eclipse project
5. edit `pom.xml` organization and stuff

PREPARE ITEMS
-------------------------
6. `atlas-create-jira-plugin-module` (Web Item and Web Section, iterated as needed)
7. edit `atlassian-plugin.xml` items and sections manually...

PREPARE SERVLET MODULE
-------------------------
8. `atlas-create-jira-plugin-module` (Servlet, eu.supersede.jira.plugins.servlet.ServletMan.java)
9. `atlas-mvn eclipse:eclipse`

SERVLET DEPENDENCIES (Velocity)
-------------------------
10. atlas-create-jira-plugin-module (Component Import)
  * com.atlassian.templaterenderer.TemplateRenderer
  * com.atlassian.sal.api.user.UserManager
11. edit pom.xml
  * `<dependency>`
  `<groupId>com.atlassian.templaterenderer</groupId>`
  `<artifactId>atlassian-template-renderer-api</artifactId>`
  `<version>1.3.1</version>`
  `<scope>provided</scope>`
  `</dependency>`
  * comment out the key `<!-- <Atlassian-Plugin-Key>${atlassian.plugin.key}</Atlassian-Plugin-Key> -->`

SERVLET
-------------------------
12. prepare velocity template `src/main/resources/templates/supersede-man.vm`
13. refactor `SupersedeMan.java` with relevant stuff (constructor and @Override doGet)
14. ..._iterate_!
