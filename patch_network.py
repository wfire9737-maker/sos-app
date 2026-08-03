with open("app/src/main/java/com/example/ui/GuardianViewModel.kt", "r") as f:
    content = f.read()

old_net = """        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as android.net.ConnectivityManager
        val networkInfo = connectivityManager.activeNetworkInfo
        if (networkInfo == null || !networkInfo.isConnected) {"""

new_net = """        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as android.net.ConnectivityManager
        val activeNetwork = connectivityManager.activeNetwork
        val networkCapabilities = connectivityManager.getNetworkCapabilities(activeNetwork)
        val isConnected = networkCapabilities?.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
        if (!isConnected) {"""

content = content.replace(old_net, new_net)

with open("app/src/main/java/com/example/ui/GuardianViewModel.kt", "w") as f:
    f.write(content)
