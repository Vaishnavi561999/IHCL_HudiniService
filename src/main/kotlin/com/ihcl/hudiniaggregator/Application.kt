package com.ihcl.hudiniaggregator

import com.ihcl.hudiniaggregator.config.PropertiesConfiguration
import com.ihcl.hudiniaggregator.config.RedisConfig
import com.ihcl.hudiniaggregator.plugins.configureDependencyInjection
import com.ihcl.hudiniaggregator.plugins.corsConfig
import com.ihcl.hudiniaggregator.plugins.statusPages
import com.ihcl.hudiniaggregator.plugins.configureRouting
import com.ihcl.hudiniaggregator.plugins.configureSerialization
import io.ktor.server.application.*

fun main(args: Array<String>): Unit = io.ktor.server.netty.EngineMain.main(args)

fun Application.module() {
    configureSerialization()
    configureRouting()
    corsConfig()
    configureDependencyInjection()
    statusPages()
    PropertiesConfiguration.initConfig(this.environment)


    // Initialize resources when application starts up
    environment.monitor.subscribe(ApplicationStarting, ::onStarting)
    environment.monitor.subscribe(ApplicationStarted, ::onStarted)
    environment.monitor.subscribe(ApplicationStopping, ::onStopping)
    environment.monitor.subscribe(ApplicationStopped, ::onStopped)
    // Shutdown resources when application is going down
    environment.monitor.subscribe(ApplicationStopPreparing){
        println("Shutting down  Redis..")
        RedisConfig.shutdown()
    }
}

private fun onStarted(app: Application){
    app.log.info("Application started")
}
private fun onStarting(app: Application){
    app.log.info("Application starting")
}
private fun onStopping(app: Application){
    app.log.info("Application stopping")
}
private fun onStopped(app: Application){
    app.log.info("Application stopped")
}

