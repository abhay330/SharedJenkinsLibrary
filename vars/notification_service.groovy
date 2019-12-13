def call(Map config) {
    pipeline {
        agent any

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

                        println "${COMPLETE_NEXUS_PUBLISH_REPO}"
			println "${COMPLETE_NEXUS_PUBLISH_REPO}"
			echo "${PROJECT_NAME}"
                    }
                }
	
                post {
		            success {
				    script{
					    def COMPLETE_CHECKMARX_PROJECT_NAME = "Speedpay_AWS_${NEXUS_PUBLISH_PACKAGE}_${TENANT}"
				    }
				    println "${PROJECT_NAM}"
				    println "${COMPLETE_CHECKMARX_PROJECT_NAME}"
		            }
		            
                    failure {
                        println "${PROJECT_NAME}"
		    }
                }
            }
        }

        post {
            success {
                println "${PROJECT_NAME}"
		    echo "${PROJECT_NAME}"
            }

            failure {
                println "${PROJECT_NAME}"
            }
        }

    }
}
