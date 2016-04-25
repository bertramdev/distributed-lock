package com.bertram.lock.provider

import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * Created by jsaardchit on 6/9/14.
 */
class RedisLockProvider extends LockProvider {
	// get logger
	private final static Logger log = LoggerFactory.getLogger(RedisLockProvider.class)

	String redisBeanName = 'redisService'

	/**
	 * This method will attempt to acquire a lock for a specified amount of time
	 * @param name
	 * @param args
	 * @return
	 */
	Boolean acquireLock(String name, Map args = null) {
		def locked = false
		def timeout = args?.timeout ?: this.acquireTimeout
        def indefinite = timeout == 0 ? true : false
		def now = System.currentTimeMillis()
		def ns = args?.namespace
		def expires = args?.ttl == null ? this.expireTimeout : args.ttl

		try {
			while (timeout > 0 || indefinite) {
				if (getRedisService().setnx(buildKey(name, ns), now as String) == 1) {
					if (expires != 0)
						getRedisService().expire(buildKey(name, ns), (expires / 1000) as Integer)

					return true
					break
				}
				else {
					timeout -= 50
					sleep(50)
				}
			}
			log.error("Unable to acquire lock for ${name}: acquire timeout expired.")
		}
		catch (Throwable t) {
			log.error("Lock acquire hard failed: ${t.message}", t)
			if ((args?.raiseError != null ? args.raiseError :this.raiseError))
				throw t
		}

		return locked
	}

	/**
	 * Releases a previously held lock
	 * @param name
	 * @param args
	 * @return
	 */
	Boolean releaseLock(String name, Map args = null) {
		def ns = args?.namespace
		try {
			getRedisService().del(buildKey(name, ns))
		}
		catch(Throwable t) {
			log.error("Unable to release lock ${name}: ${t.message}", t)
			if ((args?.raiseError != null ? args.raiseError :this.raiseError))
				throw t
		}
	}

	/**
	 * This will reset the expiration of a lock to milliseconds from now
	 * @param name
	 * @param args - use args.expires to set new timeout
	 * @return
	 */
	Boolean renewLock(String name, Map args = null) {
		def ns = args?.namespace
		try {
			def expires = args?.ttl == null ? this.expireTimeout : args.ttl
			if (expires > 0)
				getRedisService().expire(buildKey(name, ns), (expires / 1000) as Integer)
			else
				getRedisService().persist(buildKey(name, ns))
		}
		catch (Throwable t) {
			log.error("Unable to renew lock ${name}: ${t.message}", t)
			if ((args?.raiseError != null ? args.raiseError :this.raiseError))
				throw t
		}
	}

	/**
	 * Returns a list of currently held locks
	 * @return
	 */
	Set getLocks() {
		try {
			return getRedisService().keys("${namespace}.*")
		}
		catch (Throwable t) {
			log.error("Unable to get active locks: ${t.message}", t)
			throw t
		}
	}

	/**
	 * Grabs the service interface to the underlying redis implmentation to be used for locking
	 * @return
	 */
	private getRedisService() {
		return grailsApp.mainContext.getBean(redisBeanName)
	}
}
