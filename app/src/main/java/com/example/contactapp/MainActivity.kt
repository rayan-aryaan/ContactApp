package com.example.contactapp

import android.Manifest
import android.annotation.SuppressLint
import android.app.Application
import android.app.Service
import android.util.Log
import android.content.ContentResolver
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.database.ContentObserver
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.compose.rememberNavController
import android.provider.ContactsContract
import androidx.activity.viewModels
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.unit.DpOffset
import androidx.navigation.NavController
import androidx.navigation.navArgument
import androidx.lifecycle.*
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.contactapp.viewmodel.ContactsViewModel



@Composable
fun DisplayContactItem(contact: Contact, navController: NavController, contactsViewModel: ContactsViewModel, modifier: Modifier) {
    var expanded by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .padding(24.dp)
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = "${contact.firstName} ${contact.lastName}",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .clickable(onClick = { navController.navigate("contact_details/${contact.id}") })
                .weight(1f)
        )

        IconButton(onClick = { expanded = true }) {
            Icon(imageVector = Icons.Default.MoreVert, contentDescription = "More Options")
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            offset = DpOffset(x = (-30).dp, y = (-10).dp)
        ) {
            DropdownMenuItem(
                text = { Text("Delete") },
                onClick =
                {  contactsViewModel.deleteContact(contact.id)
                    expanded = false
                }
            )
        }
    }
}


@Composable
fun ContactDetailsScreen(contact: Contact, navController: NavController, contactsViewModel: ContactsViewModel) {
    Column(modifier = Modifier.padding(24.dp)) {
        IconButton(onClick = { navController.popBackStack() }) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back"
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = "${contact.firstName} ${contact.lastName}", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.height(8.dp))
        contact.phoneNumbers.forEach { phoneNumber ->
            Text(text = phoneNumber, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(4.dp))
        }
        contact.emails.forEach { email ->
            Text(text = email, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(4.dp))
        }
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = {
            contactsViewModel.deleteContact(contact.id)
            navController.popBackStack()
        }) {
            Text("Delete Contact")
        }
    }
}

@Composable
fun DisplayAllContacts(navController: NavController, contacts: List<Contact>, contactsViewModel: ContactsViewModel, modifier: Modifier) {
    Column {
        for(contact in contacts) {
            DisplayContactItem(contact = contact, navController = navController,
                contactsViewModel = contactsViewModel,
                modifier = modifier
            )
        }
    }

}
/*
fun addContact(context: Context, firstName: String, lastName: String, phoneNumber: String, email: String) {
    val contentResolver = context.contentResolver

    val contentValues = ContentValues()

    // Insert RawContact
    contentValues.put(ContactsContract.RawContacts.ACCOUNT_TYPE, null as String?)
    contentValues.put(ContactsContract.RawContacts.ACCOUNT_NAME, null as String?)
    val rawContactUri = contentResolver.insert(ContactsContract.RawContacts.CONTENT_URI, contentValues)
    val rawContactId = ContentUris.parseId(rawContactUri!!)

    // Insert Name
    contentValues.clear()
    contentValues.put(ContactsContract.Data.RAW_CONTACT_ID, rawContactId)
    contentValues.put(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE)
    contentValues.put(ContactsContract.CommonDataKinds.StructuredName.GIVEN_NAME, firstName)
    contentValues.put(ContactsContract.CommonDataKinds.StructuredName.FAMILY_NAME, lastName)
    contentResolver.insert(ContactsContract.Data.CONTENT_URI, contentValues)

    // Insert Phone Number
    contentValues.clear()
    contentValues.put(ContactsContract.Data.RAW_CONTACT_ID, rawContactId)
    contentValues.put(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE)
    contentValues.put(ContactsContract.CommonDataKinds.Phone.NUMBER, phoneNumber)
    contentValues.put(ContactsContract.CommonDataKinds.Phone.TYPE, ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE)
    contentResolver.insert(ContactsContract.Data.CONTENT_URI, contentValues)

    // Insert Email
    contentValues.clear()
    contentValues.put(ContactsContract.Data.RAW_CONTACT_ID, rawContactId)
    contentValues.put(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE)
    contentValues.put(ContactsContract.CommonDataKinds.Email.ADDRESS, email)
    contentValues.put(ContactsContract.CommonDataKinds.Email.TYPE, ContactsContract.CommonDataKinds.Email.TYPE_WORK)
    contentResolver.insert(ContactsContract.Data.CONTENT_URI, contentValues)
}

fun deleteContact(context: Context, contactId: String) {
    val contentResolver = context.contentResolver
    val deleteUri = Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_URI, contactId)
    contentResolver.delete(deleteUri, null, null)
}


 */
@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun ContactListScreen(navController: NavController, contacts: List<Contact>, contactsViewModel: ContactsViewModel, modifier: Modifier) {
    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { navController.navigate("add_contact") }) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Add Contact")
            }
        }
    ) {
        DisplayAllContacts(navController = navController, contacts = contacts, contactsViewModel = contactsViewModel, modifier = modifier)
    }
}
@Composable
fun AddContactScreen(contactsViewModel: ContactsViewModel, navController: NavController) {
    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var phoneNumber by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }

    Column(modifier = Modifier.padding(16.dp)) {
        IconButton(onClick = { navController.popBackStack() }) {
            Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
        }
        TextField(value = firstName, onValueChange = { firstName = it }, label = { Text("First Name") })
        TextField(value = lastName, onValueChange = { lastName = it }, label = { Text("Last Name") })
        TextField(value = phoneNumber, onValueChange = { phoneNumber = it }, label = { Text("Phone Number") })
        TextField(value = email, onValueChange = { email = it }, label = { Text("Email") })

        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = {
            contactsViewModel.addContact(firstName, lastName, phoneNumber, email)
            navController.popBackStack()
        }) {
            Text("Add Contact")
        }
    }
}

@Composable
fun MyContactApp(contactsViewModel: ContactsViewModel = viewModel()) {
    val contacts by contactsViewModel.contacts.observeAsState(emptyList())
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = "contact_list") {
        composable("contact_list") {
            ContactListScreen(navController = navController, contacts = contacts, contactsViewModel = contactsViewModel, modifier = Modifier)
        }
        composable(
            route = "contact_details/{contactId}",
            arguments = listOf(navArgument("contactId") { type = NavType.StringType })
        ) { backStackEntry ->
            val contactId = backStackEntry.arguments?.getString("contactId") ?: ""
            val contact = contacts.find { it.id == contactId }
            contact?.let {
                ContactDetailsScreen(contact = it, navController = navController, contactsViewModel = contactsViewModel)
            }
        }
        composable("add_contact") {
            AddContactScreen(contactsViewModel = contactsViewModel, navController = navController)
        }
    }
}


@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    MyContactApp()
}

class MainActivity : ComponentActivity() {
    private val TAG : String = MainActivity::class.simpleName.toString()

    private val contactsViewModel: ContactsViewModel by viewModels()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MyContactApp(contactsViewModel)
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_CONTACTS, Manifest.permission.WRITE_CONTACTS), 1)
        } else {
            contactsViewModel.startFetchingContacts()
        }

    }

    override fun onDestroy() {
        super.onDestroy()
        val intent = Intent(this, ContactService::class.java)
        stopService(intent)
    }

    @Deprecated("This method has been deprecated in favor of using the Activity Result API\n      which brings increased type safety via an {@link ActivityResultContract} and the prebuilt\n      contracts for common intents available in\n      {@link androidx.activity.result.contract.ActivityResultContracts}, provides hooks for\n      testing, and allow receiving results in separate, testable classes independent from your\n      activity. Use\n      {@link #registerForActivityResult(ActivityResultContract, ActivityResultCallback)} passing\n      in a {@link RequestMultiplePermissions} object for the {@link ActivityResultContract} and\n      handling the result in the {@link ActivityResultCallback#onActivityResult(Object) callback}.")
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            contactsViewModel.startFetchingContacts()
        }
        else{
            Log.i(TAG, "PERMISSION DENIED")
        }
    }
}





