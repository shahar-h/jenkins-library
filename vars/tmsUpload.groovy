import com.sap.piper.integration.TransportManagementService

import static com.sap.piper.Prerequisites.checkScript

import com.sap.piper.GenerateDocumentation
import com.sap.piper.ConfigurationHelper
import groovy.transform.Field

@Field def STEP_NAME = getClass().getName()

@Field Set GENERAL_CONFIG_KEYS = []
@Field Set STEP_CONFIG_KEYS = [
    /**
     * The archive file to be uploaded.
     * @mandatory
     */
    'deployArchive',
    /**
     * The node name in the TMS system to upload the archive to.
     * @mandatory
     */
    'nodeName',
    /**
     * The service key from the TMS system. Needed for authentication.
     * @mandatory
     */
    'tmsServiceKeyId',
    /**
     * Description for export of deploy archive.
     */
    'description'
]
@Field Set PARAMETER_KEYS = STEP_CONFIG_KEYS

/**
 * Upload archive to TMS.
 */
@GenerateDocumentation
void call(Map parameters = [:]) {
    handlePipelineStepErrors(stepName: STEP_NAME, stepParameters: parameters) {

        final script = checkScript(this, parameters) ?: this

        // load default & individual configuration
        Map config = ConfigurationHelper.newInstance(this)
            .loadStepDefaults()
            .mixinGeneralConfig(script.commonPipelineEnvironment, GENERAL_CONFIG_KEYS)
            .mixinStepConfig(script.commonPipelineEnvironment, STEP_CONFIG_KEYS)
            .mixinStageConfig(script.commonPipelineEnvironment, parameters.stageName ?: env.STAGE_NAME, STEP_CONFIG_KEYS)
            .mixin(parameters, PARAMETER_KEYS)
            .use()

        //TODO: how to make sure mandatory parameters are present?
        def tms = new TransportManagementService(script, config)

        withCredentials([string(credentialsId: config.tmsServiceKeyId, variable: 'decryptedServiceKey')]) {
            def serviceKey = readJSON text: decryptedServiceKey
            echo "XXXXXXXXXX: ${decryptedServiceKey}"
            def token = tms.retrieveOAuthToken(serviceKey.uaa.url, serviceKey.uaa.clientid, serviceKey.uaa.clientsecret)

//            def fileId = uploadDeployArchive(serviceKey.uri, token, config.deployArchive)
//            exportDeployArchive()

        }

    }
}

//def retrieveOAuthToken(baseUrl, clientId, clientSecret) {
//    echo "[${STEP_NAME}] Retrieving access token."
//
//    def url = "${baseUrl}/oauth/token/?grant_type=client_credentials&response_type=token"
//    def encodedClientIdSecret = "${clientId}:${clientSecret}".bytes.encodeBase64().toString()
//
//    def response = httpRequest httpMode: 'POST',
//        url: url,
//        customHeaders: [[maskValue: true, name: 'Authorization', value: "Basic ${encodedClientIdSecret}"]]
//
//    result = readJSON text: response.content
//    def token = result.access_token
//    if (!token) {
//        error "[${STEP_NAME}] Failed to extract OAuth token from Oauth Client reply. JSON key 'access_token' does not exist."
//    } else {
//        return token
//    }
//}

def uploadDeployArchive(baseUrl, token, deployArchive) {
    echo "Upload '${deployArchive}' to Transport Service ..."

    def uploadResult = sh(returnStdout: true,
                    script: """curl --silent --show-error --retry 12 -o response --write-out \"%{http_code}\" \
                                    -XPOST -H "Authorization: Bearer ${token}" \
                                    -F file=@\"${deployArchive}\" \"${baseUrl}/v1/files/upload\"""")

    result = readJSON text: uploadResult
    def fileId = result.fileId
    if (!fileId) {
        error "[${STEP_NAME}] ERROR: MTA file upload failed. Details: '${uploadResult}'"
    } else {
        echo "Upload SUCCESS - file id: '${fileId}'"
        return fileId
    }
}

def exportDeployArchive(baseUrl, token, fileId, nodeName, description) {
    echo "Export to node '${nodeName}' ..."

    exportData = """{
                        'description': '$description',
                        'nodeName': '$nodeName',
                        'entries': [{
                            'content_type': 'MTA',
                            'storage_type': 'FILE',
                            'uri': '$fileId'
                        }]
                    }"""
}
