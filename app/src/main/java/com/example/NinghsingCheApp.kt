package com.example

import android.app.Application
import com.example.data.ai.NinghsingCheAiAssistant
import com.example.data.local.AppDatabase
import com.example.data.preferences.UserPreferencesRepository
import com.example.data.remote.NingshingCheWebsiteClient
import com.example.data.remote.SupabaseClient
import com.example.data.repository.ArticleRepository
import com.example.data.repository.DashboardRepository

class NinghsingCheApp : Application() {

    lateinit var database: AppDatabase
        private set

    lateinit var articleRepository: ArticleRepository
        private set

    lateinit var preferencesRepository: UserPreferencesRepository
        private set

    lateinit var aiAssistant: NinghsingCheAiAssistant
        private set

    lateinit var websiteClient: NingshingCheWebsiteClient
        private set

    lateinit var supabaseClient: SupabaseClient
        private set

    lateinit var dashboardRepository: DashboardRepository
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        database = AppDatabase.getInstance(this)
        websiteClient = NingshingCheWebsiteClient()
        supabaseClient = SupabaseClient(this)
        dashboardRepository = DashboardRepository(this, supabaseClient, database)
        articleRepository = ArticleRepository(database, supabaseClient, websiteClient)
        preferencesRepository = UserPreferencesRepository(this)
        aiAssistant = NinghsingCheAiAssistant(articleRepository)
    }

    companion object {
        lateinit var instance: NinghsingCheApp
            private set
    }
}
