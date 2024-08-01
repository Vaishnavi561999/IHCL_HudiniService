package com.ihcl.hudiniaggregator.plugins

import com.ihcl.hudiniaggregator.service.serviceModule
import org.koin.core.context.startKoin

fun configureDependencyInjection() {
    startKoin {
        modules(serviceModule)
    }
}