package com.pureqr.app.util

import android.content.Context
import android.net.Uri
import android.provider.ContactsContract
import com.pureqr.app.model.ContactData

object ContactHelper {
    fun getContactData(context: Context, contactUri: Uri): ContactData? {
        val contentResolver = context.contentResolver
        val projection = arrayOf(
            ContactsContract.Contacts._ID,
            ContactsContract.Contacts.DISPLAY_NAME
        )

        return contentResolver.query(contactUri, projection, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val id = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.Contacts._ID))
                val name = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.Contacts.DISPLAY_NAME))

                var phone = ""
                contentResolver.query(
                    ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                    null,
                    "${ContactsContract.CommonDataKinds.Phone.CONTACT_ID} = ?",
                    arrayOf(id),
                    null
                )?.use { pc ->
                    if (pc.moveToFirst()) {
                        phone = pc.getString(pc.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER)) ?: ""
                    }
                }

                var email = ""
                contentResolver.query(
                    ContactsContract.CommonDataKinds.Email.CONTENT_URI,
                    null,
                    "${ContactsContract.CommonDataKinds.Email.CONTACT_ID} = ?",
                    arrayOf(id),
                    null
                )?.use { ec ->
                    if (ec.moveToFirst()) {
                        email = ec.getString(ec.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Email.ADDRESS)) ?: ""
                    }
                }

                var organization = ""
                var jobTitle = ""
                contentResolver.query(
                    ContactsContract.Data.CONTENT_URI,
                    null,
                    "${ContactsContract.Data.CONTACT_ID} = ? AND ${ContactsContract.Data.MIMETYPE} = ?",
                    arrayOf(id, ContactsContract.CommonDataKinds.Organization.CONTENT_ITEM_TYPE),
                    null
                )?.use { oc ->
                    if (oc.moveToFirst()) {
                        organization = oc.getString(oc.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Organization.COMPANY)) ?: ""
                        jobTitle = oc.getString(oc.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Organization.TITLE)) ?: ""
                    }
                }

                var website = ""
                contentResolver.query(
                    ContactsContract.Data.CONTENT_URI,
                    null,
                    "${ContactsContract.Data.CONTACT_ID} = ? AND ${ContactsContract.Data.MIMETYPE} = ?",
                    arrayOf(id, ContactsContract.CommonDataKinds.Website.CONTENT_ITEM_TYPE),
                    null
                )?.use { wc ->
                    if (wc.moveToFirst()) {
                        website = wc.getString(wc.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Website.URL)) ?: ""
                    }
                }

                var address = ""
                contentResolver.query(
                    ContactsContract.CommonDataKinds.StructuredPostal.CONTENT_URI,
                    null,
                    "${ContactsContract.CommonDataKinds.StructuredPostal.CONTACT_ID} = ?",
                    arrayOf(id),
                    null
                )?.use { ac ->
                    if (ac.moveToFirst()) {
                        address = ac.getString(ac.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.StructuredPostal.FORMATTED_ADDRESS)) ?: ""
                    }
                }

                val nameParts = name.split(" ", limit = 2)
                ContactData(
                    firstName = nameParts.getOrNull(0) ?: "",
                    lastName = nameParts.getOrNull(1) ?: "",
                    phone = phone,
                    email = email,
                    organization = organization,
                    jobTitle = jobTitle,
                    website = website,
                    address = address
                )
            } else null
        }
    }
}
