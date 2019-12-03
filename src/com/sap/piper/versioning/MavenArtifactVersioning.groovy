package com.sap.piper.versioning

class MavenArtifactVersioning extends ArtifactVersioning {
    protected MavenArtifactVersioning (script, configuration) {
        super(script, configuration)
    }

    @Override
    def getVersion() {
        script.dockerExecute(script: script, dockerImage: 'maven:3-jdk-8') {
            def version = script.sh script: 'mvn org.apache.maven.plugins:maven-help-plugin:3.1.0:evaluate -Dexpression=project.version -q -DforceStdout', returnStdout: true
            return version.replaceAll(/-SNAPSHOT$/, "")
        }
        error "Could not read maven version"
    }

    @Override
    def setVersion(version) {
        script.mavenExecute script: script, goals: 'versions:set', defines: "-DnewVersion=${version} -DgenerateBackupPoms=false", pomPath: configuration.filePath
    }
}
