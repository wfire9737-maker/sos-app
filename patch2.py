import re

ble_manager_path = "app/src/main/java/com/example/ble/nearby/NearbyBleManager.kt"

with open(ble_manager_path, "r") as f:
    mgr = f.read()

mgr = re.sub(
    r"advertiser\.startAdvertising\(\)\s+handler\.postDelayed\(\{\s+if \(isSessionActive\) \{\s+advertiser\.stopAdvertising\(\)\s+\}\s+\}, 2000L\)",
    r"isBurstActive = true\n            advertiser.startAdvertising()\n            \n            handler.postDelayed({\n                isBurstActive = false\n                if (isSessionActive) {\n                    if (!gattServer.hasActiveConnections()) {\n                        advertiser.stopAdvertising()\n                    }\n                }\n            }, 2000L)",
    mgr
)

with open(ble_manager_path, "w") as f:
    f.write(mgr)
