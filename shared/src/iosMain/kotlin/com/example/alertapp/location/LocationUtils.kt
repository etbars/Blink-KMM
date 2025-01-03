package com.example.alertapp.location

import platform.Foundation.NSString
import platform.Foundation.stringWithFormat

actual fun String.format(vararg args: Any): String = 
    NSString.stringWithFormat(this, args = args)
