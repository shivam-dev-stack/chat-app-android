package com.example.chatbot

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.google.ai.client.generativeai.GenerativeModel
import kotlinx.coroutines.launch


class ChatViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: MessageRepository
    val allMessages: LiveData<List<Message>>

    private val generativeModel = GenerativeModel(
        modelName = "gemini-2.5-flash",
        apiKey = BuildConfig.API_KEY // Replace with your Gemini API key
    )

    init {
        val messageDao = AppDatabase.getDatabase(application).messageDao()
        repository = MessageRepository(messageDao)
        allMessages = repository.allMessages.asLiveData()
    }

    fun sendMessage(userMessage: String) {
        viewModelScope.launch {
            // Save user message
            val userMsg = Message(content = userMessage, isUser = true)
            repository.insert(userMsg)

            try {
                // Generate AI response
                val response = generativeModel.generateContent(userMessage)
                val aiMessage = response.text ?: "Sorry, I couldn't generate a response."

                // Save AI response
                val aiMsg = Message(content = aiMessage, isUser = false)
                repository.insert(aiMsg)
            } catch (e: Exception) {
                // Save error message
                val errorMsg = Message(
                    content = "Error: ${e.message ?: "Unknown error occurred"}",
                    isUser = false
                )
                repository.insert(errorMsg)
            }
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            repository.clearHistory()
        }
    }
}