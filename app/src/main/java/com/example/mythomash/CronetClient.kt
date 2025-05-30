package com.example.mythomash

import android.content.Context
import android.util.Log
import org.chromium.net.CronetEngine
import org.chromium.net.UrlRequest
import org.chromium.net.UrlResponseInfo
import org.chromium.net.CronetException
import org.chromium.net.apihelpers.UploadDataProviders
import org.json.JSONObject
import java.nio.ByteBuffer
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit
import java.nio.charset.Charset

class CronetClient(context: Context) {

    // Cronet instance
    private val cronetEngine: CronetEngine = CronetEngine.Builder(context).build()

    // ResponseCallback interface
    interface ResponseCallback {
        fun onResponse(response: String)
        fun onError(error: String)
    }

    // This function initiates an HTTP request to the specified URL and handles the response asynchronously.
    fun makeRequest(url: String, callback: ResponseCallback, timeoutSeconds: Int = 1) {
        val responseBuilder = StringBuilder() // To accumulate the full response
        val executor: ScheduledExecutorService = Executors.newScheduledThreadPool(1)

        val request = cronetEngine.newUrlRequestBuilder(
            url,
            object : UrlRequest.Callback() {
                override fun onRedirectReceived(
                    request: UrlRequest,
                    info: UrlResponseInfo,
                    newLocationUrl: String
                ) {
                    request.followRedirect()
                }

                override fun onResponseStarted(
                    request: UrlRequest,
                    info: UrlResponseInfo
                ) {
                    Log.d("CronetClient","HTTP status code: ${info.httpStatusCode}")
                    request.read(ByteBuffer.allocateDirect(1024)) // 1 KB buffer
                }

                override fun onReadCompleted(
                    request: UrlRequest,
                    info: UrlResponseInfo,
                    byteBuffer: ByteBuffer
                ) {
                    // Flip the buffer to read the data
                    byteBuffer.flip()

                    // Get the chunk of data from the buffer
                    val bytes = ByteArray(byteBuffer.remaining())
                    byteBuffer.get(bytes)
                    val chunk = String(bytes, Charset.forName("UTF-8"))
                    Log.d("CronetClient","Received chunk: $chunk")
                    responseBuilder.append(chunk)

                    // After processing, read the next chunk
                    // It's essential to clear and prepare the buffer for the next read
                    byteBuffer.clear()
                    request.read(byteBuffer)
                }

                override fun onSucceeded(
                    request: UrlRequest,
                    info: UrlResponseInfo
                ) {
                    // All data received successfully, pass the full response
                    callback.onResponse(responseBuilder.toString())
                    executor.shutdown() // Shutdown the executor after the request completes
                }

                override fun onFailed(
                    request: UrlRequest,
                    info: UrlResponseInfo?,
                    error: CronetException
                ) {
                    // Handle failure
                    Log.d("CronetClient","Request failed: ${error.message}")
                    callback.onError(error.message ?: "Unknown error")
                    executor.shutdown() // Shutdown the executor after the request fails
                }
            },
            executor
        ).build()

        // Start the request
        request.start()

        // Schedule a task to cancel the request if it exceeds the timeout
        executor.schedule({
            if (!request.isDone) {
                request.cancel()
                callback.onError("Request timed out after $timeoutSeconds seconds")
                executor.shutdown() // Shutdown the executor after timeout
            }
        }, timeoutSeconds.toLong(), TimeUnit.SECONDS)
    }

    fun postImageData(
        url: String,
        base64Image: String,
        callback: ResponseCallback,
        timeoutSeconds: Int = 15
    ) {
        val executor: ScheduledExecutorService = Executors.newScheduledThreadPool(1)

        val jsonObject = JSONObject().apply {
            put("imageData", base64Image)
        }
        val jsonBody = jsonObject.toString()
        val byteBuffer = ByteBuffer.wrap(jsonBody.toByteArray(Charsets.UTF_8))

        val requestBuilder = cronetEngine.newUrlRequestBuilder(
            url,
            object : UrlRequest.Callback() {
                private val responseBuilder = StringBuilder()

                override fun onResponseStarted(request: UrlRequest, info: UrlResponseInfo) {
                    request.read(ByteBuffer.allocateDirect(1024))
                }

                override fun onReadCompleted(
                    request: UrlRequest,
                    info: UrlResponseInfo,
                    byteBuffer: ByteBuffer
                ) {
                    byteBuffer.flip()
                    val bytes = ByteArray(byteBuffer.remaining())
                    byteBuffer.get(bytes)
                    responseBuilder.append(String(bytes, Charsets.UTF_8))
                    byteBuffer.clear()
                    request.read(byteBuffer)
                }

                override fun onSucceeded(request: UrlRequest, info: UrlResponseInfo) {
                    callback.onResponse(responseBuilder.toString())
                    executor.shutdown()
                }

                override fun onFailed(request: UrlRequest, info: UrlResponseInfo?, error: CronetException) {
                    Log.e("CronetClient", "Request failed with exception", error)
                    callback.onError(error.message ?: "Unknown error")
                    executor.shutdown()
                }

                override fun onRedirectReceived(request: UrlRequest, info: UrlResponseInfo, newLocationUrl: String) {
                    request.followRedirect()
                }
            },
            executor
        )

        requestBuilder.setHttpMethod("POST")
        requestBuilder.addHeader("Content-Type", "application/json")
        requestBuilder.setUploadDataProvider(
            UploadDataProviders.create(byteBuffer),
            executor
        )

        val request = requestBuilder.build()
        request.start()

        executor.schedule({
            if (!request.isDone) {
                request.cancel()
                callback.onError("Request timed out after $timeoutSeconds seconds")
                executor.shutdown()
            }
        }, timeoutSeconds.toLong(), TimeUnit.SECONDS)
    }

    fun postJsonData(
        url: String,
        jsonData: String,
        callback: ResponseCallback,
        timeoutSeconds: Int = 15
    ) {
        val executor: ScheduledExecutorService = Executors.newScheduledThreadPool(1)

        val byteBuffer = ByteBuffer.wrap(jsonData.toByteArray(Charsets.UTF_8))

        val requestBuilder = cronetEngine.newUrlRequestBuilder(
            url,
            object : UrlRequest.Callback() {
                private val responseBuilder = StringBuilder()

                override fun onResponseStarted(request: UrlRequest, info: UrlResponseInfo) {
                    request.read(ByteBuffer.allocateDirect(1024))
                }

                override fun onReadCompleted(
                    request: UrlRequest,
                    info: UrlResponseInfo,
                    byteBuffer: ByteBuffer
                ) {
                    byteBuffer.flip()
                    val bytes = ByteArray(byteBuffer.remaining())
                    byteBuffer.get(bytes)
                    responseBuilder.append(String(bytes, Charsets.UTF_8))
                    byteBuffer.clear()
                    request.read(byteBuffer)
                }

                override fun onSucceeded(request: UrlRequest, info: UrlResponseInfo) {
                    callback.onResponse(responseBuilder.toString())
                    executor.shutdown()
                }

                override fun onFailed(request: UrlRequest, info: UrlResponseInfo?, error: CronetException) {
                    Log.e("CronetClient", "Request failed with exception", error)
                    callback.onError(error.message ?: "Unknown error")
                    executor.shutdown()
                }

                override fun onRedirectReceived(request: UrlRequest, info: UrlResponseInfo, newLocationUrl: String) {
                    request.followRedirect()
                }
            },
            executor
        )

        requestBuilder.setHttpMethod("POST")
        requestBuilder.addHeader("Content-Type", "application/json")
        requestBuilder.setUploadDataProvider(
            UploadDataProviders.create(byteBuffer),
            executor
        )

        val request = requestBuilder.build()
        request.start()

        executor.schedule({
            if (!request.isDone) {
                request.cancel()
                callback.onError("Request timed out after $timeoutSeconds seconds")
                executor.shutdown()
            }
        }, timeoutSeconds.toLong(), TimeUnit.SECONDS)
    }

}