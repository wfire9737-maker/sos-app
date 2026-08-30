import re

with open("app/src/main/java/com/example/di/AppModule.kt", "r") as f:
    content = f.read()

target = """    @Provides
    @Singleton
    fun provideNearbyDeviceScanner(@ApplicationContext context: Context): NearbyDeviceScanner {
        return NearbyDeviceScanner(context)
    }"""
    
replacement = """    @Provides
    @Singleton
    fun provideNearbyDeviceScanner(@ApplicationContext context: Context): NearbyDeviceScanner {
        return NearbyDeviceScanner(context)
    }

    @Provides
    @Singleton
    fun provideNearbyGattServer(@ApplicationContext context: Context): com.example.ble.nearby.NearbyGattServer {
        return com.example.ble.nearby.NearbyGattServer(context)
    }

    @Provides
    @Singleton
    fun provideNearbyGattClient(@ApplicationContext context: Context): com.example.ble.nearby.NearbyGattClient {
        return com.example.ble.nearby.NearbyGattClient(context)
    }"""
    
content = content.replace(target, replacement)

with open("app/src/main/java/com/example/di/AppModule.kt", "w") as f:
    f.write(content)
