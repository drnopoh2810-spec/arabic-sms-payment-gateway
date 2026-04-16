package me.capcom.smsgateway.modules.payment

import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val paymentModule = module {
    single { PaymentSettings(androidContext()) }
    single { PaymentParser() }
    single { PaymentService(androidContext(), get(), get(), get(), get(), get(), get()) }
}