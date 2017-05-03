node('label_name') {

  ws('/var/jenkins/workspace') {
    stage('Refresh Workspace') {
      checkout([$class: 'GitSCM', branches: [[name: "${GIT_COMMIT}"]], doGenerateSubmoduleConfigurations: false, extensions: [], submoduleCfg: [], userRemoteConfigs: [[credentialsId: 'ef6e0514-d07c-4c69-a704-4721fa12a756', url: "${GIT_URL}"]]])

      withEnv(["GIT_COMMIT=${GIT_COMMIT}"]) {
        sh('/bin/ant -f deploy/jenkins.xml build -Dbuild=${BUILD_NUMBER} -Drevision=${GIT_COMMIT}')
      }
    }

    stage('PHP Unit Tests') {
      withEnv(["GIT_COMMIT=${GIT_COMMIT}"]) {
        try {
          sh('/bin/ant -f deploy/jenkins.xml test-unit -Dbuild=${BUILD_NUMBER} -Drevision=${GIT_COMMIT} -DUSE_PDO=true')
        } finally {
          step([$class: 'JUnitResultArchiver', testResults: 'test_framework/log.junit.xml'])
        }
      }
    }

    stage('UI Tests') {
      try {
        sh('/bin/ant -f deploy/jenkins.xml test-jasmine -Djasmine.url="http://localhost/admin/jasmine"')
      } finally {
        step([$class: 'JUnitResultArchiver', testResults: 'test_framework/log.jasmine-junit.xml'])
        archiveArtifacts('test_framework/log.jasmine-junit.xml');
      }
    }

    stage('Package / Deploy') {
      withEnv(["GIT_COMMIT=${GIT_COMMIT}"]) {
        sh('/bin/ant -v -f deploy/jenkins.xml rpm-deploy-trunk -Dlabel="${BUILD_NUMBER}" -Drelease.id=${BUILD_NUMBER}-${GIT_COMMIT}.0 -Drelease.activate.options=""')
      }
    }
  }
}
