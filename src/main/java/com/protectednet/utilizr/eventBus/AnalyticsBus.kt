package com.protectednet.utilizr.eventBus

import kotlinx.coroutines.flow.MutableSharedFlow

/**
 * Similar to [RxBus] but for analytics events only.
 *
 * A module can publish events to this and other modules can optionally subscribe and deal with
 * them how they see fit.
 *
 * To ensure a contract between modules and differ from RxBus, all events must implement
 * [AnalyticsBusIdentifier].
 */
class AnalyticsBus : AnalyticsBusProvider {

    private val events = MutableSharedFlow<AnalyticsBusIdentifier>()

    override suspend fun <T : AnalyticsBusIdentifier> publish(event: T) {
        events.emit(event)
    }

    override suspend fun subscribe(onEvent: (event: AnalyticsBusIdentifier) -> Unit) {
        events.collect { onEvent(it) }
    }
}

interface AnalyticsBusIdentifier
