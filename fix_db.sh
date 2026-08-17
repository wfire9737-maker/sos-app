cat app/src/main/java/com/example/service/DatabaseService.kt | sed '/demo-esp32-safety-band/,/)/d' > tmp_db.kt
mv tmp_db.kt app/src/main/java/com/example/service/DatabaseService.kt
