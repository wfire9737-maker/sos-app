import re

with open("app/src/main/java/com/example/di/AppModule.kt", "r") as f:
    content = f.read()

import_nearby = """import com.example.ble.nearby.NearbyPresenceAdvertiser
import com.example.ble.nearby.NearbyDeviceScanner
import com.example.ble.nearby.NearbyBleManager"""

if "NearbyBleManager" not in content:
    content = content.replace('import javax.inject.Singleton', f'import javax.inject.Singleton\n{import_nearby}')

provide_nearby = """
    @Provides
    @Singleton
    fun provideNearbyPresenceAdvertiser(@ApplicationContext context: Context): NearbyPresenceAdvertiser {
        return NearbyPresenceAdvertiser(context)
    }

    @Provides
    @Singleton
    fun provideNearbyDeviceScanner(@ApplicationContext context: Context): NearbyDeviceScanner {
        return NearbyDeviceScanner(context)
    }
}"""

if "provideNearbyPresenceAdvertiser" not in content:
    content = content.replace('\n}', f'{provide_nearby}')

with open("app/src/main/java/com/example/di/AppModule.kt", "w") as f:
    f.write(content)

