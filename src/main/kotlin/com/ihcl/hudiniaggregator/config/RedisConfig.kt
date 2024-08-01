package com.ihcl.hudiniaggregator.config

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import redis.clients.jedis.ConnectionPoolConfig
import redis.clients.jedis.DefaultJedisClientConfig
import redis.clients.jedis.HostAndPort
import redis.clients.jedis.JedisPooled
import java.time.Duration

object RedisConfig {

    private val log: Logger = LoggerFactory.getLogger(javaClass)
    private val jedisPooled = initialize()
    fun setKey(key:String, value:String) {
        try {
             jedisPooled.set(key,value)
        } catch (e:Exception){
            log.error("Redis Error: ${e.localizedMessage} while storing value for Key : [${key}]. Cause: "+e.cause)
        }
    }
    fun getKey(key:String):String?{
        var value : String? = null
        try {
            value = jedisPooled.get(key)
        } catch (e:Exception){
            log.error("Redis Error: ${e.localizedMessage} while reading value for Key : [${key}]. Cause: "+e.cause)
        }
       return value
    }
    fun shutdown(){
        try {
            jedisPooled.close()
        }catch (e:Exception){
            log.error("Redis Error: ${e.localizedMessage} while releasing redis connection]")
        }
    }
    private fun initialize(): JedisPooled {
        val prop = PropertiesConfiguration.env
        val poolConfig = ConnectionPoolConfig()
        // maximum active connections in the pool,
        // tune this according to your needs and application type
        // default is 8
        poolConfig.maxTotal = 20

        // maximum idle connections in the pool, default is 8
        poolConfig.maxIdle = 20
        // minimum idle connections in the pool, default 0
        poolConfig.minIdle = 0

        // Enables waiting for a connection to become available.
        poolConfig.blockWhenExhausted = true
        // The maximum number of seconds to wait for a connection to become available
        poolConfig.setMaxWait(Duration.ofMillis(prop.requestTimeoutMillis.toLong()))

        // Enables sending a PING command periodically while the connection is idle.
        poolConfig.testWhileIdle = true
        // controls the period between checks for idle connections in the pool
        poolConfig.timeBetweenEvictionRuns = Duration.ofSeconds(1)

        // JedisPooled does all hard work on fetching and releasing connection to the pool
        // to prevent connection starvation
        // val jedis : JedisPooled = JedisPooled(poolConfig,redisUri);
        log.info("Connecting to Redis using HOST: ${prop.redisHost}, PORT: ${prop.redisPort}")
        val hostAndPort = HostAndPort(prop.redisHost,prop.redisPort.toInt())
        val jedisPooled = JedisPooled(
            hostAndPort,
            DefaultJedisClientConfig.builder()
                .socketTimeoutMillis(prop.requestTimeoutMillis.toInt()) // set timeout to 5 seconds
                .connectionTimeoutMillis(prop.requestTimeoutMillis.toInt())
                .password("${prop.redisKey}=")// set connection timeout to 5 seconds
                .ssl(true)
                .build(),
            poolConfig
        )
        log.info("Connected to Redis...${jedisPooled}")
        return jedisPooled
    }
}
