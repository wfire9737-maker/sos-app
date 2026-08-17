        function showEmergencyAlert() {
            console.log("EMERGENCY: physical button was pressed!");
            showStatus("Emergency button pressed! Alert is active.", "emergency");
            document.getElementById('main-screen').style.display = 'none';
            document.getElementById('emergency-screen').style.display = 'flex';
        }

        function updateGPSCoordinates(lat, lon) {
            const gpsElement = document.getElementById('gps-coordinates');
            if (lat !== undefined && lon !== undefined) {
                gpsElement.textContent = `Location: ${lat.toFixed(4)}, ${lon.toFixed(4)}`;
            } else {
                gpsElement.textContent = "Location: Acquiring GPS signal...";
            }
        }
