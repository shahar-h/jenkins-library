package com.sap.piper.integration

class TransportManagementService implements Serializable {

    final Script script
    final Map config

    TransportManagementService(Script script, Map config) {
        this.script = script
        this.config = config
    }

    def retrieveOAuthToken(String oAuthUrl, String clientId, String clientSecret) {
        def encodedUsernameColonPassword = "${clientId}:${clientSecret}".bytes.encodeBase64().toString()
        this.script.echo "[${getClass().getSimpleName()}] Retrieving access token."

        this.script.echo "YYYYYYYYYY -> url: ${oAuthUrl}; clientId: ${clientId}; clientSecret: ${clientSecret}"

        def parameters = [
            url          : "${oAuthUrl}/oauth/token/?grant_type=client_credentials&response_type=token",
            httpMode     : "POST",
//            requestBody  : "grant_type=client_credentials&response_type=token",
            customHeaders: [
                [
                    maskValue: true,
                    name     : 'authorization',
                    value    : "Basic ${encodedUsernameColonPassword}"
                ]
//                [
//                    maskValue: false,
//                    name     : 'Content-Type',
//                    value    : 'application/x-www-form-urlencoded'
//                ],
            ],
        ]

        def response = sendApiRequest(parameters)

        return parseJson(response).access_token
    }
    
    private parseJson(text) {
        def parsedResponse = this.script.readJSON(text: text)
        if (this.config.verbose)
            this.script.echo "[${getClass().getSimpleName()}] " +
                "Parsed response is ${parsedResponse}"
        return parsedResponse
    }

    private sendApiRequest(parameters) {
        def defaultParameters = [
            acceptType            : 'APPLICATION_JSON',
            quiet                 : !this.config.verbose,
            consoleLogResponseBody: !this.config.verbose,
            ignoreSslErrors       : true,
            validResponseCodes    : "100:400"
        ]
        def response = this.script.httpRequest(defaultParameters + parameters)

        if (this.config.verbose)
            this.script.echo "[${getClass().getSimpleName()}] Received response " +
                "'${response.content}' with status ${response.status}."

        if (response.status == 400) {
            def message = "HTTP 400 - Bad request received as answer from Transport Management Service: " +
                "${response.content}"
            this.script.error message
        }

        return response.content
    }
}
