#!/bin/bash
sed -i -e '/id="gps-coordinates"/i \      <div\n        id="gsm-status"\n        style="\n          font-size: 18px;\n          margin-bottom: 30px;\n          text-align: center;\n          color: #ffcccc;\n        "\n      ></div>' pwa_sos.html

awk '
BEGIN { in_func = 0; }
/async function checkDeviceStatus\(\)/ {
    in_func = 1
    print "      async function checkDeviceStatus() {"
    print "        try {"
    print "          const response = await fetch(`${ESP32_BASE_URL}/status`, {"
    print "            method: \"GET\","
    print "            signal: AbortSignal.timeout(3000),"
    print "          });"
    print "          const data = await response.json();"
    print ""
    print "          updateConnectionIndicator(true);"
    print ""
    print "          if (data.sosActive && !wasEmergencyActive) {"
    print "            wasEmergencyActive = true;"
    print "            showEmergencyAlert();"
    print "            updateGPSCoordinates(data.lat, data.lon);"
    print "            checkGSMStatus();"
    print "          } else if (data.sosActive && wasEmergencyActive) {"
    print "            updateGPSCoordinates(data.lat, data.lon);"
    print "            checkGSMStatus();"
    print "          } else if (!data.sosActive) {"
    print "            wasEmergencyActive = false;"
    print "            hideEmergencyAlert();"
    print "          }"
    print ""
    print "          return data;"
    print "        } catch (error) {"
    print "          updateConnectionIndicator(false);"
    print "          return null;"
    print "        }"
    print "      }"
    print ""
    print "      async function checkGSMStatus() {"
    print "        try {"
    print "          const response = await fetch(`${ESP32_BASE_URL}/gsm-status`, {"
    print "            method: \"GET\","
    print "            signal: AbortSignal.timeout(3000)"
    print "          });"
    print "          const data = await response.json();"
    print "          const gsmElement = document.getElementById(\"gsm-status\");"
    print "          if (data.gsmFallbackUsed) {"
    print "            gsmElement.textContent = \"⚠️ SMS Fallback Alert Sent\";"
    print "          } else {"
    print "            gsmElement.textContent = \"\";"
    print "          }"
    print "        } catch (error) {"
    print "          console.error(\"Failed to check GSM status\", error);"
    print "        }"
    print "      }"
    next
}
in_func == 1 && /async function resetSOS\(\)/ {
    in_func = 0
}
in_func == 0 { print }
' pwa_sos.html > pwa_sos_gsm.html
mv pwa_sos_gsm.html pwa_sos.html
