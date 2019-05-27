package com.example.nrfswarm.protocols


// Public interface methods visible to all classes

interface UIUpdaterInterface {

    fun resetUIWithConnection(status: Boolean)
    fun updateStatusViewWith(status: String)
    fun update(message: String)
}