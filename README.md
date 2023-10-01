# Lycoris
![Lycoris Header](https://github.com/arcanegolem/Lycoris/blob/master/images/lycoris_header.jpg)
Lycoris is an effortless PDF viewing library fully made with Jetpack Compose. Depends on Retrofit2 and Coil.

## Contents
* [Requirements](#Requrements)
* [Usage](#Usage)
* [Known Issues](#Known-issues)

## Requirements

- Add `INTERNET` permission to your Android Manifest
```xml
<uses-permission android:name="android.permission.INTERNET" />
```

- [OPTIONAL] Enable usage of cleartext traffic to your Android Manifest `<application>` tag to enable downloading of PDF documents from unsecure `http://` URLs
```xml
<application>
  ...
  android:usesCleartextTraffic="true"
  ...
</application>
```

## Usage
Module contains an overloaded `PdfViewer` composable function, usage examples below:

**Retrieving PDF document via raw resource:**
```kotlin
// Function structure
@Composable
fun PdfViewer(
   modifier: Modifier = Modifier.fillMaxSize(),
   @RawRes pdfResId: Int,
   documentDescription: String,
   verticalArrangement: Arrangement.Vertical = Arrangement.spacedBy(8.dp)
)

// Use-case example
PdfViewer(
  pdfResId = R.raw.sample_pdf, 
  documentDescription = "sample description",
)
```

**Retriving PDF document via URI:**
```kotlin
// Function structure
@Composable
fun PdfViewer(
   modifier: Modifier = Modifier.fillMaxSize(),
   uri: Uri,
   verticalArrangement: Arrangement.Vertical = Arrangement.spacedBy(8.dp)
)

// Use-case example
PdfViewer(
  uri = // Your URI
)
```

**Retriving PDF document via URL:**
```kotlin
// Function structure
@Composable
fun PdfViewer(
   modifier: Modifier = Modifier.fillMaxSize(),
   @Url url: String,
   headers: HashMap<String, String>,
   verticalArrangement: Arrangement.Vertical = Arrangement.spacedBy(8.dp)
)

// Use-case example
PdfViewer(
  url = "https://sample.link.com/sample_pdf.pdf",
  headers = hashMapOf( "headerKey" to "headerValue" )
)
```

## Known issues
- Occasional slow load of PDF documents retrieved via URL
