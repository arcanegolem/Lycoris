# Lycoris [![](https://jitpack.io/v/arcanegolem/Lycoris.svg)](https://jitpack.io/#arcanegolem/Lycoris)
![Lycoris Header](https://github.com/arcanegolem/Lycoris/blob/master/images/header.jpg)
Lycoris is an effortless PDF viewing library fully made with Jetpack Compose. Depends on Retrofit2 and Coil.

## Contents
* [NEW](#NEW)
* [Requirements](#Requirements)
* [Usage](#Usage)
* [Thanks to](#Thanks-to)
* [Known Issues](#Known-issues)

## NEW
* Background color for enlarged page dialog
* `Close` button hotfix

## Requirements
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
  implementation 'com.github.arcanegolem:Lycoris:0.1.3'
}
```

## Usage
Module contains overloaded `PdfViewer`, `HorizontalPagerPdfViewer` and `VerticalPagerPdfViewer` composable functions, usage examples below:

**WARNING:** `PdfViewer` function utilizes LazyColumn composable.

**Retrieving PDF document via raw resource:**
```kotlin
//============== Parameters ================
fun PdfViewer(
   modifier: Modifier = Modifier.fillMaxSize(),
   @RawRes pdfResId: Int,
   documentDescription: String,
   verticalArrangement: Arrangement.Vertical = Arrangement.spacedBy(8.dp)
   dialogBackgroundColor: Color = Color.Transparent,
   minPageDialogScale : Float,
   maxPageDialogScale : Float
)

fun HorizontalPagerPdfViewer(
   modifier: Modifier = Modifier.fillMaxWidth(),
   @RawRes pdfResId: Int,
   documentDescription : String
)

fun VerticalPagerPdfViewer(
   modifier: Modifier = Modifier.fillMaxWidth(),
   @RawRes pdfResId: Int,
   documentDescription : String
)

//================ Usage ===================
PdfViewer(
  pdfResId = R.raw.sample_pdf, 
  documentDescription = "sample description",
)

HorizontalPagerPdfViewer(
  pdfResId = R.raw.sample_pdf, 
  documentDescription = "sample description",
)

VerticalPagerPdfViewer(
  pdfResId = R.raw.sample_pdf, 
  documentDescription = "sample description",
)
```

**Retriving PDF document via URI:**
```kotlin
//============== Parameters ================
fun PdfViewer(
   modifier: Modifier = Modifier.fillMaxSize(),
   uri: Uri,
   verticalArrangement: Arrangement.Vertical = Arrangement.spacedBy(8.dp)
   dialogBackgroundColor: Color = Color.Transparent,
   minPageDialogScale : Float,
   maxPageDialogScale : Float
)

fun HorizontalPagerPdfViewer(
   modifier: Modifier = Modifier.fillMaxWidth(),
   uri: Uri,
)

fun VerticalPagerPdfViewer(
   modifier: Modifier = Modifier.fillMaxWidth(),
   uri: Uri,
)

//================ Usage ===================
PdfViewer(
  uri = // Your URI
)

HorizontalPagerPdfViewer(
  uri = // Your URI
)

VerticalPagerPdfViewer(
  uri = // Your URI
)
```

**Retriving PDF document via URL:**
```kotlin
//============== Parameters ================
fun PdfViewer(
   modifier: Modifier = Modifier.fillMaxSize(),
   @Url url: String,
   headers: HashMap<String, String>,
   verticalArrangement: Arrangement.Vertical = Arrangement.spacedBy(8.dp)
   dialogBackgroundColor: Color = Color.Transparent,
   minPageDialogScale : Float,
   maxPageDialogScale : Float
)

fun HorizontalPagerPdfViewer(
   modifier: Modifier = Modifier.fillMaxWidth(),
   @Url url: String,
   headers: HashMap<String, String>,
)

fun VerticalPagerPdfViewer(
   modifier: Modifier = Modifier.fillMaxWidth(),
   @Url url: String,
   headers: HashMap<String, String>,
)

//================ Usage ===================
PdfViewer(
  url = "https://sample.link.com/sample_pdf.pdf",
  headers = hashMapOf( "headerKey" to "headerValue" )
)

HorizontalPagerPdfViewer(
  url = "https://sample.link.com/sample_pdf.pdf",
  headers = hashMapOf( "headerKey" to "headerValue" )
)

VerticalPagerPdfViewer(
  url = "https://sample.link.com/sample_pdf.pdf",
  headers = hashMapOf( "headerKey" to "headerValue" )
)
```

## Thanks to
- [Mikołaj Pich](https://github.com/mklkj) for preparing Lycoris for it's initial release.

## Known issues
- Occasional slow load of PDF documents retrieved via URL in PdfViewer
- Incorrect positioning of Viewer composables
