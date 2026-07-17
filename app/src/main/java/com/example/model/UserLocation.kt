package com.example.model

import org.json.JSONArray
import org.json.JSONObject

data class FavoritePlace(
    val id: String,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val type: String // HOME, WORK, COLLEGE, OTHER
) {
    fun toMap(): Map<String, Any> {
        return mapOf(
            "id" to id,
            "name" to name,
            "latitude" to latitude,
            "longitude" to longitude,
            "type" to type
        )
    }

    companion object {
        fun fromMap(map: Map<String, Any?>): FavoritePlace {
            return FavoritePlace(
                id = map["id"] as? String ?: "",
                name = map["name"] as? String ?: "",
                latitude = (map["latitude"] as? Number)?.toDouble() ?: 0.0,
                longitude = (map["longitude"] as? Number)?.toDouble() ?: 0.0,
                type = map["type"] as? String ?: "OTHER"
            )
        }
    }
}

data class UserLocation(
    val userId: String = "",
    val latitude: Double = 37.7749, // Default to San Francisco or a standard city center
    val longitude: Double = -122.4194,
    val speed: Double = 0.0, // km/h
    val bearing: Float = 0f, // degrees
    val altitude: Double = 0.0, // meters
    val accuracy: Float = 5.0f, // meters
    val timestamp: Long = System.currentTimeMillis(),
    val distanceTraveled: Double = 0.0, // km
    val viewMode: String = "NORMAL", // NORMAL, SATELLITE, TERRAIN
    val trafficEnabled: Boolean = false,
    val favorites: List<FavoritePlace> = emptyList()
) {
    fun toMap(): Map<String, Any> {
        return mapOf(
            "userId" to userId,
            "latitude" to latitude,
            "longitude" to longitude,
            "speed" to speed,
            "bearing" to bearing,
            "altitude" to altitude,
            "accuracy" to accuracy,
            "timestamp" to timestamp,
            "distanceTraveled" to distanceTraveled,
            "viewMode" to viewMode,
            "trafficEnabled" to trafficEnabled,
            "favorites" to favorites.map { it.toMap() }
        )
    }

    fun toJsonObject(): JSONObject {
        val obj = JSONObject()
        obj.put("userId", userId)
        obj.put("latitude", latitude)
        obj.put("longitude", longitude)
        obj.put("speed", speed)
        obj.put("bearing", bearing.toDouble())
        obj.put("altitude", altitude)
        obj.put("accuracy", accuracy.toDouble())
        obj.put("timestamp", timestamp)
        obj.put("distanceTraveled", distanceTraveled)
        obj.put("viewMode", viewMode)
        obj.put("trafficEnabled", trafficEnabled)

        val favsArray = JSONArray()
        favorites.forEach { fav ->
            val favObj = JSONObject()
            favObj.put("id", fav.id)
            favObj.put("name", fav.name)
            favObj.put("latitude", fav.latitude)
            favObj.put("longitude", fav.longitude)
            favObj.put("type", fav.type)
            favsArray.put(favObj)
        }
        obj.put("favorites", favsArray)
        return obj
    }

    companion object {
        fun fromMap(map: Map<String, Any?>): UserLocation {
            val favListRaw = map["favorites"] as? List<Map<String, Any?>> ?: emptyList()
            val favs = favListRaw.map { FavoritePlace.fromMap(it) }

            return UserLocation(
                userId = map["userId"] as? String ?: "",
                latitude = (map["latitude"] as? Number)?.toDouble() ?: 37.7749,
                longitude = (map["longitude"] as? Number)?.toDouble() ?: -122.4194,
                speed = (map["speed"] as? Number)?.toDouble() ?: 0.0,
                bearing = (map["bearing"] as? Number)?.toFloat() ?: 0f,
                altitude = (map["altitude"] as? Number)?.toDouble() ?: 0.0,
                accuracy = (map["accuracy"] as? Number)?.toFloat() ?: 5.0f,
                timestamp = (map["timestamp"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                distanceTraveled = (map["distanceTraveled"] as? Number)?.toDouble() ?: 0.0,
                viewMode = map["viewMode"] as? String ?: "NORMAL",
                trafficEnabled = map["trafficEnabled"] as? Boolean ?: false,
                favorites = favs
            )
        }

        fun fromJsonObject(obj: JSONObject): UserLocation {
            val favs = mutableListOf<FavoritePlace>()
            val favsArray = obj.optJSONArray("favorites")
            if (favsArray != null) {
                for (i in 0 until favsArray.length()) {
                    val favObj = favsArray.getJSONObject(i)
                    favs.add(
                        FavoritePlace(
                            id = favObj.optString("id"),
                            name = favObj.optString("name"),
                            latitude = favObj.optDouble("latitude"),
                            longitude = favObj.optDouble("longitude"),
                            type = favObj.optString("type")
                        )
                    )
                }
            }

            return UserLocation(
                userId = obj.optString("userId"),
                latitude = obj.optDouble("latitude", 37.7749),
                longitude = obj.optDouble("longitude", -122.4194),
                speed = obj.optDouble("speed", 0.0),
                bearing = obj.optDouble("bearing", 0.0).toFloat(),
                altitude = obj.optDouble("altitude", 0.0),
                accuracy = obj.optDouble("accuracy", 5.0).toFloat(),
                timestamp = obj.optLong("timestamp", System.currentTimeMillis()),
                distanceTraveled = obj.optDouble("distanceTraveled", 0.0),
                viewMode = obj.optString("viewMode", "NORMAL"),
                trafficEnabled = obj.optBoolean("trafficEnabled", false),
                favorites = favs
            )
        }
    }
}
