package com.motocare.app.data.repository

import androidx.room.withTransaction
import com.motocare.app.data.local.MotoCareDatabase
import com.motocare.app.data.local.entity.AttachmentReferenceEntity
import com.motocare.app.data.local.entity.ProblemLogEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProblemRepository @Inject constructor(private val database: MotoCareDatabase) {
    fun observe(motorcycleId: Long): Flow<List<ProblemLogEntity>> = database.phaseThreeDao().observeProblems(motorcycleId)

    suspend fun save(problem: ProblemLogEntity, mediaUri: String?): Long = database.withTransaction {
        val id = if (problem.id == 0L) database.phaseThreeDao().insertProblem(problem) else {
            database.phaseThreeDao().updateProblem(problem); problem.id
        }
        database.attachmentDao().deleteForOwner("PROBLEM", id)
        mediaUri?.let {
            database.attachmentDao().insert(
                AttachmentReferenceEntity(ownerType = "PROBLEM", ownerId = id, uri = it, mediaType = "image/*"),
            )
        }
        id
    }

    suspend fun unresolved(): List<ProblemLogEntity> = database.phaseThreeDao().getAllUnresolvedProblems()

    suspend fun delete(problem: ProblemLogEntity) = database.withTransaction {
        database.attachmentDao().deleteForOwner("PROBLEM", problem.id)
        database.phaseThreeDao().deleteProblem(problem)
    }
}
