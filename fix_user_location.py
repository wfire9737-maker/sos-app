import os

filepath = "app/src/main/java/com/example/model/UserLocation.kt"
with open(filepath, "r") as f:
    content = f.read()

content = content.replace("val favorites: List<FavoritePlace> = emptyList()", 'val favorites: List<FavoritePlace> = emptyList(),\n    val address: String = ""')
content = content.replace('"trafficEnabled" to trafficEnabled,', '"trafficEnabled" to trafficEnabled,\n            "address" to address,')
content = content.replace('obj.put("trafficEnabled", trafficEnabled)', 'obj.put("trafficEnabled", trafficEnabled)\n        obj.put("address", address)')
content = content.replace('trafficEnabled = map["trafficEnabled"] as? Boolean ?: false,', 'trafficEnabled = map["trafficEnabled"] as? Boolean ?: false,\n                address = map["address"] as? String ?: "",')
content = content.replace('trafficEnabled = obj.optBoolean("trafficEnabled", false),', 'trafficEnabled = obj.optBoolean("trafficEnabled", false),\n                address = obj.optString("address", ""),')

with open(filepath, "w") as f:
    f.write(content)
print("Added address to UserLocation")
