package com.example.alertapp.location

actual fun String.format(vararg args: Any): String = java.lang.String.format(this, *args)
