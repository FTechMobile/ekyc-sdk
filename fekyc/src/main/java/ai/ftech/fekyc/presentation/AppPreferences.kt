package ai.ftech.fekyc.presentation

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson

internal object AppPreferences {
    private const val MODE = Context.MODE_PRIVATE
    private lateinit var preferences: SharedPreferences
    private lateinit var gson: Gson
    private val TOKEN_KEY = "TOKEN_KEY"
    private val FTECH_KEY = "FTECH_KEY"
    private val APP_ID = "APP_ID"
    private val SESSION_ID_FRONT = "SESSION_ID_FRONT"
    private val SESSION_ID_BACK = "SESSION_ID_BACK"
    private val SESSION_ID_FACE = "SESSION_ID_FACE"
    private val TRANS_ID = "TRANS_ID"

    fun init(context: Context) {
        preferences = context.getSharedPreferences(context.packageName, MODE)
        gson = Gson()
    }

    private inline fun SharedPreferences.edit(operation: (SharedPreferences.Editor) -> Unit) {
        val editor = edit()
        operation(editor)
        editor.apply()
    }

    private inline fun SharedPreferences.commit(operation: (SharedPreferences.Editor) -> Unit) {
        val editor = edit()
        operation(editor)
        editor.commit()
    }

    fun clearData() {
        transactionId = ""
        sessionIdFront = ""
        sessionIdBack = ""
        sessionIdFace = ""
    }

    var token: String?
        get() = preferences.getString(TOKEN_KEY, "")
        set(value) = preferences.edit {
            it.putString(TOKEN_KEY, value)
        }

    var ftechKey: String?
        get() = preferences.getString(FTECH_KEY, "")
        set(value) = preferences.edit {
            it.putString(FTECH_KEY, value)
        }
    var appId: String?
        get() = preferences.getString(APP_ID, "")
        set(value) = preferences.edit {
            it.putString(APP_ID, value)
        }
    var transactionId: String?
        get() = preferences.getString(TRANS_ID, "")
        set(value) = preferences.edit {
            it.putString(TRANS_ID, value)
        }

    var sessionIdFront
        get() = preferences.getString(SESSION_ID_FRONT, "")
        set(value) = preferences.edit {
            it.putString(SESSION_ID_FRONT, value)
        }
    var sessionIdBack
        get() = preferences.getString(SESSION_ID_BACK, "")
        set(value) = preferences.edit {
            it.putString(SESSION_ID_BACK, value)
        }

    var sessionIdFace
        get() = preferences.getString(SESSION_ID_FACE, "")
        set(value) = preferences.edit {
            it.putString(SESSION_ID_FACE, value)
        }
}
