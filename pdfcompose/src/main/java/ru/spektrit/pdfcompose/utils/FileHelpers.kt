package ru.spektrit.pdfcompose.utils

import java.util.Date

internal fun provideFileName(): String { return "${Date().time}.pdf" }