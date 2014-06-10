package com.bertram.lock.conf

import com.bertram.lock.LockService
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

		// TODO: Once more providers are added, add conditional to determine how to setup the provider based on type
		// Configure the provider bean
		this.beanBuilder."${getLockServiceBeanName()}"(config.provider.type) {
			redisBeanName = config.provider?.connect ?: 'redisService'
			grailsApp = app
			if (config.provider?.namespace) {
				namespace = config.provider.namespace
			}
		}

		this.configured = true
	}

	public static String getLockServiceBeanName() {
		return "${LockService.class.toString()}.Provider"
	}
}
