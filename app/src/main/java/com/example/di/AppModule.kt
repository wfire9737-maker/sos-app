package com.example.di

import android.app.Application
import android.content.Context
import com.example.data.DeviceDataSource
import com.example.data.MockDeviceDataSource
import com.example.service.AIProvider
import com.example.service.AIService
import com.example.service.AiAnalysisService
import com.example.service.AlarmVibratorService
import com.example.service.AnalyticsService
import com.example.service.AuthService
import com.example.service.DatabaseService
import com.example.service.DeviceService
import com.example.service.EmergencyProvider
import com.example.service.EmergencyService
import com.example.service.FallDetectionService
import com.example.service.HistoryProvider
import com.example.service.HistoryService
import com.example.service.LocationService
import com.example.service.NotificationProvider
import com.example.service.NotificationService
import com.example.service.SafetyTimerService
import com.example.service.VoiceSosService
import com.example.repository.FallRepository
import com.example.data.FallDatabase

import androidx.room.Room
import com.example.data.local.SmartSosDatabase
import com.example.data.local.dao.UserDao
import com.example.data.local.dao.EmergencyContactDao
import com.example.data.local.dao.SosHistoryDao

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideAuthService(@ApplicationContext context: Context): AuthService = AuthService(context)

    @Provides
    @Singleton
    fun provideDatabaseService(@ApplicationContext context: Context): DatabaseService = DatabaseService(context)

    @Provides
    @Singleton
    fun provideLocationService(@ApplicationContext context: Context, databaseService: DatabaseService): LocationService = LocationService(context, databaseService.firestoreInstance)

    @Provides
    @Singleton
    fun provideAlarmVibratorService(@ApplicationContext context: Context): AlarmVibratorService = AlarmVibratorService(context)

    @Provides
    @Singleton
    fun provideNotificationService(@ApplicationContext context: Context, databaseService: DatabaseService): NotificationService = NotificationService(context, databaseService.firestoreInstance)

    @Provides
    @Singleton
    fun provideNotificationProvider(@ApplicationContext context: Context, notificationService: NotificationService): NotificationProvider = NotificationProvider(context, notificationService)

    @Provides
    @Singleton
    fun provideHistoryService(@ApplicationContext context: Context, databaseService: DatabaseService): HistoryService = HistoryService(context, databaseService.firestoreInstance)

    @Provides
    @Singleton
    fun provideHistoryProvider(@ApplicationContext context: Context, historyService: HistoryService): HistoryProvider = HistoryProvider(context, historyService)

    @Provides
    @Singleton
    fun provideAiAnalysisService(@ApplicationContext context: Context, databaseService: DatabaseService): AiAnalysisService = AiAnalysisService(context, databaseService.firestoreInstance)

    @Provides
    @Singleton
    fun provideDeviceDataSource(): DeviceDataSource = MockDeviceDataSource()

    @Provides
    @Singleton
    fun provideDeviceService(@ApplicationContext context: Context, databaseService: DatabaseService, notificationService: NotificationService, deviceDataSource: DeviceDataSource): DeviceService = DeviceService(context, databaseService, notificationService, deviceDataSource)

    @Provides
    @Singleton
    fun provideFallDatabase(@ApplicationContext context: Context): FallDatabase = FallDatabase.getDatabase(context)

    @Provides
    @Singleton
    fun provideFallRepository(database: FallDatabase): FallRepository = FallRepository(database.fallEventDao())

    @Provides
    @Singleton
    fun provideFallDetectionService(@ApplicationContext context: Context, repository: FallRepository): FallDetectionService = FallDetectionService(context, repository)

    @Provides
    @Singleton
    fun provideVoiceSosService(@ApplicationContext context: Context): VoiceSosService = VoiceSosService(context)

    @Provides
    @Singleton
    fun provideAIService(@ApplicationContext context: Context, databaseService: DatabaseService): AIService = AIService(context, databaseService.firestoreInstance)

    @Provides
    @Singleton
    fun provideAIProvider(@ApplicationContext context: Context, aiService: AIService): AIProvider = AIProvider(context, aiService)

    @Provides
    @Singleton
    fun provideEmergencyService(@ApplicationContext context: Context, databaseService: DatabaseService, locationService: LocationService, notificationService: NotificationService): EmergencyService = EmergencyService(context, databaseService.firestoreInstance, locationService, notificationService, databaseService)

    @Provides
    @Singleton
    fun provideEmergencyProvider(@ApplicationContext context: Context, emergencyService: EmergencyService): EmergencyProvider = EmergencyProvider(context, emergencyService)

    @Provides
    @Singleton
    fun provideSafetyTimerService(@ApplicationContext context: Context, notificationProvider: NotificationProvider): SafetyTimerService = SafetyTimerService(context, notificationProvider)

    @Provides
    @Singleton
    fun provideAnalyticsService(@ApplicationContext context: Context): AnalyticsService = AnalyticsService(context)

    @Provides
    @Singleton
    fun provideSecurityService(@ApplicationContext context: Context): com.example.service.SecurityService = com.example.service.SecurityService(context)

    @Provides
    @Singleton
    fun provideSmartSosDatabase(@ApplicationContext context: Context): SmartSosDatabase {
        return Room.databaseBuilder(
            context,
            SmartSosDatabase::class.java,
            "smart_sos_db"
        ).fallbackToDestructiveMigration().build()
    }

    @Provides
    fun provideUserDao(database: SmartSosDatabase): UserDao = database.userDao()

    @Provides
    fun provideEmergencyContactDao(database: SmartSosDatabase): EmergencyContactDao = database.emergencyContactDao()

    @Provides
    fun provideSosHistoryDao(database: SmartSosDatabase): SosHistoryDao = database.sosHistoryDao()

}
