cat app/src/main/java/com/example/service/DeviceService.kt | sed '/bleManager.connectionState.collect/,/}/ {
    // We will replace this whole block using perl or python for easier multi-line matching.
}'
