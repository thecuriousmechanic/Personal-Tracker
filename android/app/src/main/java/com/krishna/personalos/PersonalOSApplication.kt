package com.krishna.personalos

import android.app.Application
import com.krishna.personalos.core.database.AppDatabase
import com.krishna.personalos.core.repository.PersonalOSRepository

class PersonalOSApplication : Application() {
    val repository by lazy { PersonalOSRepository(AppDatabase.get(this)) }
}
