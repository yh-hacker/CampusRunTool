# Third-party notices

CampusRunTool's own source code is licensed under the Apache License 2.0. The
following components and data are used by the app and remain under their own
licenses/terms.

## Libraries

| Component | License | Link |
|---|---|---|
| osmdroid | Apache License 2.0 | https://github.com/osmdroid/osmdroid |
| AndroidX / AppCompat | Apache License 2.0 | https://developer.android.com/jetpack/androidx |
| Material Components for Android | Apache License 2.0 | https://github.com/material-components/material-components-android |

## Map data and tiles

- **OpenStreetMap** — map data © OpenStreetMap contributors, available under
  the Open Database License (ODbL). https://www.openstreetmap.org/copyright
- **高德地图 / AMap** — tiles and data © 高德地图. Use of AMap tiles is subject
  to AMap's terms of service.
- **Esri World Imagery** — © Esri, Maxar, Earthstar Geographics, and the GIS
  User Community. Use is subject to Esri's terms.

The app only requests tiles at runtime; no map data is redistributed in this
repository.

## Inspiration

The no-root mock-location approach was studied from
[ZCShou/GoGoGo](https://github.com/ZCShou/GoGoGo) (GPLv3). This project is an
independent implementation: no GoGoGo source code is copied into this
repository. The map, route drawing, interpolation, diagnostics, floating
window, and coordinate handling are original code.
