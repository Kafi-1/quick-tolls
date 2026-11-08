package com.kafi.utilityhub.capture

import android.content.Intent

object ProjectionHolder {
    var resultCode: Int = 0
    var data: Intent? = null

    fun store(code: Int, intent: Intent?) {
        resultCode = code
        data = intent
    }

    fun clear() {
        resultCode = 0
        data = null
    }
}