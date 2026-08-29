package com.example.bridge

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.provider.ContactsContract
import android.provider.MediaStore
import android.provider.Settings
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.data.model.ActionResult
import com.example.data.model.ContactInfo
import java.net.URLEncoder

class AndroidActionBridge(private val context: Context) {

    companion object {
        private const val TAG = "AndroidActionBridge"
    }

    /**
     * Opens WhatsApp application or deep link
     */
    fun openWhatsApp(): ActionResult {
        return try {
            val pm = context.packageManager
            val intent = pm.getLaunchIntentForPackage("com.whatsapp")
                ?: pm.getLaunchIntentForPackage("com.whatsapp.w4b")
            
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                ActionResult(
                    success = true,
                    actionName = "openWhatsApp",
                    message = "WhatsApp opened successfully on your device."
                )
            } else {
                // Fallback to web link
                val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://api.whatsapp.com/")).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(webIntent)
                ActionResult(
                    success = true,
                    actionName = "openWhatsApp",
                    message = "WhatsApp app is not installed, opening WhatsApp Web."
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to open WhatsApp", e)
            ActionResult(
                success = false,
                actionName = "openWhatsApp",
                message = "Could not open WhatsApp: ${e.localizedMessage ?: "Unknown error"}"
            )
        }
    }

    /**
     * Opens an app by name or launches settings / camera / maps
     */
    fun openApp(appName: String): ActionResult {
        val normalized = appName.trim().lowercase()
        return try {
            when {
                normalized.contains("whatsapp") -> openWhatsApp()
                
                normalized.contains("setting") -> {
                    val intent = Intent(Settings.ACTION_SETTINGS).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)
                    ActionResult(true, "openApp", "Device Settings opened.")
                }
                
                normalized.contains("youtube") -> {
                    val intent = context.packageManager.getLaunchIntentForPackage("com.google.android.youtube")
                    if (intent != null) {
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        context.startActivity(intent)
                        ActionResult(true, "openApp", "YouTube app opened.")
                    } else {
                        openUrl("https://www.youtube.com")
                    }
                }
                
                normalized.contains("instagram") || normalized.contains("insta") -> {
                    val intent = context.packageManager.getLaunchIntentForPackage("com.instagram.android")
                    if (intent != null) {
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        context.startActivity(intent)
                        ActionResult(true, "openApp", "Instagram app opened.")
                    } else {
                        openUrl("https://www.instagram.com")
                    }
                }
                
                normalized.contains("chrome") -> {
                    val intent = context.packageManager.getLaunchIntentForPackage("com.android.chrome")
                    if (intent != null) {
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        context.startActivity(intent)
                        ActionResult(true, "openApp", "Google Chrome opened.")
                    } else {
                        openUrl("https://www.google.com")
                    }
                }

                normalized.contains("camera") -> {
                    val intent = Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    if (intent.resolveActivity(context.packageManager) != null) {
                        context.startActivity(intent)
                        ActionResult(true, "openApp", "Camera opened.")
                    } else {
                        ActionResult(false, "openApp", "Camera application not available.")
                    }
                }

                normalized.contains("map") || normalized.contains("google map") -> {
                    val intent = context.packageManager.getLaunchIntentForPackage("com.google.android.apps.maps")
                    if (intent != null) {
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        context.startActivity(intent)
                        ActionResult(true, "openApp", "Google Maps opened.")
                    } else {
                        val gmmIntentUri = Uri.parse("geo:0,0?q=")
                        val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        context.startActivity(mapIntent)
                        ActionResult(true, "openApp", "Maps opened.")
                    }
                }

                normalized.contains("spotify") -> {
                    val intent = context.packageManager.getLaunchIntentForPackage("com.spotify.music")
                    if (intent != null) {
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        context.startActivity(intent)
                        ActionResult(true, "openApp", "Spotify opened.")
                    } else {
                        openUrl("https://open.spotify.com")
                    }
                }

                normalized.contains("calculator") || normalized.contains("calc") -> {
                    val calcPackages = listOf(
                        "com.google.android.calculator",
                        "com.android.calculator2",
                        "com.sec.android.app.popupcalculator"
                    )
                    var found = false
                    for (pkg in calcPackages) {
                        val intent = context.packageManager.getLaunchIntentForPackage(pkg)
                        if (intent != null) {
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            context.startActivity(intent)
                            found = true
                            break
                        }
                    }
                    if (found) {
                        ActionResult(true, "openApp", "Calculator opened.")
                    } else {
                        ActionResult(false, "openApp", "Calculator app not found.")
                    }
                }

                else -> {
                    // Search installed apps by label or package
                    val launchIntent = findLaunchIntentForAppLabel(normalized)
                    if (launchIntent != null) {
                        launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        context.startActivity(launchIntent)
                        ActionResult(true, "openApp", "Opened $appName successfully.")
                    } else {
                        ActionResult(
                            success = false,
                            actionName = "openApp",
                            message = "Could not find application '$appName' installed on this device."
                        )
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error opening app: $appName", e)
            ActionResult(
                success = false,
                actionName = "openApp",
                message = "Failed to open $appName: ${e.localizedMessage ?: "Unknown error"}"
            )
        }
    }

    /**
     * Initiates a phone call or opens the dialer with the given phone number
     */
    fun makeCall(phoneNumber: String): ActionResult {
        val cleanNumber = phoneNumber.replace(Regex("[^0-9+]"), "")
        if (cleanNumber.isBlank()) {
            return ActionResult(
                success = false,
                actionName = "makeCall",
                message = "Invalid phone number provided."
            )
        }

        return try {
            val dialIntent = Intent(Intent.ACTION_DIAL).apply {
                data = Uri.parse("tel:$cleanNumber")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(dialIntent)
            ActionResult(
                success = true,
                actionName = "makeCall",
                message = "Opened dialer with phone number $cleanNumber.",
                data = mapOf("phoneNumber" to cleanNumber)
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error making call to $phoneNumber", e)
            ActionResult(
                success = false,
                actionName = "makeCall",
                message = "Failed to initiate call to $phoneNumber: ${e.localizedMessage ?: "Unknown error"}"
            )
        }
    }

    /**
     * Searches device contacts and initiates call or prompts for clarification
     */
    fun callContact(contactName: String): ActionResult {
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.READ_CONTACTS
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasPermission) {
            return ActionResult(
                success = false,
                actionName = "callContact",
                message = "Permission to read contacts is not granted. Please grant contacts permission in the app."
            )
        }

        val trimmed = contactName.trim()
        if (trimmed.isEmpty()) {
            return ActionResult(
                success = false,
                actionName = "callContact",
                message = "Contact name cannot be empty."
            )
        }

        val matches = queryContacts(trimmed)

        return when {
            matches.isEmpty() -> {
                ActionResult(
                    success = false,
                    actionName = "callContact",
                    message = "No contact found matching '$trimmed' in your contacts list."
                )
            }
            matches.size == 1 -> {
                val contact = matches.first()
                val callResult = makeCall(contact.phoneNumber)
                if (callResult.success) {
                    ActionResult(
                        success = true,
                        actionName = "callContact",
                        message = "Calling ${contact.name} at ${contact.phoneNumber}.",
                        data = mapOf("name" to contact.name, "phoneNumber" to contact.phoneNumber),
                        contactsList = matches
                    )
                } else {
                    callResult
                }
            }
            else -> {
                // Multiple contacts found - return options so Arushi can ask for clarification
                val names = matches.joinToString(", ") { "${it.name} (${it.phoneNumber})" }
                ActionResult(
                    success = true,
                    actionName = "callContact",
                    message = "Found ${matches.size} contacts matching '$trimmed': $names. Please specify which one you would like to call.",
                    data = mapOf("count" to matches.size),
                    contactsList = matches
                )
            }
        }
    }

    /**
     * Opens a web URL in browser
     */
    fun openUrl(url: String): ActionResult {
        var cleanUrl = url.trim()
        if (!cleanUrl.startsWith("http://") && !cleanUrl.startsWith("https://")) {
            cleanUrl = "https://$cleanUrl"
        }

        return try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(cleanUrl)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            ActionResult(
                success = true,
                actionName = "openUrl",
                message = "Opening $cleanUrl in browser.",
                data = mapOf("url" to cleanUrl)
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error opening url: $url", e)
            ActionResult(
                success = false,
                actionName = "openUrl",
                message = "Could not open URL $url: ${e.localizedMessage ?: "Unknown error"}"
            )
        }
    }

    private fun queryContacts(query: String): List<ContactInfo> {
        val result = mutableListOf<ContactInfo>()
        val uri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI
        val projection = arrayOf(
            ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Phone.NUMBER
        )
        
        // Match name query
        val selection = "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} LIKE ?"
        val selectionArgs = arrayOf("%$query%")
        
        var cursor: Cursor? = null
        try {
            cursor = context.contentResolver.query(
                uri,
                projection,
                selection,
                selectionArgs,
                "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} ASC"
            )

            cursor?.let {
                val idIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.CONTACT_ID)
                val nameIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                val numberIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)

                val seenNumbers = mutableSetOf<String>()
                while (it.moveToNext()) {
                    val id = if (idIndex >= 0) it.getString(idIndex) ?: "" else ""
                    val name = if (nameIndex >= 0) it.getString(nameIndex) ?: "" else ""
                    val number = if (numberIndex >= 0) it.getString(numberIndex) ?: "" else ""
                    
                    val cleanNumber = number.replace(Regex("[^0-9+]"), "")
                    if (cleanNumber.isNotBlank() && seenNumbers.add(cleanNumber)) {
                        result.add(ContactInfo(id = id, name = name, phoneNumber = number))
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error querying contacts for $query", e)
        } finally {
            cursor?.close()
        }
        return result
    }

    private fun findLaunchIntentForAppLabel(query: String): Intent? {
        val pm = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        val apps = pm.queryIntentActivities(intent, 0)
        
        // 1. Exact match on app label
        for (resolveInfo in apps) {
            val label = resolveInfo.loadLabel(pm).toString().lowercase()
            if (label == query) {
                return pm.getLaunchIntentForPackage(resolveInfo.activityInfo.packageName)
            }
        }

        // 2. Contains match
        for (resolveInfo in apps) {
            val label = resolveInfo.loadLabel(pm).toString().lowercase()
            if (label.contains(query) || query.contains(label)) {
                return pm.getLaunchIntentForPackage(resolveInfo.activityInfo.packageName)
            }
        }

        // 3. Package name match
        for (resolveInfo in apps) {
            val pkg = resolveInfo.activityInfo.packageName.lowercase()
            if (pkg.contains(query)) {
                return pm.getLaunchIntentForPackage(resolveInfo.activityInfo.packageName)
            }
        }

        return null
    }
}
