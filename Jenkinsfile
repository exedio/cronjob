
import hudson.plugins.jacoco.model.CoverageGraphLayout
import hudson.plugins.jacoco.model.CoverageGraphLayout.CoverageType
import hudson.plugins.jacoco.model.CoverageGraphLayout.CoverageValue
import java.awt.Color

timestamps
{
	//noinspection GroovyAssignabilityCheck
	node
	{
		try
		{
			abortable
			{
				echo("Delete working dir before build")
				deleteDir()

				def scmResult = checkout scm
				computeGitTree(scmResult)

				env.BUILD_TIMESTAMP = new Date().format("yyyy-MM-dd_HH-mm-ss");
				env.JAVA_HOME = "${tool 'jdk 1.8.0_60'}"
				env.PATH = "${env.JAVA_HOME}/bin:${env.PATH}"
				def antHome = tool 'Ant version 1.9.3'

				sh "java -version"
				sh "${antHome}/bin/ant -version"

				def isRelease = env.BRANCH_NAME.toString().equals("master");

				properties([
						buildDiscarder(logRotator(
								numToKeepStr         : isRelease ? '1000' : '15',
								artifactNumToKeepStr : isRelease ? '1000' :  '2'
						))
				])

				sh 'echo' +
						' scmResult=' + scmResult +
						' BUILD_TIMESTAMP -${BUILD_TIMESTAMP}-' +
						' BRANCH_NAME -${BRANCH_NAME}-' +
						' BUILD_NUMBER -${BUILD_NUMBER}-' +
						' BUILD_ID -${BUILD_ID}-' +
						' isRelease=' + isRelease

				sh "${antHome}/bin/ant clean jenkins" +
						' "-Dbuild.revision=${BUILD_NUMBER}"' +
						' "-Dbuild.tag=git ${BRANCH_NAME} ' + scmResult.GIT_COMMIT + ' ' + scmResult.GIT_TREE + ' jenkins ${BUILD_NUMBER} ${BUILD_TIMESTAMP}"' +
						' -Ddisable-ansi-colors=true' +
						' -Dfindbugs.output=xml'

				warnings(
						canComputeNew: true,
						canResolveRelativePaths: true,
						categoriesPattern: '',
						consoleParsers: [[parserName: 'Java Compiler (javac)']],
						defaultEncoding: '', excludePattern: '', healthy: '', includePattern: '', messagesPattern: '', unHealthy: '',
						unstableTotalAll: '0',
						usePreviousBuildAsReference: false,
						useStableBuildAsReference: false,
				)
				findbugs(
						canComputeNew: true,
						defaultEncoding: '', excludePattern: '', healthy: '', includePattern: '',
						isRankActivated: false,
						pattern: 'build/findbugs.xml',
						unHealthy: '',
						unstableTotalAll: '0',
						usePreviousBuildAsReference: false,
						useStableBuildAsReference: false,
				)
				step([$class: 'JacocoPublisher',
						changeBuildStatus: true,
						minimumBranchCoverage: '30',
						coverageGraphLayout:
								new CoverageGraphLayout()
								.baseStroke(2f)
								.axis().skipZero().crop(50)
								.plot().type(CoverageType.BRANCH).value(CoverageValue.PERCENTAGE).color(Color.BLUE)
								.axis().skipZero().crop()
								.plot().type(CoverageType.BRANCH).value(CoverageValue.MISSED).color(Color.RED)
								.axis().skipZero().crop()
								.plot().type(CoverageType.LINE).value(CoverageValue.MISSED).color(Color.ORANGE),
						execPattern: 'build/jacoco.exec',
						classPattern: 'build/classes/src',
						sourcePattern: 'src'])
				archive 'build/success/*'
			}
		}
		catch(Exception e)
		{
			//todo handle script returned exit code 143
			throw e;
		}
		finally
		{
			// because junit failure aborts ant
			junit(
					allowEmptyResults: false,
					testResults: 'build/testresults/**/*.xml',
			)
			def to = emailextrecipients([
					[$class: 'CulpritsRecipientProvider'],
					[$class: 'RequesterRecipientProvider']
			])
			//TODO details
			step([$class: 'Mailer',
					recipients: to,
					attachLog: true,
					notifyEveryUnstableBuild: true])

			if('SUCCESS'.equals(currentBuild.result) ||
				'UNSTABLE'.equals(currentBuild.result))
			{
				echo("Delete working dir after " + currentBuild.result)
				deleteDir()
			}
		}
	}
}

def abortable(Closure body)
{
	try
	{
		body.call();
	}
	catch(hudson.AbortException e)
	{
		if(e.getMessage().contains("exit code 143"))
			return
		throw e;
	}
}

def computeGitTree(scmResult)
{
	sh "git cat-file -p " + scmResult.GIT_COMMIT + " | grep '^tree ' | sed -e 's/^tree //' > .git/jenkins-head-tree"
	scmResult.GIT_TREE = readFile('.git/jenkins-head-tree').trim()
}
