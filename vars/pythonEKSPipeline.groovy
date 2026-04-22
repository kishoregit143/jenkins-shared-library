// call is the default function name
def call (Map configMap){
    pipeline {
    // These are pre-build sections
        agent {
            node {
                label 'AGENT-1' //roboshop-java
            }
        }
        environment {
            COURSE = "Jenkins"
            appVersion = configMap.get("appVersion")
            ACC_ID = "307223751858"
            PROJECT = configMap.get("project")
            COMPONENT = configMap.get("component")
        }
        options {
            timeout(time: 10, unit: 'MINUTES') 
            disableConcurrentBuilds()
        }
        // This is build section
        stages {
            stage('Read Version') {
                steps {
                    script{
                        appVersion = readFile(file: 'version')
                        echo "app version: ${appVersion}"
                    }
                }
            }
            stage('Install Dependencies') {
                steps {
                    script{
                        sh """
                            pip3 install -r requirements.txt
                        """
                    }
                }
            }
            stage('Unit Test') {
                steps {
                    script{
                        sh """
                            echo test
                        """
                    }
                }
            }
           
            
            stage('Build Image') {
                steps {
                    script{
                        withAWS(region:'us-east-1',credentials:'aws-creds') {
                            sh """
                             aws ecr describe-repositories --repository-names ${PROJECT}/${COMPONENT} \
                             || aws ecr create-repository --repository-name ${PROJECT}/${COMPONENT}
                             aws ecr get-login-password --region us-east-1 | docker login --username AWS --password-stdin ${ACC_ID}.dkr.ecr.us-east-1.amazonaws.com
                             docker build -t ${ACC_ID}.dkr.ecr.us-east-1.amazonaws.com/${PROJECT}/${COMPONENT}:${appVersion} .
                             docker images
                             docker push ${ACC_ID}.dkr.ecr.us-east-1.amazonaws.com/${PROJECT}/${COMPONENT}:${appVersion}
                            """
                        }
                    }
                }
            }
            
        

            stage('Trigger DEV Deploy') {
                steps {
                    script {
                        build job: "../${COMPONENT}-deploy",
                            wait: false, // Wait for completion
                            propagate: false, // Propagate status
                            parameters: [
                                string(name: 'appVersion', value: "${appVersion}"),
                                string(name: 'deploy_to', value: "dev")
                            ]
                    }
                }
            }

        }

            

        post{
            always{
                echo 'I will always say Hello again!'
                cleanWs()
            }
            success {
                echo 'I will run if success'
            }
            failure {
                echo 'I will run if failure'
            }
            aborted {
                echo 'pipeline is aborted'
            }
        }
    }
}