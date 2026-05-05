package com.calico.tutor.ui.viewmodel

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.calico.tutor.di.ServiceLocator
import com.calico.tutor.ui.screen.DatabaseHelper
import com.calico.tutor.util.JwtUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed class ProfileState {
    object Idle : ProfileState()
    object Loading : ProfileState()
    data class Success(
        val userName: String,
        val userEmail: String,
        val profileImageUrl: String? = null,
        val rating: Double? = null,
        val bio: String? = null,
        val isOffline: Boolean = false
    ) : ProfileState()
    data class Error(val message: String) : ProfileState()
}

class ProfileViewModel(
    private val context: Context
) : ViewModel() {

    private val _profileState = MutableStateFlow<ProfileState>(ProfileState.Idle)
    val profileState: StateFlow<ProfileState> = _profileState.asStateFlow()

    private val tokenManager by lazy { ServiceLocator.provideTokenManager(context) }

    private fun isNetworkAvailable(): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    fun loadProfile() {
        viewModelScope.launch {
            _profileState.value = ProfileState.Loading

            val email = tokenManager.getEmail() ?: "User"
            val firebaseUid = tokenManager.getIdToken()?.let { JwtUtils.extractFirebaseUid(it) }
            
            // Check network once at the start
            val hasNetwork = isNetworkAvailable()
            Log.d("ProfileViewModel", "Network available: $hasNetwork")

// If no network, go directly to offline
            if (!hasNetwork) {
                loadFromCache(email, isOffline = true)
                return@launch
            }

            try {
                // Try online first
                val tutorResponse = withContext(Dispatchers.IO) {
                    if (firebaseUid != null) {
                        ServiceLocator.subjectsApiService(context).getTutorProfile(firebaseUid)
                    } else {
                        null
                    }
                }

                var profileImageUrl: String? = null

                if (tutorResponse != null) {
                    profileImageUrl = tutorResponse.profileImage ?: tutorResponse.profilePictureUrl
                    
                    // Save to SQLite for offline
                    withContext(Dispatchers.IO) {
                        try {
                            val profile = DatabaseHelper.TutorProfile(
                                name = tutorResponse.name,
                                email = tutorResponse.email,
                                subject = tutorResponse.courses?.firstOrNull(),
                                profileImageUrl = profileImageUrl
                            )
                            ServiceLocator.provideDatabaseHelper(context).saveTutorProfile(profile)
                            Log.d("ProfileViewModel", "✅ Saved profile to SQLite")
                        } catch (e: Exception) {
                            Log.e("ProfileViewModel", "❌ Failed to save profile: ${e.message}")
                        }
                    }
                }

                val userName = tutorResponse?.name 
                    ?: email.substringBefore("@").replaceFirstChar { 
                        if (it.isLowerCase()) it.titlecase() else it.toString() 
                    }

                _profileState.value = ProfileState.Success(
                    userName = userName,
                    userEmail = tutorResponse?.email ?: email,
                    profileImageUrl = profileImageUrl,
                    rating = tutorResponse?.rating,
                    bio = tutorResponse?.bio,
                    isOffline = false
                )

            } catch (e: Exception) {
                Log.w("ProfileViewModel", "❌ Online failed: ${e.message}")
                
                // We already confirmed network exists above, so use cached data without offline banner
                var profileImageUrl: String? = null
                var cachedName = ""
                var cachedEmail = ""
                
                withContext(Dispatchers.IO) {
                    try {
                        val dbHelper = ServiceLocator.provideDatabaseHelper(context)
                        val profiles = dbHelper.getTutorProfiles()
                        if (profiles.isNotEmpty()) {
                            val profile = profiles.first()
                            profileImageUrl = profile.profileImageUrl
                            cachedName = profile.name
                            cachedEmail = profile.email
                        }
                    } catch (e: Exception) {
                        // Ignore
                    }
                }

                val userName = cachedName.ifEmpty { 
                    email.substringBefore("@").replaceFirstChar { 
                        if (it.isLowerCase()) it.titlecase() else it.toString() 
                    }
                }

                // Network existed, just API failed - show as ONLINE
                _profileState.value = ProfileState.Success(
                    userName = userName,
                    userEmail = cachedEmail.ifEmpty { email },
                    profileImageUrl = profileImageUrl,
                    isOffline = false
                )
            }
        }
    }

    private suspend fun loadFromCache(email: String, isOffline: Boolean) {
        var profileImageUrl: String? = null
        var cachedName = ""
        var cachedEmail = ""
        
        withContext(Dispatchers.IO) {
            try {
                val dbHelper = ServiceLocator.provideDatabaseHelper(context)
                val profiles = dbHelper.getTutorProfiles()
                if (profiles.isNotEmpty()) {
                    val profile = profiles.first()
                    profileImageUrl = profile.profileImageUrl
                    cachedName = profile.name
                    cachedEmail = profile.email
                    Log.d("ProfileViewModel", "✅ Loaded from SQLite: $profileImageUrl")
                }
            } catch (e: Exception) {
                Log.e("ProfileViewModel", "❌ Failed to load from SQLite: ${e.message}")
            }
        }

        val userName = cachedName.ifEmpty { 
            email.substringBefore("@").replaceFirstChar { 
                if (it.isLowerCase()) it.titlecase() else it.toString() 
            }
        }

        _profileState.value = ProfileState.Success(
            userName = userName,
            userEmail = cachedEmail.ifEmpty { email },
            profileImageUrl = profileImageUrl,
            isOffline = isOffline
        )
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