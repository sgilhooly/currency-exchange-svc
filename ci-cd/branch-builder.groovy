
def flow="none"
pipeline {
  agent {
    docker {
      image 'mtbuild:8'
      args '--privileged --group-add docker'
    }
  }
  stages {
    stage('build') {
      steps {

        script {
          echo "Checking build stature for branch $env.GIT_BRANCH"
          if ("origin/master" == env.GIT_BRANCH) {
            flow = "release"
          } else if ("origin/develop" == env.GIT_BRANCH) {
            flow = "develop"
          } else {
            sh 'printenv'
            error("Unable to determine branch name from ${env.GIT_BRANCH}")
          }
        }

        git credentialsId: 'github-ssh', changelog: true, poll: true, branch: 'builder', url: 'https://github.com/sgilhooly/currency-exchange-svc.git'
        withCredentials([string(credentialsId: 'aws-access-secret', variable: 'AWS_SECRET_ACCESS_KEY'),
                         string(credentialsId: 'aws-access-key', variable: 'AWS_ACCESS_KEY_ID')]) {
          withEnv(["REGISTRY_SETTINGS=$WORKSPACE/aws-registry.properties", 'AWS_PROFILE=default']) {
            sh "./gradlew --no-daemon -PdockerRegistry=971963691537.dkr.ecr.us-east-1.amazonaws.com -Pflow=${flow} publishImage"
          }
        }
      }
    }
    stage('deploy') {
      steps {
        cleanWs()
        git credentialsId: 'github-ssh', changelog: false, poll: false, branch: 'master', url: 'git@github.com:sgilhooly/release-control.git'
        withCredentials([sshUserPrivateKey(credentialsId: 'github-ssh', keyFileVariable: 'GIT_KEY_FILE', passphraseVariable: 'NO_PASSPHRASE', usernameVariable: 'NO_USER')]) {
          sh 'echo $GIT_SSH_COMMAND'
          sh 'git --version'
          //withEnv(['GIT_SSH_COMMAND=\'ssh -o UserKnownHostsFile=/dev/null -o StrictHostKeyChecking=no -i $GIT_KEY_FILE\'']) {
          sh 'GIT_SSH_COMMAND=\'ssh -o UserKnownHostsFile=/dev/null -o StrictHostKeyChecking=no -i $GIT_KEY_FILE\' git submodule update --init --recursive'
          sh 'echo ran update with $?'
          //}
        }
        writeFile file: 'environment.libsonnet',
            text: "{namespace: \"$JOB_NAME\"}"
        writeFile file: 'override.libsonnet',
            text: """{microservices+: { "exchange-rate"+: { configMounts+: { 'aws-cred': { mountPath: "/var/mineraltree/.aws", secret:: true } }, env_+:: { "HOME": "/var/mineraltree" }}}}"""
        withCredentials([file(credentialsId: 'eng-kube-config', variable: 'KUBECONFIG'),
                         string(credentialsId: 'aws-access-secret', variable: 'AWS_SECRET_ACCESS_KEY'),
                         string(credentialsId: 'aws-access-key', variable: 'AWS_ACCESS_KEY_ID')]) {
            sh 'bin/app-update deploy'
        }
      }
    }
    stage('test-kick') {
        steps {
            build job: 'exchange-rate-test', wait: false
        }
    }
  }
}

