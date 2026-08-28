import re

with open("app/src/main/java/com/example/ui/navigation/NavGraph.kt", "r") as f:
    content = f.read()

bad_block = """    // Determine starting route depending on session availability
    val authState = viewModel.authState.value
    val startDestination = if (authState is AuthState.Success) Screen.Home.route else Screen.Onboarding.route"""

good_block = """    // Determine starting route depending on session availability
    val authStateValue by viewModel.authState.collectAsState()
    val startDestination = if (viewModel.authState.value is AuthState.Success) Screen.Home.route else Screen.Onboarding.route

    LaunchedEffect(authStateValue) {
        if (authStateValue is AuthState.Success) {
            val currentRoute = navController.currentBackStackEntry?.destination?.route
            if (currentRoute == Screen.Login.route || currentRoute == Screen.Register.route || currentRoute == Screen.Onboarding.route) {
                navController.navigate(Screen.Home.route) {
                    popUpTo(0) { inclusive = true }
                }
            }
        } else if (authStateValue is AuthState.Initial) {
            val currentRoute = navController.currentBackStackEntry?.destination?.route
            if (currentRoute != Screen.Onboarding.route && currentRoute != Screen.Login.route && currentRoute != Screen.Register.route) {
                if (currentRoute != null) {
                    navController.navigate(Screen.Onboarding.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            }
        }
    }"""

content = content.replace(bad_block, good_block)

with open("app/src/main/java/com/example/ui/navigation/NavGraph.kt", "w") as f:
    f.write(content)
