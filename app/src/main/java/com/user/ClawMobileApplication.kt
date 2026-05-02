package com.user

import android.app.Application
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.user.data.ChatDatabase
import com.user.data.PrefsManager
import com.user.repository.ChatRepository
import com.user.service.InterventionPollService
import com.user.service.PermissionPollService
import java.util.concurrent.TimeUnit

class ClawMobileApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        PDFBoxResourceLoader.init(this)
        schedulePermissionPolling()
        scheduleInterventionPolling()
    }

    private fun scheduleInterventionPolling() {
        val request = PeriodicWorkRequestBuilder<InterventionPollService>(
            15, TimeUnit.MINUTES
        ).build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            InterventionPollService.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }

    private fun schedulePermissionPolling() {
        val request = PeriodicWorkRequestBuilder<PermissionPollService>(
            30, TimeUnit.MINUTES
        ).build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            PermissionPollService.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }

    val database by lazy { ChatDatabase.getDatabase(this) }
    val prefsManager by lazy { PrefsManager(this) }

    // DAO instances for use in activities
    val gitConnectionDao by lazy { database.gitConnectionDao() }
    val projectContextDao by lazy { database.projectContextDao() }
    val chatDao by lazy { database.chatDao() }
    val taskDao by lazy { database.taskDao() }
    val cachedResponseDao by lazy { database.cachedResponseDao() }

    val repository by lazy {
        ChatRepository(
            chatDao,
            gitConnectionDao,
            projectContextDao,
            taskDao,
            prefsManager
        )
    }
}