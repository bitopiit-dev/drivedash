# DriveDash

DriveDash is an Android app that turns a phone or tablet into a live trip dashboard. It shows your current speed on an animated speedometer, the distance left to your destination, where you are right now and the outside temperature. It works for driving, cycling and walking.

## Features

- **Live speedometer:** animated neon gauge with a digital readout in km/h or mph, including walking speed with one decimal.
- **Speed alert:** set a limit; the gauge turns red and beeps when you go over it.
- **Destination tracking:** search a place name or enter `lat,lng` to see remaining distance, a direction arrow, a progress bar, time left and arrival time.
- **Location and weather:** present area name and outside temperature in °C, refreshed as you move.
- **Trip stats:** distance, time, average speed, top speed, altitude and compass heading.
- **Any screen size:** single column on phones, two columns on tablets and in landscape.

## Requirements

- Android 6.0 (API 23) or newer, phone or tablet
- Location permission; internet for place names and temperature
- Built with Android Studio, Kotlin and Jetpack Compose

## Build

1. Clone the repository and open the folder in Android Studio.
2. Let Gradle sync finish.
3. Run on a device, or build an APK from the terminal:

```
./gradlew assembleDebug
```

The APK is created in `app/build/outputs/apk/debug/`.

## Notes

- Distance to the destination is measured in a straight line, so road distance is usually longer and the arrival time is an estimate.
- Speed comes from GPS and is most accurate outdoors.
- Do not operate the app while driving. Set the destination before you start or let a passenger do it.
- Weather data is provided by [Open-Meteo](https://open-meteo.com).

## Ownership

Owned by **Khondaker Arifur Rahman**.

© 2026 Khondaker Arifur Rahman. All rights reserved. No part of this software may be copied, modified, published or distributed without prior written permission from the owner.
