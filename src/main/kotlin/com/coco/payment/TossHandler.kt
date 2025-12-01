package com.coco.payment

import org.springframework.stereotype.Component
import java.io.IOException
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets
import java.util.*


@Component
class TossHandler {

//    @Throws(IOException::class)
//    private fun sendRequest(requestData: JSONObject, secretKey: String?, urlString: String): JSONObject {
//        val connection = createConnection(secretKey, urlString)
//        connection.getOutputStream().use { os ->
//            os.write(requestData.toString().getBytes(StandardCharsets.UTF_8))
//        }
//        try {
//            if (connection.getResponseCode() == 200) connection.getInputStream() else connection.getErrorStream()
//                .use { responseStream ->
//                    InputStreamReader(responseStream, StandardCharsets.UTF_8).use { reader ->
//                        return JSONParser().parse(reader) as JSONObject
//                    }
//                }
//        } catch (e: Exception) {
//            logger.error("Error reading response", e)
//            val errorResponse: JSONObject = JSONObject()
//            errorResponse.put("error", "Error reading response")
//            return errorResponse
//        }
//    }
//
//    @Throws(IOException::class)
//    private fun createConnection(secretKey: String?, urlString: String): HttpURLConnection {
//        val url = URL(urlString)
//        val connection = url.openConnection() as HttpURLConnection
//        connection.setRequestProperty(
//            "Authorization",
//            "Basic " + Base64.getEncoder().encodeToString((secretKey + ":").toByteArray(StandardCharsets.UTF_8))
//        )
//        connection.setRequestProperty("Content-Type", "application/json")
//        connection.setRequestMethod("POST")
//        connection.setDoOutput(true)
//        return connection
//    }
}