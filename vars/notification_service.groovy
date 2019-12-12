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

                        println ${PROJECT_NAME}
			println ${NEXUS_PUBLISH_REPO}
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
                        println ${PROJECT_NAME}
		    }
                }
            }
        }

        post {
            success {
                println ${PROJECT_NAME}
            }

            failure {
                println PROJECT_NAME
            }
        }

    }
}
