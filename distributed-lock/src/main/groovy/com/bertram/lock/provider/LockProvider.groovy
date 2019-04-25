package com.bertram.lock.provider
import java.util.concurrent.ConcurrentHashMap
/**
 * Created by jsaardchit on 6/9/14.
 */
abstract class LockProvider {
	public static final Long DEFAULT_LOCK_ACQUIRE_TIMEOUT = 30000l
	public static final Long DEFAULT_LOCK_EXPIRE_TIMEOUT = 30000l
	public static final Boolean DEFAULT_LOCK_ACQUIRE_FAIL_EXCEPTION = true
	protected static final String DEFAULT_LOCK_NAME_SPACE = 'distributed-lock'


	public ConcurrentHashMap<String,Map> LOCAL_LOCK_CACHE = new ConcurrentHashMap<>()
	def grailsApp
	def namespace = DEFAULT_LOCK_NAME_SPACE
	def acquireTimeout = DEFAULT_LOCK_ACQUIRE_TIMEOUT
	def expireTimeout = DEFAULT_LOCK_EXPIRE_TIMEOUT
	def raiseError = DEFAULT_LOCK_ACQUIRE_FAIL_EXCEPTION
	def lockObject = new Object
	abstract String acquireLock(String name, Map args)
	abstract Boolean releaseLock(String name, Map args)
	abstract Boolean renewLock(String name, Map args)
	abstract String checkLock(String name, Map args)
	abstract Set getLocks()

	protected String buildKey(String key, String namespace = null) {
		return "${namespace ?: this.namespace}.${key}".toString()
	}

	String checkLocalLock(String name,Map args) {
		def ns = args?.namespace
		def key = buildKey(name,ns)
		def now = new Date().time
		synchronized(lockObject) {
			def keyRow = LOCAL_LOCK_CACHE.get(key)
			if(keyRow && keyRow?.timeout >= now) {
				return keyRow.value
			} else {
				return null
			}
		}
	}


	Boolean acquireLocalLock(String name, String keyValue, Map args) {
		def now = new Date().time
		def ns = args?.namespace
		def key = buildKey(name,ns)
		def expires = args?.ttl == null ? this.expireTimeout : args.ttl
		synchronized(lockObject) {
			def keyRow = LOCAL_LOCK_CACHE.get(key)
			if(!keyRow || keyRow?.timeout < now) {
				LOCAL_LOCK_CACHE.put(key,[timeout: now+expires,value: keyValue])
				return true
			} else {
				return false
			}
		}
	}

	Boolean renewLocalLock(String name, Map args = null) {
		def ns = args?.namespace
		def expires = args?.ttl == null ? this.expireTimeout : args.ttl
		def key = buildKey(name,ns)
		def now = new Date().time

		synchronized(lockObject) {
			def keyRow = LOCAL_LOCK_CACHE.get(key)
			if(keyRow && keyRow?.timeout >= now) {
				keyRow.timeout = expires
				return true
			} else {
				return false
			}
		}
	}

	Boolean releaseLocalLock(String name, String keyValue, Map args) {
		def now = new Date().time
		def ns = args?.namespace
		def key = buildKey(name,ns)
		synchronized(lockObject) {

			def keyRow = LOCAL_LOCK_CACHE.get(key)
			if(keyRow) {
				if(keyValue) {
					if(keyRow.value == keyValue) {
						LOCAL_LOCK_CACHE.remove(key)
						return true	
					} else {
						return false
					}
				} else {
					LOCAL_LOCK_CACHE.remove(key)
					return true
				}
				
			} else {
				return true
			}
		}
	}
}
