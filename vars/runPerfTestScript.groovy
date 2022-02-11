void call(Map args = [:]) {
    String jobName = args.jobName ?: 'distribution-build-opensearch'
    lib = library(identifier: 'jenkins@20211123', retriever: legacySCM(scm))
    def buildManifest = lib.jenkins.BuildManifest.new(readYaml(file: args.buildManifest))
    String artifactRootUrl = buildManifest.getArtifactRootUrl(jobName, args.buildId)
    echo "Artifact root URL: ${artifactRootUrl}"
    echo "${args}"
    echo "${args.buildManifest}"
    echo "${buildManifest}"


    install_npm()
    install_dependencies()
    withAWS(role: 'opensearch-test', roleAccount: "${AWS_ACCOUNT_PUBLIC}", duration: 900, roleSessionName: 'jenkins-session') {
        s3Download(file: "config.yml", bucket: "${ARTIFACT_BUCKET_NAME}", path: "${PERF_TEST_CONFIG_LOCATION}/config.yml", force: true)
        s3Download(file: "manifest.yml", bucket: "${ARTIFACT_BUCKET_NAME}", path: "${PERF_TEST_MANIFEST_LOCATION}/manifest.yml", force: true)
    }

    sh([
        './test.sh',
        'perf-test',
        "--stack test-single-disabled",
        "--bundle-manifest manifest.yml",
        "--config config.yml"

    ].join(' '))
}

void install_npm(){
    sh'''
        sudo yum install -y gcc-c++ make
        curl -sL https://rpm.nodesource.com/setup_16.x | sudo -E bash -
        sudo yum install -y nodejs --enablerepo=nodesource
        node -v
      '''
}

void install_dependencies() {
    sh '''
        sudo npm install -g aws-cdk
        sudo npm install -g cdk-assume-role-credential-plugin
    '''
}