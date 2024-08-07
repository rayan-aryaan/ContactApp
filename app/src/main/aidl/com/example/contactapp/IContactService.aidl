// IContactService.aidl
package com.example.contactapp;

import com.example.contactapp.Contact;


interface IContactService {
    List<Contact> getContacts();
    void addContact(in Contact contact);
    void deleteContact(String contactId);
}