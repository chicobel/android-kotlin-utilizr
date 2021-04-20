package com.protectednet.utilizr.eventBus

import io.reactivex.Observable
import io.reactivex.disposables.Disposable
import io.reactivex.subjects.PublishSubject

object RxBus {
    val publisher= PublishSubject.create<Any>()

    fun publish(event: Any){
        publisher.onNext(event)
    }


    fun <T> listen(eventType: Class<T>): Observable<T> = publisher.ofType(eventType)

    inline fun <reified EVENT> subscribe(crossinline subscriber: (event:EVENT) -> Unit):Disposable =
        publisher.ofType(EVENT::class.java).subscribe{ event-> subscriber.invoke(event)}
}