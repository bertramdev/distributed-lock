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
	String acquireLock(String name, Map args = null) {
		def timeout = args?.timeout ?: this.acquireTimeout
        def indefinite = timeout == 0 ? true : false
		def now = System.currentTimeMillis()
		def keyValue = java.util.UUID.randomUUID()?.toString()
		def ns = args?.namespace
		def expires = args?.ttl == null ? this.expireTimeout : args.ttl

		try {
			while (timeout > 0 || indefinite) {
				if (getRedisService().setnx(buildKey(name, ns), keyValue) == 1) {
					def val = getRedisService().get(buildKey(name, ns))
					if(val == keyValue) {
						if (expires != 0)
							getRedisService().expire(buildKey(name, ns), (expires / 1000) as Integer)
						return keyValue
						break	
					}
					
				}
				else {
					def randomTimeout = 50 + (int)(Math.random() * 1000)
					timeout -= randomTimeout
					sleep(randomTimeout)
				}
			}
			if(getRedisService().pttl(buildKey(name, ns)) <= 0) {
				log.warn("Non expiring lock detected, clearing lock and attempting reacquire...")
				getRedisService().del(buildKey(name, ns))
				return acquireLock(name,args)
			} else {
				log.error("Unable to acquire lock for ${name}: acquire timeout expired.")		
			}
		}
		catch (Throwable t) {
			log.error("Lock acquire hard failed: ${t.message}", t)
			if ((args?.raiseError != null ? args.raiseError :this.raiseError))
				throw t
		}

		return null
	}

	/**
	 * Releases a previously held lock
	 * @param name
	 * @param args
	 * @return
	 */
	Boolean releaseLock(String name, Map args = null) {
		def ns = args?.namespace
		def id = args?.lock
		try {
			def val = getRedisService().get(buildKey(name, ns))
			if(val && id && val != id) {
				log.warn("Someone else has the lock ${name}")
				return
			}
			getRedisService().del(buildKey(name, ns))
		}
		catch(Throwable t) {
			log.error("Unable to release lock ${name}: ${t.message}", t)
			if ((args?.raiseError != null ? args.raiseError :this.raiseError))
				throw t
		}
	}

	/**
	* This will check if a lock is currently acquired or not
	* @param name - The unique lock key we are checking for
	* @param args - Optional namespace can be passedin this Map
	* @return String the UUID representing the lock instance or who may have the lock (NULL if no lock is currently acquired)
	*/
	String checkLock(String name, Map args=null) {
		def ns = args?.namespace
		try {
			def val = getRedisService().get(buildKey(name, ns))
			return val
		}
		catch(Throwable t) {
			log.error("Unable to check for lock ${name}: ${t.message}", t)
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
