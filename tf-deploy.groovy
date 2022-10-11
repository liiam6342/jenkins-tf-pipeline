	#!/usr/bin/env groovy
// Reference https://www.eficode.com/blog/jenkins-groovy-tutorial
// Jenkinsfile (Declarative Pipeline)

pipeline {
    environment {
    CHECKOV     = '' 
    REPO        = "https://github.com/USERNAME/REPO.git"
    BRANCH      = "master"
    }

  agent any
  stages {
    stage('Checkout code') {
        steps {
          git url: REPO, branch: BRANCH, credentialsId: 'github'
            }     
           }

    stage('Terraform Init') {
        steps { 
            script {   
             withCredentials([usernamePassword(credentialsId: 'awscreds', usernameVariable: 'AWS_ACC_KEY', passwordVariable: 'AWS_SEC_KEY')]){
                    sh "terraform init -backend-config=access_key=${AWS_ACC_KEY} -backend-config=secret_key=${AWS_SEC_KEY} -backend-config=region=eu-west-1 "               
             }
            }
        }
    }

    stage('Terraform Validate') {
        steps {
            script {            
                    sh "terraform validate"
                }
        }
    }


    stage('Terraform Lint') {
        steps {
            script {            
                    sh "tflint --init"
                    sh "tflint -f=checkstyle > tflint-report.xml"
                }
            }    
    }

    stage('Terraform Compliance') {
        steps {
            script {
                def checkovResult = sh script: "checkov -d . --skip-check \"$env.CHECKOV\" -o junitxml > checkov-report.xml", returnStatus: true
               
                if(checkovResult != 0) {
                    echo "Checkov failed check Test Results"
                    error("Checkov failed check Test Results")
                }
                
            }
        }
        post {
            always {
                junit checksName: 'Terraform Checkov', testResults: "checkov-report.xml", allowEmptyResults: true, skipPublishingChecks: true
            }
        }
    }
    stage('Terraform Plan') {
        steps {
              script {
                withCredentials([usernamePassword(credentialsId: 'awscreds', usernameVariable: 'AWS_ACC_KEY', passwordVariable: 'AWS_SEC_KEY')]){
                    def planResult = sh script: "terraform plan -no-color -out=tfplan >tfplan.txt 2>&1 -var 'secret_access=${AWS_SEC_KEY}' -var 'access_key=${AWS_ACC_KEY}'", returnStatus: true
                    sh "cat tfplan.txt"

                    if(planResult != 0) {
                        echo "Terraform Plan Failed"
                        error("Terraform Plan Failed")
                    }        
                }
              }
            }    
        }

    stage('Terraform Apply') {
        steps {
            script {
                def applyResult = sh script: "terraform apply -no-color -auto-approve tfplan >tfapply.txt 2>&1", returnStatus: true
                sh "cat tfapply.txt"

                if(applyResult != 0) {
                    echo "Terraform Apply Failed"
                    error("Terraform Apply Failed")
                }
            }
        }
    }

    stage('Terraform Destroy') {
        steps {
            script {
                withCredentials([usernamePassword(credentialsId: 'awscreds', usernameVariable: 'AWS_ACC_KEY', passwordVariable: 'AWS_SEC_KEY')]){
                def applyResult = sh script: "terraform destroy --auto-approve -var 'secret_access=${AWS_SEC_KEY}' -var 'access_key=${AWS_ACC_KEY}'", returnStatus: true
                }
            }
        }
     }
  }
}