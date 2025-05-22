package com.example.mythomash.ui.backup

import android.graphics.Bitmap
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class BackupViewModel : ViewModel() {

    private val _selectedImage = MutableLiveData<Bitmap?>()
    val selectedImage: LiveData<Bitmap?> = _selectedImage

    private val _uploadStatus = MutableLiveData<String>()
    val uploadStatus: LiveData<String> = _uploadStatus

    fun setImage(bitmap: Bitmap) {
        _selectedImage.value = bitmap
    }

    fun clearImage() {
        _selectedImage.value = null
    }

    fun setUploadStatus(message: String) {
        _uploadStatus.postValue(message)
    }
}
