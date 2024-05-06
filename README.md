# Lycoris [![](https://jitpack.io/v/arcanegolem/Lycoris.svg)](https://jitpack.io/#arcanegolem/Lycoris)
![Lycoris Header](https://github.com/arcanegolem/Lycoris/blob/master/images/header.jpg)
Lycoris is an effortless PDF viewing library which provides ready-to-use PDF viewing composables fully made with and for Jetpack Compose. Depends on Retrofit2, Coil and Material Icons.

# Contents
* [DEMO](#DEMO)
* [NEW](#NEW)
* [Requirements](#Requirements)
* [Usage](#Usage)
* [Thanks to](#Thanks-to)
* [Known Issues](#Known-issues)

# DEMO
![Lycoris demo](https://github.com/arcanegolem/Lycoris/blob/master/images/lycoris_demo.gif)

**This can be achieved with following code (Sample in MainActivity):**
```kotlin
// ...
Column (
  modifier = Modifier.fillMaxSize()
) {
  PdfViewer (
    pdfResId = R.raw.sample_multipage,
    controlsAlignment = Alignment.CenterEnd,
  )
}
// ...
```

# NEW(!)
## PdfViewer
* Now has zoom controls instead of separate dialogs for each page
* Added `iconTint` and `accentColor` for zoom controls (NOTE: `accentColor` will apply with 40% alpha)
* Added `controlsAlignment` parameter for zoom controls positioning inside PdfViewer's box
* Added `bitmapScale` for upscaling/downscaling pages for better readability/performance

## HorizontalPagerPdfViewer
* Now marked **Experimental**
* Is unstable and unmaintained for now

## VerticalPagerPdfViewer
* Now marked **Experimental**
* Is unstable and unmaintained for now

# Requirements
- Add `INTERNET` permission to your Android Manifest
```xml
<uses-permission android:name="android.permission.INTERNET" />
```

- [OPTIONAL] Set `android:usesCleartextTraffic="true"` in your Android Manifest `<application>` tag to enable downloading of PDF documents from unsecure `http://` URLs
```xml
<application>
  ...
  android:usesCleartextTraffic="true"
  ...
</application>
```

- Add Jitpack to your project repositories
```gradle
dependencyResolutionManagement {
  repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
  repositories {
    mavenCentral()
    maven { url 'https://jitpack.io' }
  }
}
```

- Add lycoris to your project dependencies
```gradle
dependencies {
  implementation 'com.github.arcanegolem:Lycoris:1.0.0-alpha01'
}
```

# Usage
Module contains overloaded `PdfViewer`, `HorizontalPagerPdfViewer` and `VerticalPagerPdfViewer` ready-to-use **composable** functions, usage examples below:

**WARNING:** `PdfViewer` function utilizes LazyColumn composable.

## PDF from Raw Resource
```kotlin
PdfViewer (
   modifier: Modifier = Modifier,
   @RawRes pdfResId: Int,
   pagesVerticalArrangement: Arrangement.Vertical = Arrangement.spacedBy(8.dp),
   iconTint : Color = Color.Black,
   accentColor : Color = Color.DarkGray,
   controlsAlignment: Alignment = Alignment.BottomEnd,
   bitmapScale : Int = 1
)

VerticalPagerPdfViewer (
   modifier: Modifier = Modifier,
   @RawRes pdfResId: Int,
   documentDescription : String
)

HorizontalPagerPdfViewer(
   modifier: Modifier = Modifier,
   @RawRes pdfResId: Int,
   documentDescription : String
)
```

## PDF from Uri
```kotlin
PdfViewer (
   modifier: Modifier = Modifier,
   uri: Uri,
   pagesVerticalArrangement: Arrangement.Vertical = Arrangement.spacedBy(8.dp),
   accentColor: Color = Color.DarkGray,
   iconTint: Color = Color.Black,
   controlsAlignment: Alignment = Alignment.BottomEnd,
   bitmapScale : Int = 1
)

VerticalPagerPdfViewer(
   modifier: Modifier = Modifier,
   uri: Uri
)

HorizontalPagerPdfViewer(
   modifier: Modifier = Modifier,
   uri: Uri
)
```

## PDF from URL
```kotlin
PdfViewer(
   modifier: Modifier = Modifier,
   @Url url: String,
   headers: HashMap<String, String>? = null,
   pagesVerticalArrangement: Arrangement.Vertical = Arrangement.spacedBy(8.dp),
   accentColor: Color = Color.DarkGray,
   iconTint: Color = Color.Black,
   controlsAlignment: Alignment = Alignment.BottomEnd,
   bitmapScale : Int = 1
)

VerticalPagerPdfViewer(
   modifier: Modifier = Modifier,
   @Url url: String
   headers: HashMap<String, String>,
)

HorizontalPagerPdfViewer(
   modifier: Modifier = Modifier,
   @Url url: String,
   headers: HashMap<String, String>
)
```

# Thanks to
- [Mikołaj Pich](https://github.com/mklkj) for preparing Lycoris for it's initial release.

# Known issues
