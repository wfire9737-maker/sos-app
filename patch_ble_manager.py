import os

ble_manager_path = "app/src/main/java/com/example/ble/nearby/NearbyBleManager.kt"

with open(ble_manager_path, "r") as f:
    mgr = f.read()

mtarg3 = """    private var currentIntervalMs: Long = 0L
    private var isSessionActive = false

    private val advertiseRunnable = object : Runnable {
        override fun run() {
            if (!isSessionActive || currentIntervalMs <= 0) return
                
            // Expose presence for a short burst (e.g., 2 seconds)
            advertiser.startAdvertising()
                
            handler.postDelayed({
                if (isSessionActive) {
                    advertiser.stopAdvertising()
                }
            }, 2000L) // 2-second burst
    
            // Schedule the next session
            handler.postDelayed(this, currentIntervalMs)
        }
    }"""

mrepl3 = """    private var currentIntervalMs: Long = 0L
    private var isSessionActive = false
    private var isBurstActive = false

    private val advertiseRunnable = object : Runnable {
        override fun run() {
            if (!isSessionActive || currentIntervalMs <= 0) return
                
            isBurstActive = true
            // Expose presence for a short burst (e.g., 2 seconds)
            advertiser.startAdvertising()
                
            handler.postDelayed({
                isBurstActive = false
                if (isSessionActive) {
                    if (!gattServer.hasActiveConnections()) {
                        advertiser.stopAdvertising()
                    }
                }
            }, 2000L) // 2-second burst
    
            // Schedule the next session
            handler.postDelayed(this, currentIntervalMs)
        }
    }"""

mgr = mgr.replace(mtarg3, mrepl3)

# To fix the initialization order issue, we need to move the declarations of `isBurstActive` and `isSessionActive` before `init`
# Actually, since they are accessed inside closures (lambdas) passed to `gattServer.onActiveConnectionsChanged`, they just need to be declared in the class body. Wait, Kotlin evaluates `isBurstActive` inside the lambda, which runs later. But if it's referenced in `init`, it must be declared before it? No, in Kotlin, properties declared below `init` are available to closures created in `init`, but if they are accessed immediately, they would be uninitialized. Here they are accessed asynchronously. However, the compiler might still complain about unresolved reference if it's a scoping thing, or just because they didn't exist at all due to the regex failure. Since it was purely because they weren't declared, adding the declaration is enough.

with open(ble_manager_path, "w") as f:
    f.write(mgr)
