package com.protectednet.utilizr.eventBus

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * App-level events that can be triggered from anywhere.
 * Uses sealed interface for type safety and exhaustiveness checking.
 */
sealed interface AppEvent {
    /**
     * Event to show the paywall sheet
     */
    object ShowPaywall : AppEvent
}

object AppEvents {
    private val _appEvents = MutableSharedFlow<AppEvent>(
        replay = 0,    // Fire-and-forget (don't replay to late collectors)
        extraBufferCapacity = 1,    // Buffer one rapid emission
        onBufferOverflow = BufferOverflow.DROP_OLDEST   // Latest event matters, drop older ones
    )
    val appEvents: SharedFlow<AppEvent> = _appEvents.asSharedFlow()
    
    /**
     * Publish an app event (non-suspending - never blocks).
     * Returns true if event was successfully emitted, false if dropped.
     */
    fun publish(event: AppEvent): Boolean {
        return _appEvents.tryEmit(event)
    }
}
