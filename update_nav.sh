cat app/src/main/java/com/example/ui/navigation/NavGraph.kt | sed '/val sosCountdown/a \    val activeEmergency by viewModel.activeEmergency.collectAsState()' > tmp_nav.kt
mv tmp_nav.kt app/src/main/java/com/example/ui/navigation/NavGraph.kt
