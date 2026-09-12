package com.baverika.notoir

import android.app.Application
import com.baverika.notoir.data.local.database.NotoirDatabase
import com.baverika.notoir.data.repository.NoteRepositoryImpl
import com.baverika.notoir.domain.repository.NoteRepository
import com.baverika.notoir.util.security.PasswordManager

class NotoirApplication : Application() {
    val database: NotoirDatabase by lazy {
        NotoirDatabase.getInstance(this)
    }

    val noteRepository: NoteRepository by lazy {
        NoteRepositoryImpl(database.noteDao())
    }

    val passwordManager: PasswordManager by lazy {
        PasswordManager(this)
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    companion object {
        lateinit var instance: NotoirApplication
            private set
    }
}
