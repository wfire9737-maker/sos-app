                if (data.sosActive && !wasEmergencyActive) {
                    wasEmergencyActive = true;
                    showEmergencyAlert();
                    updateGPSCoordinates(data.lat, data.lon);
                } else if (data.sosActive && wasEmergencyActive) {
                    updateGPSCoordinates(data.lat, data.lon);
                } else if (!data.sosActive) {
                    wasEmergencyActive = false;
                    hideEmergencyAlert(); // In case device was reset physically
                }
