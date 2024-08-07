package com.example.contactapp.viewmodel

import android.app.Application
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import com.example.contactapp.Contact
import com.example.contactapp.ContactService

class ContactsViewModel(application: Application) : AndroidViewModel(application) {
    val contacts: LiveData<List<Contact>> = ContactService.contactsLiveData

    fun startFetchingContacts() {
        val context = getApplication<Application>().applicationContext
        val intent = Intent(context, ContactService::class.java)
        context.startService(intent)

    }

    fun addContact(firstName: String, lastName: String, phoneNumber: String, email: String) {
        val context = getApplication<Application>().applicationContext
        val intent = Intent(context, ContactService::class.java).apply {
            action = "ADD_CONTACT"
            putExtra("firstName", firstName)
            putExtra("lastName", lastName)
            putExtra("phoneNumber", phoneNumber)
            putExtra("email", email)
        }
        context.startService(intent)
    }

    fun deleteContact(contactId: String) {
        val context = getApplication<Application>().applicationContext
        val intent = Intent(context, ContactService::class.java).apply {
            action = "DELETE_CONTACT"
            putExtra("contactId", contactId)
        }
        context.startService(intent)
    }
}