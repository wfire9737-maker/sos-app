cat app/src/main/java/com/example/ui/navigation/NavGraph.kt | sed '/val activeEmergency/a \
    LaunchedEffect(activeEmergency) {\
        if (activeEmergency != null) {\
            val currentRoute = navController.currentBackStackEntry?.destination?.route\
            if (currentRoute != Screen.Emergency.route) {\
                navController.navigate(Screen.Emergency.route)\
            }\
        }\
    }' > tmp_nav_eff.kt
mv tmp_nav_eff.kt app/src/main/java/com/example/ui/navigation/NavGraph.kt
