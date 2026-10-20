package com.example.bit240200foodmanager

import android.app.Application
import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.CoroutineWorker
import com.example.bit240200foodmanager.data.FoodRepository
import java.util.concurrent.TimeUnit

class FoodApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        FoodRepository.configure(this)
        // EN default even if the device itself is Vietnamese; AppCompat stores selections.
        if (AppCompatDelegate.getApplicationLocales().isEmpty) {
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags("en"))
        }
        FoodSyncScheduler.periodic(this)
        FoodSyncScheduler.now(this)
        val conn = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        conn.registerDefaultNetworkCallback(object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) { FoodSyncScheduler.now(this@FoodApplication) }
        })
    }
}

class FoodSyncWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result = if (FoodRepository.sync()) Result.success() else Result.retry()
}

object FoodSyncScheduler {
    private val online = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
    fun now(context: Context) {
        val request = OneTimeWorkRequestBuilder<FoodSyncWorker>().setConstraints(online).build()
        WorkManager.getInstance(context).enqueueUniqueWork("BIT240200_sync_now", ExistingWorkPolicy.KEEP, request)
    }
    fun periodic(context: Context) {
        val request = PeriodicWorkRequestBuilder<FoodSyncWorker>(15, TimeUnit.MINUTES).setConstraints(online).build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork("BIT240200_periodic", ExistingPeriodicWorkPolicy.KEEP, request)
    }
}
