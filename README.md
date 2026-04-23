# Flight Tracker (Android)

Offline-capable flight tracker. Look up a flight, view live gate/time/delay
info from **FlightAware AeroAPI**, pre-download map tiles along the filed
route corridor via **MapLibre Native**, then see your live GPS position
against the cached route while in the air — **no internet required**.

## Prerequisites (one-time macOS setup)

```bash
brew install --cask temurin@21
brew install --cask android-studio
```

After installing Android Studio, open it once → **SDK Manager**:
- Android SDK Platform 35 (Android 15)
- Android SDK Build-Tools 35.x
- Android SDK Platform-Tools
- Android SDK Command-line Tools

Add to `~/.zshrc`:

```bash
export JAVA_HOME="$(/usr/libexec/java_home -v 21)"
export ANDROID_HOME="$HOME/Library/Android/sdk"
export PATH="$PATH:$ANDROID_HOME/platform-tools:$ANDROID_HOME/emulator:$ANDROID_HOME/cmdline-tools/latest/bin"
```

Reload: `source ~/.zshrc`.

### Enable USB debugging on the S25 Ultra
Settings → About phone → Software information → tap **Build number** 7 times →
back → **Developer options** → **USB debugging** ON. Connect USB-C and accept
the fingerprint prompt on the phone.

Verify: `adb devices` should show your phone.

## Configure API key

1. Sign up: https://flightaware.com/aeroapi/portal/
2. Create a key (Personal pay-as-you-go tier is fine).
3. Create `local.properties` in the project root:

   ```properties
   sdk.dir=/Users/lewis/Library/Android/sdk
   AERO_API_KEY=your_aeroapi_key_here
   ```

   (`sdk.dir` is written automatically when you open the project in Android
   Studio.)

## Generate the Gradle wrapper (first time only)

The wrapper jar isn't committed. Generate it once:

```bash
brew install gradle
cd /Users/lewis/Workspace/offline-flight-tracker
gradle wrapper --gradle-version 8.11.1
```

After this, use `./gradlew` as normal. You can `brew uninstall gradle` if you
want — the wrapper handles everything going forward.

## Build & install

```bash
./gradlew installDebug
adb shell am start -n com.lewishadden.flighttracker/.MainActivity
```

Or just open the project in Android Studio and click **Run** with your S25 Ultra
selected as the target.

## Using the app

1. **Search** by flight number (e.g. `BA283`, `UAL123`, `AA1`). AeroAPI returns
   matches for the current date window.
2. **Flight detail** shows scheduled / estimated / actual times, gate, terminal,
   delay, aircraft type, filed route distance, altitude, ETE.
3. Tap **Download offline map** *while on Wi-Fi before boarding*. The app:
   - Fetches the filed route waypoints from AeroAPI.
   - Densifies along great-circle arcs so high-latitude curvature is covered.
   - Builds a padded corridor bbox around the whole path.
   - Registers a MapLibre offline region (zoom 3–9) and downloads tiles to
     internal SQLite storage.
   - Shows MB / tile progress. Typical long-haul: 50–300 MB.
4. Once downloaded, tap **Open flight map**. Grant location permission. A
   foreground service starts streaming GPS at 1–2 Hz. You'll see:
   - The projected path as a polyline.
   - Origin/destination markers.
   - Your live position marker — **works offline at altitude**.
   - An overlay with distance along path + cross-track error.

## Architecture

```
ui/*           Compose screens + ViewModels (Hilt injected)
data/api       Retrofit service + kotlinx-serialization DTOs for AeroAPI
data/db        Room — flights, route_fixes, offline_regions
data/repository FlightRepository (network + cache)
domain/model   Pure Kotlin domain types (Flight, RouteFix, TrackPoint)
map            OfflineMapManager — wraps MapLibre OfflineManager
location       LocationService (foreground) + LocationController
util/Geo.kt    Great-circle densify, bbox, nearest-point-on-polyline
```

### Why this is offline-capable in the air

- **Map tiles**: MapLibre Native's `OfflineManager` persists every tile
  inside the corridor bbox to a local SQLite DB. Rendering reads from that DB
  — zero network calls once downloaded.
- **Flight path**: The AeroAPI filed route is cached in Room, re-read offline.
- **Position**: GPS is a radio signal from satellites; it does not need
  internet. Modern phones near a window at cruise altitude get a lock in
  ~30–90 seconds.
- **Cross-track + progress**: Computed on-device using haversine + polyline
  projection (`util/Geo.kt`). No cloud calls.

### Notes

- AeroAPI's flight object doesn't always populate boarding time directly;
  the "Boarding" field on detail uses `estimated_out` (push-back) as the
  closest proxy. Some carriers publish actual boarding via AeroAPI's
  webhook-only fields, not in the REST `/flights/{ident}` response.
- Origin/destination lat/lon aren't included in the top-level AeroAPI
  response used here. For the cleanest corridor, the pre-download uses only
  the waypoints from `/flights/{id}/route`. If the airport endpoints are
  needed, add `AeroApiService.getAirport(code)` and fetch them before
  downloading.
- Offline region downloads follow the active tile pyramid, which includes
  glyphs/sprites — all required for a fully offline render.

## License

Personal use. MapLibre is BSD-2-Clause. OpenFreeMap tiles are OpenStreetMap
data under ODbL — attribution shown on the map.
