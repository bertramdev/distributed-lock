package com.bertram.lock.provider

/**
 * Created by jsaardchit on 6/9/14.
 */
abstract class LockProvider {
	public static final Long DEFAULT_LOCK_ACQUIRE_TIMEOUT = 30000l
	public static final Long DEFAULT_LOCK_EXPIRE_TIMEOUT = 0l
	public static final Boolean DEFAULT_LOCK_ACQUIRE_FAIL_EXCEPTION = true
	protected static final String LOCK_NAME_SPACE = 'distributed-lock'

	def grailsApp
	def acquireTimeout = DEFAULT_LOCK_ACQUIRE_TIMEOUT
	def expireTimeout = DEFAULT_LOCK_EXPIRE_TIMEOUT
	def raiseError = DEFAULT_LOCK_ACQUIRE_FAIL_EXCEPTION

	abstract Boolean acquireLock(String name, Map args)
	abstract Boolean releaseLock(String name, Map args)
	abstract Boolean renewLock(String name, Map args)
	abstract Set getLocks()

	protected String buildKey(String key) {
		return "${nameSpace}.${key}".toString()
	}
	protected String getNameSpace() {
		return LOCK_NAME_SPACE
	}
}
