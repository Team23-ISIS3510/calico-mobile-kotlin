package com.calico.tutor.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.calico.tutor.di.ServiceLocator
import com.calico.tutor.data.dto.response.TutorResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.URL

sealed class ProfileState {
    object Idle : ProfileState()
    object Loading : ProfileState()
    data class Success(
        val userName: String,
        val userEmail: String,
        val profileImageUrl: String? = null,
        val cachedPicturePath: String? = null,
        val rating: Double? = null,
        val bio: String? = null
    ) : ProfileState()
    data class Error(val message: String) : ProfileState()
}

class ProfileViewModel(
    private val context: Context
) : ViewModel() {

    private val _profileState = MutableStateFlow<ProfileState>(ProfileState.Idle)
    val profileState: StateFlow<ProfileState> = _profileState.asStateFlow()

    private val tokenManager by lazy { ServiceLocator.provideTokenManager(context) }

    fun loadProfile() {
        viewModelScope.launch {
            _profileState.value = ProfileState.Loading

            try {
                val email = tokenManager.getEmail() ?: "User"
                val firebaseUid = tokenManager.getFirebaseUid()

                val tutorResponse = withContext(Dispatchers.IO) {
                    try {
                        firebaseUid?.let { uid ->
                            ServiceLocator.subjectsApiService(context).getTutorProfile(uid)
                        }
                    } catch (e: Exception) {
                        null
                    }
                }

                var cachedPicturePath: String? = null

                // Cache profile picture if URL exists
                if (!tutorResponse?.profileImage.isNullOrEmpty()) {
                    cachedPicturePath = withContext(Dispatchers.IO) {
                        cacheProfilePicture(tutorResponse!!.profileImage)
                    }
                }

                val userName = tutorResponse?.name 
                    ?: email.substringBefore("@").replaceFirstChar { 
                        if (it.isLowerCase()) it.titlecase() else it.toString() 
                    }

                _profileState.value = ProfileState.Success(
                    userName = userName,
                    userEmail = tutorResponse?.email ?: email,
                    profileImageUrl = tutorResponse?.profileImage,
                    cachedPicturePath = cachedPicturePath,
                    rating = tutorResponse?.rating,
                    bio = tutorResponse?.bio
                )

            } catch (e: Exception) {
                val email = tokenManager.getEmail() ?: "User"
                val userName = email.substringBefore("@").replaceFirstChar { 
                    if (it.isLowerCase()) it.titlecase() else it.toString() 
                }

                _profileState.value = ProfileState.Success(
                    userName = userName,
                    userEmail = email
                )
            }
        }
    }

    private fun cacheProfilePicture(url: String?): String? {
        if (url.isNullOrEmpty()) return null

        return try {
            val cacheDir = context.cacheDir
            val file = File(cacheDir, "profile_temp.jpg")

            if (file.exists()) {
                return file.absolutePath
            }

            val inputStream = URL(url).openStream()
            val outputStream = FileOutputStream(file)

            inputStream.use { input ->
                outputStream.use { output ->
                    input.copyTo(output)
                }
            }

            file.absolutePath
        } catch (e: Exception) {
            null
        }
    }

    fun cacheProfilePictureFromUrl(url: String?) {
        if (url.isNullOrEmpty()) return

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val cacheDir = context.cacheDir
                val file = File(cacheDir, "profile_temp.jpg")

                val inputStream = URL(url).openStream()
                val outputStream = FileOutputStream(file)

                inputStream.use { input ->
                    outputStream.use { output ->
                        input.copyTo(output)
                    }
                }

                withContext(Dispatchers.Main) {
                    val currentState = _profileState.value
                    if (currentState is ProfileState.Success) {
                        _profileState.value = currentState.copy(cachedPicturePath = file.absolutePath)
                    }
                }
            } catch (e: Exception) {
                // Silently fail
            }
        }
    }

    fun getCachedProfilePicture(): File? {
        val cacheDir = context.cacheDir
        val file = File(cacheDir, "profile_temp.jpg")
        return if (file.exists()) file else null
    }

    fun clearCache() {
        viewModelScope.launch(Dispatchers.IO) {
            val cacheDir = context.cacheDir
            val file = File(cacheDir, "profile_temp.jpg")
            if (file.exists()) {
                file.delete()
            }
        }
    }
}

class ProfileViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ProfileViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ProfileViewModel(context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}