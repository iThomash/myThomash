package com.example.mythomash.ui.info

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import android.util.Log
import com.example.mythomash.CronetClient
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class InfoViewModelFactory(
    private val context: Context,
    private val initialData: Map<String, String>


) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(InfoViewModel::class.java)) {
            val viewModel = InfoViewModel(context)
            viewModel.setData(initialData)
            return viewModel as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}


class InfoViewModel(private val context: Context) : ViewModel() {

    private val _responseLiveData = MutableLiveData<String>()
    val responseLiveData: LiveData<String> get() = _responseLiveData

    private val _errorLiveData = MutableLiveData<String>()
    val errorLiveData: LiveData<String> get() = _errorLiveData

    private val _infoData = MutableLiveData<Map<String, String>>()
    val infoData: LiveData<Map<String, String>> get() = _infoData

    private val cronetClient = CronetClient(context)

    fun fetchData(url: String) {
        cronetClient.makeRequest(url, object : CronetClient.ResponseCallback {
            override fun onResponse(response: String) {
                if (response.isNullOrEmpty()) {
                    _errorLiveData.postValue("Empty response from server")
                } else {
                    _responseLiveData.postValue(response)
                }
            }

            override fun onError(error: String) {
                _errorLiveData.postValue("Error: $error")
            }
        })
    }

    fun setData(data: Map<String, String>) {
        _infoData.postValue(data)
    }
}
