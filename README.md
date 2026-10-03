# QiblaArrow

A full-screen **black** screen with one **long arrow** that points to the Qibla (the Kaaba).

- Below the arrow: **Qibla Direction** and its **degrees** (from true north), plus how far to turn ("Turn 47° right")
- The arrow turns **green** when you are facing the Qibla (within 3°)
- Top-right corner: **By: Black Falcon**
- Package: `qiblaarrow.blackfalcon.jan` — APK: `QiblaArrow.apk`

## Notes
- Location is needed once to work out the Qibla direction. It is read when you open the app and saved on the phone; nothing is sent anywhere.
- "Up" on the screen is the way you are facing: phone flat → top of the phone; phone upright → back of the phone.
- Keep the phone away from magnets and metal; move it in a figure-8 to calibrate if the arrow seems off.
- No internet, no dependencies. Android 8.0+.

## Build
Push to GitHub — the **Build APK** workflow runs automatically.
Open **Actions → latest run → Artifacts → QiblaArrow** to download `QiblaArrow.apk`.
