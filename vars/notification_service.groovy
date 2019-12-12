def call(Map config) {
    pipeline {
        agent {
            node {
                label "SPDCOREJ02E1B.devspeedpay.com"
                customWorkspace "workspace/${env.JOB_NAME}"
            }
        }

        options {
            disableConcurrentBuilds()
            skipDefaultCheckout(true)
        }

        parameters {
            string(description: '''Name of the Notification Engine Project for which build need to be executed; Example : NotifyCustomer''', name: 'PROJECT_NAME')

            string(description: '''Name of the Nexus Repository for Project; Example : nuget-notificationservice-notifycustomer''', name: 'NEXUS_PUBLISH_REPO')

            string(description: '''Name of the Nexus Package; Example : NotificationServiceNotifyCustomer''', name: 'NEXUS_PUBLISH_PACKAGE')

            string(description: '''Name of the Solution File; Example : WesternUnion.Speedpay.Notification.NotifyCustomer//WesternUnion.Speedpay.Notification.NotifyCustomer.csproj''', name: 'SOLUTION_FILE')

            string(description: '''Name of the Test Solution File; Example : WesternUnion.Speedpay.Notification.NotifyCustomer.Test//WesternUnion.Speedpay.Notification.NotifyCustomer.Test.csproj''', name: 'TEST_SOLUTION_FILE')

            string(description: '''Name of the CheckMarx Project; Example : Speedpay_AWS_NotificationServiceNotifyCustomer''', name: 'CHECKMARX_PROJECT_NAME')
        }

        environment {
            ENVIRONMENT       = "DEV"
            TENANT = "Track-5"
            NUGET_SOURCE1 = "https://spdcore07e1a.devspeedpay.com/repository/NuGet-Proxy/"
            NUGET_SOURCE2 = "https://nexus.nprd-speedpay.com/repository/nuget-notificationservice-core/"
            NUGET_SOURCE3 = "https://nexus.nprd-speedpay.com/repository/Speedpay-NuGet-Group/"
            
            ASSEMBLY_VERSION_FILE = "SharedAssemblyVersion.cs"
            VERSION_PREFIX = "20.0."
            
        }

        stages {
            stage ("PREPARE"){
                steps {
                    script{
                        cleanWs()
                    }
                }
            }

            stage("CHECKOUT LATEST CODE") {
                steps {
	                script {
		                checkout scm
		                Version = "${VERSION_PREFIX}${BUILD_NUMBER}"
		                powershell("(Get-Content -Path ${ASSEMBLY_VERSION_FILE}).replace('1.0.0.0', '${Version}') | Out-File ${ASSEMBLY_VERSION_FILE}")
		                GIT_SHA = powershell script: "git rev-parse --short HEAD", returnStdout: true
		                Version += "-${GIT_SHA}".trim()
		                powershell("(Get-Content -Path ${ASSEMBLY_VERSION_FILE}).replace('INFORMATIONAL_VERSION', '${Version}') | Out-File ${ASSEMBLY_VERSION_FILE}")
                        currentBuild.displayName = Version
	                }

                }
            }

            stage("BUILD SOLUTION FILE") {
                steps {
                    script {
                        def PROJECT_NAME = params.PROJECT_NAME
                        def NEXUS_PUBLISH_REPO = params.NEXUS_PUBLISH_REPO
                        def NEXUS_PUBLISH_PACKAGE = params.NEXUS_PUBLISH_PACKAGE
                        
                        def CHECKMARX_PROJECT_NAME = params.CHECKMARX_PROJECT_NAME
                        
                        def COMPLETE_NEXUS_PUBLISH_REPO = "${env.NEXUS_URL}/repository/${NEXUS_PUBLISH_REPO}/"
                        def COMPLETE_NEXUS_PUBLISH_PACKAGE = "${NEXUS_PUBLISH_PACKAGE}\\*.nupkg"
                        def COMPLETE_CHECKMARX_PROJECT_NAME = "Speedpay_AWS_${NEXUS_PUBLISH_PACKAGE}_${TENANT}"

                        Windows_Bat([
                            cmd: "mkdir ${NEXUS_PUBLISH_PACKAGE} && \
	                            dotnet build ${SOLUTION_FILE} -c \"Release\" -r \"win81-x64\" --source \"${NUGET_SOURCE1}\" --source \"${NUGET_SOURCE2}\" --source \"${NUGET_SOURCE3}\" && \
			                    dotnet publish ${SOLUTION_FILE} -c \"Release\" -r \"win81-x64\" --output ..\\${NEXUS_PUBLISH_PACKAGE}\\ "
                        ])
                    }
                }
	
                post {
		            success {
			            Windows_Bat([
			                cmd: "pushd ${NEXUS_PUBLISH_PACKAGE} && \
				                ${NUGET} spec WesternUnion.Speedpay.Notification.${PROJECT_NAME} && \
				                ${NUGET} pack WesternUnion.Speedpay.Notification.${PROJECT_NAME}.nuspec -Version ${Version} -NoPackageAnalysis && \
				                popd"
                        ])
		            }
		            
                    failure {
                        emailext attachLog: true,
                        body: "<p>Hi team,<br>Build Process failed for <b>NotificationService-${PROJECT_NAME}</b></p>",
                        postsendScript: '$DEFAULT_POSTSEND_SCRIPT',
                        presendScript: '$DEFAULT_PRESEND_SCRIPT',
                        replyTo: 'grp-aci-speedpay-communicationengine-pune@aciworldwide.com'
                        subject: "NotificationService-${PROJECT_NAME}:Jenkins ${BUILD_STATUS} [#${BUILD_NUMBER}]",
                        to: 'grp-aci-speedpay-communicationengine-pune@aciworldwide.com'
		            }
                }
            }
        }

        post {
            success {
                load 'environment.properties'
                load 'scanner.properties'

                emailext attachLog: true,
                body: "<p>Hi Team,<br>Build succeeded for <b>[#${BUILD_NUMBER}] - ${JOB_NAME}</b>.<br><br>Coverage status:<br> ${Coverage}<br>Report Link : <b>https://jenkins-master.nprd-speedpay.com/job/SpeedpayNotificationServiceNotifyCustomer/job/${BRANCH_NAME}/HTML_20Report/</b><br><br>Code Vulnerability Report :<br>Lines of code scanned = ${scanlines}<br>Files scanned = ${scanfiles}<br>Scan comments = ${scancomment}<br>URL = ${Link}<br><br>Regards,<br>DevOps Team</p>",
                compressLog: true,
                postsendScript: '$DEFAULT_POSTSEND_SCRIPT',
                presendScript: '$DEFAULT_PRESEND_SCRIPT',
                replyTo: 'grp-aci-speedpay-communicationengine-pune@aciworldwide.com'
                subject: "NotificationService-${PROJECT_NAME}:Jenkins ${BUILD_STATUS} [#${BUILD_NUMBER}]",
                to: 'grp-aci-speedpay-communicationengine-pune@aciworldwide.com'
            }

            failure {
                println PROJECT_NAME
            }
        }

    }
}
