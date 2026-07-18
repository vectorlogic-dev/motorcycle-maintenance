package com.motocare.app.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.motocare.app.data.local.MotoCareDatabase
import com.motocare.app.data.local.entity.MotorcycleEntity
import com.motocare.app.data.repository.MotorcycleRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RepositoryOperationsTest {
    private lateinit var database: MotoCareDatabase
    private lateinit var repository: MotorcycleRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, MotoCareDatabase::class.java).build()
        repository = MotorcycleRepository(database.motorcycleDao())
    }

    @After
    fun tearDown() = database.close()

    @Test
    fun motorcycleRepository_addUpdateAndArchive() = runTest {
        val id = repository.add(
            MotorcycleEntity(
                name = "Daily bike",
                manufacturer = "Honda",
                model = "BeAT",
                initialOdometerKm = 10,
                currentOdometerKm = 10,
            ),
        )

        val added = repository.get(id)
        assertNotNull(added)
        repository.update(added!!.copy(currentOdometerKm = 125))
        assertEquals(125L, repository.get(id)?.currentOdometerKm)

        repository.archive(id)
        assertEquals(emptyList<MotorcycleEntity>(), repository.activeMotorcycles.first())
        assertEquals(true, repository.get(id)?.archived)
    }
}
