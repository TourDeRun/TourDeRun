package com.tourderun.core.di

import javax.inject.Qualifier

/** 디스크·네트워크 작업용 디스패처. `Dispatchers.IO`를 직접 쓰지 않고 이것을 주입받는다. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class IoDispatcher
