package com.rasoiai.data.local.dao

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.rasoiai.data.local.RasoiDatabase
import org.junit.After
import org.junit.Before

/**
 * Base class for DAO tests providing in-memory Room database setup/teardown.
 *
 * Subclasses access [database] to get specific DAOs in their tests.
 */
abstract class BaseDaoTest {

    protected lateinit var database: RasoiDatabase

    @Before
    fun setupDatabase() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(
            context,
            RasoiDatabase::class.java
        ).allowMainThreadQueries().build()
    }

    @After
    fun tearDownDatabase() {
        database.close()
    }
}
