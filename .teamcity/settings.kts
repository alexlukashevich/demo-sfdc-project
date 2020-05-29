import jetbrains.buildServer.configs.kotlin.v2018_2.*
import jetbrains.buildServer.configs.kotlin.v2018_2.buildFeatures.commitStatusPublisher
import jetbrains.buildServer.configs.kotlin.v2018_2.buildFeatures.pullRequests
import jetbrains.buildServer.configs.kotlin.v2018_2.buildSteps.ant
import jetbrains.buildServer.configs.kotlin.v2018_2.triggers.vcs
import jetbrains.buildServer.configs.kotlin.v2018_2.vcs.GitVcsRoot

/*
The settings script is an entry point for defining a TeamCity
project hierarchy. The script should contain a single call to the
project() function with a Project instance or an init function as
an argument.

VcsRoots, BuildTypes, Templates, and subprojects can be
registered inside the project using the vcsRoot(), buildType(),
template(), and subProject() methods respectively.

To debug settings scripts in command-line, run the

    mvnDebug org.jetbrains.teamcity:teamcity-configs-maven-plugin:generate

command and attach your debugger to the port 8000.

To debug in IntelliJ Idea, open the 'Maven Projects' tool window (View
-> Tool Windows -> Maven Projects), find the generate task node
(Plugins -> teamcity-configs -> teamcity-configs:generate), the
'Debug' option is available in the context menu for the task.
*/

version = "2019.1"

project {

    vcsRoot(Demo1)

    buildType(DemoBuild)
    buildType(DemoDeployQA)
    buildType(DemoDeployUat)

    features {
        feature {
            id = "PROJECT_EXT_2"
            type = "ReportTab"
            param("startPage", "pmd.html")
            param("title", "PMD report")
            param("type", "BuildReportTab")
        }
    }
}

object DemoBuild : BuildType({
    name = "Demo PR CI"

    artifactRules = """
        pmd.html
        pmd.xml
        outputCPD.xml
    """.trimIndent()

    params {
        param("env.MAX_POLL", "1000")
        param("system.cpd.minimumTokenCount", "101")
        param("system.cpd.format", "xml")
        param("env.SF_URL", "https://test.salesforce.com")
        param("env.SF_LOGIN", "a@Demo.com.ci")
        param("system.src.dir", "src")
        param("system.pmd.format", "xml")
        password("env.SF_PASS", "credentialsJSON:2b4b1af1-1a49-4c75-a5fe-3698508313cd", label = "Pass", display = ParameterDisplay.HIDDEN)
    }

    vcs {
        root(DslContext.settingsRoot)
    }

    steps {
        ant {
            mode = antFile {
            }
            targets = "validateRunTests"
            coverageEngine = idea {
                includeClasses = "*.cls"
            }
        }
        ant {
            mode = antFile {
            }
            targets = "staticCodeAnalysis"
        }
    }

    triggers {
        vcs {
            branchFilter = "+:pull-requests/*"
            enableQueueOptimization = false
        }
    }

    features {
        commitStatusPublisher {
            publisher = bitbucketServer {
                url = "https://git.itransition.com/"
            }
            param("stashAuthType", "token")
            param("secure:stashToken", "credentialsJSON:4d6cde32-c1ee-479d-81b4-5a1e0b856402")
        }
        pullRequests {
            provider = bitbucketServer {
                authType = vcsRoot()
            }
        }
        feature {
            type = "xml-report-plugin"
            param("xmlReportParsing.reportType", "pmd")
            param("xmlReportParsing.reportDirs", "+:pmd.xml")
            param("xmlReportParsing.verboseOutput", "true")
        }
        feature {
            type = "xml-report-plugin"
            param("xmlReportParsing.reportType", "pmdCpd")
            param("xmlReportParsing.reportDirs", "+:cpd.xml")
            param("xmlReportParsing.verboseOutput", "true")
        }
    }
})

object DemoDeployQA : BuildType({
    name = "Demo deploy QA"

    params {
        param("env.MAX_POLL", "1000")
        param("env.SF_LOGIN", "a@Demo.com.qa")
        password("env.SF_PASS", "credentialsJSON:8395e2a5-4a72-4e1d-9063-5ebb95a12bb8", label = "Pass", display = ParameterDisplay.HIDDEN)
        param("env.SF_URL", "https://test.salesforce.com")
    }

    vcs {
        root(Demo1)
    }

    steps {
        ant {
            mode = antFile {
            }
            targets = "deployRunTests"
        }
    }

    triggers {
        vcs {
            triggerRules = "+:comment=(.*Merge in Demo.*):**"
            branchFilter = "+:Release-2.0"
        }
    }
})

object DemoDeployUat : BuildType({
    name = "Demo deploy UAT"

    params {
        param("env.MAX_POLL", "1000")
        param("env.SF_LOGIN", "a@Demo.com.uat")
        password("env.SF_PASS", "credentialsJSON:79081036-fa02-412f-9315-685b957d149c", label = "Pass", display = ParameterDisplay.HIDDEN)
        param("env.SF_URL", "https://test.salesforce.com")
    }

    vcs {
        root(Demo1)
    }

    steps {
        ant {
            mode = antFile {
            }
            targets = "deployRunTests"
        }
    }

    triggers {
        vcs {
            triggerRules = "+:comment=(.*Merge in Demo.*):**"
            branchFilter = "+:Release-2.0"
        }
    }
})

object Demo1 : GitVcsRoot({
    name = "Demo"
    url = "https://git.itransition.com/scm/Demo/Demo-salesforce-customization.git"
    branch = "refs/heads/Release-2.0"
    branchSpec = "+:refs/heads/*"
    authMethod = password {
        userName = "git"
        password = "credentialsJSON:4e7395b5-488f-433d-be78-536fe2260480"
    }
})
})
