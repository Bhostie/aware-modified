#!/bin/bash
# Script to add proactive database initialization to all providers

# List of providers that need the fix (excluding ones we already fixed)
PROVIDERS=(
    "WiFi_Provider.java"
    "Processor_Provider.java"
    "Bluetooth_Provider.java"
    "Applications_Provider.java"
    "Screen_Provider.java"
    "Temperature_Provider.java"
    "Locations_Provider.java"
    "ESM_Provider.java"
    "Installations_Provider.java"
    "Network_Provider.java"
    "Barometer_Provider.java"
    "Magnetometer_Provider.java"
    "Accelerometer_Provider.java"
    "Gyroscope_Provider.java"
    "Telephony_Provider.java"
    "Linear_Accelerometer_Provider.java"
    "Gravity_Provider.java"
    "Traffic_Provider.java"
    "Mqtt_Provider.java"
)

echo "Providers to check: ${#PROVIDERS[@]}"

