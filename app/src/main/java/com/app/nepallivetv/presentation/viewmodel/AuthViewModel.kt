package com.app.nepallivetv.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.nepallivetv.data.local.datastore.DatastorePreferences
import com.app.nepallivetv.data.model.LoginRequest
import com.app.nepallivetv.data.model.RegisterRequest
import com.app.nepallivetv.data.remote.LiveTvApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import retrofit2.HttpException
import org.json.JSONObject

sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    object Success : AuthState()
    data class Error(val message: String) : AuthState()
}

class AuthViewModel(
    private val api: LiveTvApi,
    private val datastorePreferences: DatastorePreferences
) : ViewModel() {

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState

    val isLoggedIn = datastorePreferences.authTokenFlow
    
    private fun parseError(e: Exception): String {
        return if (e is HttpException) {
            try {
                val errorBodyString = e.response()?.errorBody()?.string()
                if (errorBodyString != null) {
                    val json = JSONObject(errorBodyString)
                    json.optString("detail", "An error occurred")
                } else {
                    e.message ?: "An error occurred"
                }
            } catch (ex: Exception) {
                e.message ?: "An error occurred"
            }
        } else {
            e.message ?: "An error occurred"
        }
    }

    fun register(name: String, email: String, phone: String, pass: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                api.register(RegisterRequest(name, email, phone, pass))
                _authState.value = AuthState.Success
            } catch (e: Exception) {
                _authState.value = AuthState.Error(parseError(e))
            }
        }
    }

    fun login(loginId: String, pass: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                val res = api.login(LoginRequest(loginId, pass))
                datastorePreferences.saveAuthData(res.accessToken, res.userName, res.email)
                _authState.value = AuthState.Success
            } catch (e: Exception) {
                _authState.value = AuthState.Error(parseError(e))
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            datastorePreferences.clearAuthData()
        }
    }
    
    fun resetState() {
        _authState.value = AuthState.Idle
    }
}