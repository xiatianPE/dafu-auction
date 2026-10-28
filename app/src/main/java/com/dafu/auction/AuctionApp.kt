package com.dafu.auction

import android.app.Application
import com.dafu.auction.di.appModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

/**
 * 应用入口
 */
class AuctionApp : Application() {

    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@AuctionApp)
            modules(appModule)
        }
    }
}
