package com.example.mythomash.ui.info

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import android.util.Log
import com.example.mythomash.CronetClient

class InfoViewModel(private val context: Context) : ViewModel() {

    private val _responseLiveData = MutableLiveData<String>()
    val responseLiveData: LiveData<String> get() = _responseLiveData

    private val _errorLiveData = MutableLiveData<String>()
    val errorLiveData: LiveData<String> get() = _errorLiveData

    private val cronetClient = CronetClient(context) // Use the context passed here

    // Function to initiate the request
    fun fetchData(url: String) {
        cronetClient.makeRequest(url, object : CronetClient.ResponseCallback {
            override fun onResponse(response: String) {
                if (response.isNullOrEmpty()) {
                    _errorLiveData.postValue("Empty response from server")
                } else {
                    Log.d("InfoViewModel", "Response: $response")
                    _responseLiveData.postValue(response)
                }
            }

            override fun onError(error: String) {
                Log.e("InfoViewModel", "Error: $error")
                _errorLiveData.postValue("Error: $error")
            }
        })
    }
}
