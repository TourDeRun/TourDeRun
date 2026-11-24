package com.tourderun.core.di

import com.tourderun.core.data.account.AccountRepository
import com.tourderun.core.data.account.FirestoreAccountRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class AccountModule {
    @Binds abstract fun bindAccountRepository(impl: FirestoreAccountRepository): AccountRepository
}
