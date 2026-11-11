package com.protectednet.utilizr.eventBus

interface AnalyticsBusProvider {
    suspend fun <T : AnalyticsBusIdentifier> publish(event: T)
    suspend fun subscribe(onEvent: (event: AnalyticsBusIdentifier) -> Unit)
}