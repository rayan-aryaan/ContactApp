package com.example.contactapp

import android.app.Service
import android.content.ContentResolver
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Handler
import android.os.IBinder
import android.provider.ContactsContract
import android.util.Log
import androidx.lifecycle.MutableLiveData
import com.example.contactapp.observer.ContactObserver

class ContactService : Service() {

    private val binder = object : IContactService.Stub() {
        override fun getContacts(): List<Contact> {
            return getContactsFromDevice()
        }

        override fun addContact(contact: Contact) {
            addContactToDevice(applicationContext, contact)
        }

        override fun deleteContact(contactId: String) {
            deleteContactFromDevice(applicationContext, contactId)
        }
    }

    override fun onBind(intent: Intent?): IBinder? {
        return binder
    }

    private fun getContactsFromDevice(): List<Contact> {
        return getContacts(applicationContext)
    }

    companion object {
        val contactsLiveData = MutableLiveData<List<Contact>>()
    }

    private lateinit var contactObserver: ContactObserver

    override fun onCreate() {
        super.onCreate()
        contactObserver = ContactObserver(Handler(), applicationContext) {
            fetchContacts()
        }
        contactObserver.register()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            "ADD_CONTACT" -> {
                val firstName = intent.getStringExtra("firstName") ?: ""
                val lastName = intent.getStringExtra("lastName") ?: ""
                val phoneNumber = intent.getStringExtra("phoneNumber") ?: ""
                val email = intent.getStringExtra("email") ?: ""
                Log.d("ContactService", "Adding contact: $firstName $lastName, $phoneNumber, $email")
                addContactToDevice(applicationContext, Contact("", firstName, lastName, listOf(phoneNumber), listOf(email)))
            }
            "DELETE_CONTACT" -> {
                val contactId = intent.getStringExtra("contactId") ?: ""
                Log.d("ContactService", "Deleting contact ID: $contactId")
                deleteContactFromDevice(applicationContext, contactId)
            }
            else -> {
                Log.d("ContactService", "Fetching contacts")
                fetchContacts()
            }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        contactObserver.unregister()
    }

    private fun fetchContacts() {
        val thread = Thread {
            try {
                val contacts = getContacts(applicationContext)
                contactsLiveData.postValue(contacts)
            } catch (e: Exception) {
                Log.e("ContactService", "Error fetching contacts", e)
            }
        }
        thread.start()
    }

    private fun getContacts(context: Context): List<Contact> {
        val contactList = mutableListOf<Contact>()
        val contentResolver = context.contentResolver

        val cursor = contentResolver.query(
            ContactsContract.Contacts.CONTENT_URI,
            null,
            null,
            null,
            ContactsContract.Contacts.DISPLAY_NAME + " ASC"
        )

        cursor?.use {
            val idIndex = cursor.getColumnIndex(ContactsContract.Contacts._ID)
            val hasPhoneNumberIndex = cursor.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER)

            while (cursor.moveToNext()) {
                val contactId = cursor.getString(idIndex)
                val hasPhoneNumber = cursor.getInt(hasPhoneNumberIndex) > 0

                val firstName = getStructuredName(contentResolver, contactId, ContactsContract.CommonDataKinds.StructuredName.GIVEN_NAME) ?: ""
                val lastName = getStructuredName(contentResolver, contactId, ContactsContract.CommonDataKinds.StructuredName.FAMILY_NAME) ?: ""

                val phoneNumbers = if (hasPhoneNumber) {
                    getPhoneNumbers(contentResolver, contactId)
                } else {
                    emptyList()
                }

                val emails = getEmails(contentResolver, contactId)

                contactList.add(Contact(contactId, firstName, lastName, phoneNumbers, emails))
            }
        }

        return contactList
    }

    private fun getStructuredName(contentResolver: ContentResolver, contactId: String, columnName: String): String? {
        val nameCursor = contentResolver.query(
            ContactsContract.Data.CONTENT_URI,
            null,
            "${ContactsContract.Data.CONTACT_ID} = ? AND ${ContactsContract.Data.MIMETYPE} = ?",
            arrayOf(contactId, ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE),
            null
        )

        var result: String? = null
        nameCursor?.use {
            val nameIndex = it.getColumnIndex(columnName)
            if (it.moveToFirst()) {
                result = it.getString(nameIndex)
            }
        }
        return result
    }

    private fun getPhoneNumbers(contentResolver: ContentResolver, contactId: String): List<String> {
        val phoneNumbers = mutableListOf<String>()
        val phoneCursor = contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            null,
            ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?",
            arrayOf(contactId),
            null
        )

        phoneCursor?.use {
            val phoneNumberIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
            while (it.moveToNext()) {
                phoneNumbers.add(it.getString(phoneNumberIndex))
            }
        }
        return phoneNumbers
    }

    private fun getEmails(contentResolver: ContentResolver, contactId: String): List<String> {
        val emails = mutableListOf<String>()
        val emailCursor = contentResolver.query(
            ContactsContract.CommonDataKinds.Email.CONTENT_URI,
            null,
            ContactsContract.CommonDataKinds.Email.CONTACT_ID + " = ?",
            arrayOf(contactId),
            null
        )

        emailCursor?.use {
            val emailIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Email.ADDRESS)
            while (it.moveToNext()) {
                emails.add(it.getString(emailIndex))
            }
        }
        return emails
    }

    private fun addContactToDevice(context: Context, contact: Contact) {
        val thread = Thread {
            try {
                val contentResolver = context.contentResolver
                val contentValues = ContentValues()

                // Insert RawContact
                contentValues.put(ContactsContract.RawContacts.ACCOUNT_TYPE, null as String?)
                contentValues.put(ContactsContract.RawContacts.ACCOUNT_NAME, null as String?)
                val rawContactUri = contentResolver.insert(ContactsContract.RawContacts.CONTENT_URI, contentValues)
                val rawContactId = ContentUris.parseId(rawContactUri!!)

                Log.d("ContactService", "Inserted RawContact with ID: $rawContactId")

                // Insert Name
                contentValues.clear()
                contentValues.put(ContactsContract.Data.RAW_CONTACT_ID, rawContactId)
                contentValues.put(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE)
                contentValues.put(ContactsContract.CommonDataKinds.StructuredName.GIVEN_NAME, contact.firstName)
                contentValues.put(ContactsContract.CommonDataKinds.StructuredName.FAMILY_NAME, contact.lastName)
                val nameUri = contentResolver.insert(ContactsContract.Data.CONTENT_URI, contentValues)
                Log.d("ContactService", "Inserted Name: $nameUri")

                // Insert Phone Numbers
                for (phoneNumber in contact.phoneNumbers) {
                    contentValues.clear()
                    contentValues.put(ContactsContract.Data.RAW_CONTACT_ID, rawContactId)
                    contentValues.put(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE)
                    contentValues.put(ContactsContract.CommonDataKinds.Phone.NUMBER, phoneNumber)
                    contentValues.put(ContactsContract.CommonDataKinds.Phone.TYPE, ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE)
                    val phoneUri = contentResolver.insert(ContactsContract.Data.CONTENT_URI, contentValues)
                    Log.d("ContactService", "Inserted Phone Number: $phoneUri")
                }

                // Insert Emails
                for (email in contact.emails) {
                    contentValues.clear()
                    contentValues.put(ContactsContract.Data.RAW_CONTACT_ID, rawContactId)
                    contentValues.put(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE)
                    contentValues.put(ContactsContract.CommonDataKinds.Email.ADDRESS, email)
                    contentValues.put(ContactsContract.CommonDataKinds.Email.TYPE, ContactsContract.CommonDataKinds.Email.TYPE_WORK)
                    val emailUri = contentResolver.insert(ContactsContract.Data.CONTENT_URI, contentValues)
                    Log.d("ContactService", "Inserted Email: $emailUri")
                }

                fetchContacts() // Refresh the contacts list after adding a contact
            } catch (e: Exception) {
                Log.e("ContactService", "Error adding contact", e)
            }
        }
        thread.start()
    }


    private fun deleteContactFromDevice(context: Context, contactId: String) {
        val thread = Thread {
            try {
                val contentResolver = context.contentResolver
                val deleteUri = Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_URI, contactId)
                contentResolver.delete(deleteUri, null, null)
                fetchContacts() // Refresh the contacts list after deleting a contact
            } catch (e: Exception) {
                Log.e("ContactService", "Error deleting contact", e)
            }
        }
        thread.start()
    }
}
