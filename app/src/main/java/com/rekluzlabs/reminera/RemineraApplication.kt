package com.rekluzlabs.reminera

import android.app.Application
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader

class RemineraApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        PDFBoxResourceLoader.init(applicationContext)
    }
}
