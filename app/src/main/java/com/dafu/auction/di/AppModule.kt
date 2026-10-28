package com.dafu.auction.di

import com.dafu.auction.data.local.AuctionRoomDatabase
import com.dafu.auction.data.local.PreferencesHelper
import com.dafu.auction.data.repository.CatalogRepositoryImpl
import com.dafu.auction.domain.repository.CatalogRepository
import com.dafu.auction.domain.usecase.EvaluateUseCase
import com.dafu.auction.domain.usecase.InferEngine
import com.dafu.auction.presentation.viewmodel.AuctionViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

/**
 * 全局依赖注入
 */
val appModule = module {

    // ---- 数据库 ----
    single { AuctionRoomDatabase.getInstance(androidContext()) }
    single { get<AuctionRoomDatabase>().customItemDao() }
    single { get<AuctionRoomDatabase>().goldPriceEntryDao() }

    // ---- 仓储 ----
    single<CatalogRepository> { CatalogRepositoryImpl(get(), get()) }

    // ---- 本地设置 ----
    single { PreferencesHelper(androidContext()) }

    // ---- 业务用例 ----
    single { EvaluateUseCase() }
    single { InferEngine() }

    // ---- ViewModel ----
    viewModel { AuctionViewModel(get(), get(), get(), get()) }
}
