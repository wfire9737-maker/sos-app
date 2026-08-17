#!/bin/bash
cat pwa_sos.html | awk '
BEGIN { in_func = 0; }
/async function checkDeviceStatus\(\)/ {
    in_func = 1
    print "        async function checkDeviceStatus() {"
    print "            try {"
    print "                const response = await fetch(`${ESP32_BASE_URL}/status`, {"
    print "                    method: \"GET\","
    print "                    signal: AbortSignal.timeout(3000)"
    print "                });"
    print "                const data = await response.json();"
    print ""
    print "                updateConnectionIndicator(true);"
    print ""
    print "                if (data.sosActive && !wasEmergencyActive) {"
    print "                    wasEmergencyActive = true;"
    print "                    showEmergencyAlert();"
    print "                    updateGPSCoordinates(data.lat, data.lon);"
    print "                } else if (data.sosActive && wasEmergencyActive) {"
    print "                    updateGPSCoordinates(data.lat, data.lon);"
    print "                } else if (!data.sosActive) {"
    print "                    wasEmergencyActive = false;"
    print "                    hideEmergencyAlert();"
    print "                }"
    print ""
    print "                return data;"
    print "            } catch (error) {"
    print "                updateConnectionIndicator(false);"
    print "                return null;"
    print "            }"
    print "        }"
    next
}
in_func == 1 && /async function resetSOS\(\)/ {
    in_func = 0
}
in_func == 0 { print }
' > pwa_sos_fixed.html
mv pwa_sos_fixed.html pwa_sos.html
