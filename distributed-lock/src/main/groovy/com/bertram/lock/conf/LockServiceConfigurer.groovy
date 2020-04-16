package com.bertram.lock.conf

import com.bertram.lock.LockService
import com.bertram.lock.provider.RedisLockProvider
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * Created by jsaardchit on 6/9/14.
 */
class LockServiceConfigurer {
	// setup logger
	private final static Logger log = LoggerFactory.getLogger(LockServiceConfigurer)

	def config
	def beanBuilder
	def configured = false

	public LockServiceConfigurer(config) {
		this.config = config
	}

	/**
	 * Configure the beans the lock service will rely on
	 * @param beanBuilder
	 * @return
	 */
	public configure(beanBuilder, app) {
		this.beanBuilder = beanBuilder

		if(!config?.provider?.type) {
			config.provider.type = RedisLockProvider
		}
		// TODO: Once more providers are added, add conditional to determine how to setup the provider based on type
		// Configure the provider bean
		if(config.provider.type instanceof String) {
			config.provider.type = Class.forName(config.provider.type)
		}
		this.beanBuilder."${getLockServiceBeanName()}"(config.provider.type) {
			if(config.provider.type == RedisLockProvider) {
				redisBeanName = config.provider?.connect ?: 'redisService'	
			}
			grailsApp = app
			if (config.namespace)
				namespace = config.namespace
			if (config.raiseError)
				raiseError = config.raiseError
			if (config.defaultTimeout)
				acquireTimeout = config.defaultTimeout?.toLong()
			if (config.expireTimeout)
				expireTimeout = config.expireTimeout?.toLong()
		}

		this.configured = true
	}

	public static String getLockServiceBeanName() {
		return "${LockService.class.toString()}.Provider"
	}
}
