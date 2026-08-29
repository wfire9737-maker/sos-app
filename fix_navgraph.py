import re

with open("app/src/main/java/com/example/ui/navigation/NavGraph.kt", "r") as f:
    content = f.read()

bad_str = """        composable(Screen.About.route) {
            AboutScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.navigateUp() }
            )

        composable(Screen.DeveloperDashboard.route) {"""

good_str = """        composable(Screen.About.route) {
            AboutScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.navigateUp() }
            )
        }
        composable(Screen.DeveloperDashboard.route) {"""

content = content.replace(bad_str, good_str)

# and then at the end we have:
#     if (fallState == "FALL_COUNTDOWN") {
#         FallCountdownDialog(
#             secondsLeft = countdown,
#             onCancel = { viewModel.fallDetectionService.cancelFallCountdown() }
#         )
#     }
#     }
# let's fix that too. Wait, NavHost closing brace is before `if (fallState == "FALL_COUNTDOWN")`?
