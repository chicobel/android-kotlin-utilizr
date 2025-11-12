package com.protectednet.utilizr.eventBus

import com.protectednet.utilizr.eventBus.EventBus.observe
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.filterIsInstance

/**
 * Generic event bus using SharedFlow, similar to RxBus but with modern coroutines.
 * Completely agnostic. It can handle any event type
 * 
 * Usage:
 * ```
 * // Publish an event
 * EventBus.publish(MyEvent())
 * 
 * // Observe specific event type (in a coroutine scope)
 * EventBus.observe<MyEvent>().collect { event ->
 *     // Handle event
 * }
 * ```
 */
object EventBus {
    private val _events = MutableSharedFlow<Any>(
        replay = 0,    // Fire-and-forget (don't replay to late collectors)
        extraBufferCapacity = 1,    // Buffer one rapid emission
        onBufferOverflow = BufferOverflow.DROP_OLDEST   // Latest event matters, drop older ones
    )

    /**
     * SharedFlow of all events. Use [observe] for type-safe filtering.
     */
    val events: SharedFlow<Any> = _events.asSharedFlow()
    
    /**
     * Publishes an event. Non-blocking, so safe to call from any thread.
     * Returns true if the event was emitted successfully, false if it was dropped.
     */
    fun publish(event: Any): Boolean {
        return _events.tryEmit(event)
    }
    
    /**
     * Returns a Flow that filters events by the specified type.
     * Use this for collecting specific event types in a type-safe manner.
     * 
     * This is the modern SharedFlow-based alternative to RxBus.subscribe.
     * 
     * Example:
     * ```
     * // In a LaunchedEffect or coroutine scope
     * EventBus.observe<MyEvent>().collect { event ->
     *     // event is of type MyEvent
     * }
     * ```
     */
    inline fun <reified EVENT> observe(): Flow<EVENT> {
        return events.filterIsInstance<EVENT>()
    }
}
