package com.bertram.lock.provider

import com.bertram.lock.DistributedLock
import groovy.util.logging.Slf4j
import grails.async.Promises

/**
* Uses GORM instead of Redis to provide distributed locking capabilities. This can be handy with reducing outside dependencies and in low load situations. It does require GORM async libraries however.
* @author David Estes
*/
@Slf4j
public class GormLockProvider extends LockProvider {

	/**
	 * This method will attempt to acquire a lock for a specified amount of time
	 * @param name
	 * @param args
	 * @return
	 */
	String acquireLock(String name, Map args = null) {
		def timeout = args?.timeout ?: this.acquireTimeout
        def indefinite = timeout == 0 ? true : false
		//def now = System.currentTimeMillis()
		def keyValue
		def ns = args?.namespace
		def expires = args?.ttl == null ? this.expireTimeout : args.ttl

		try {
			while (timeout > 0 || indefinite) {

				keyValue = java.util.UUID.randomUUID()?.toString()
				log.debug("Making Lock Attempt ${buildKey(name, ns)} ${keyValue}")
				try {
					def lockAcquired = Promises.tasks {
						def now = new Date().time
						DistributedLock.withNewSession { session ->
							def localLock = checkLocalLock(name,args)
							if(localLock) {
								//still locked, no need to even check right now
								return false
							}

							DistributedLock.where{name == buildKey(name,ns) && timeout < now}.deleteAll()
							def count = DistributedLock.executeQuery("select count(*) from DistributedLock d where d.name = :name",[name:buildKey(name,ns)]).first()
							if(count > 0) {
								return false
							}
							def localLocked = acquireLocalLock(name,keyValue,args)
							if(localLocked) {
								try {
									def lock = new DistributedLock(name:buildKey(name,ns), value: keyValue,timeout: now + expires)
									lock.save(flush:true,failOnError:true)
									lock.discard()
									return true		
								} catch(ex2) {
									releaseLocalLock(name,args)
									return false
								}
								
							} else {
								//lock already acquired locally, skip this
								return false
							}
							
						}
					}.get()
					if(!lockAcquired) {
						log.debug("Lock Acquired by someone else, waiting to try again...")
						def randomTimeout = 250 + (int)(Math.random() * 1000)
						timeout -= randomTimeout
						sleep(randomTimeout)
					} else {
						return keyValue
					}
				} catch(ex) {
					// Possible duplicate lock exception
					log.debug("Lock Acquired by someone else, waiting to try again...")
					def randomTimeout = 250 + (int)(Math.random() * 1000)
					timeout -= randomTimeout
					sleep(randomTimeout)
				}
			}
			log.error("Unable to acquire lock for ${name}: acquire timeout expired.")			
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
		def timeout = args?.timeout ?: this.acquireTimeout
        def indefinite = timeout == 0 ? true : false
		def ns = args?.namespace
		def id = args?.lock
		try {
			while (timeout > 0 || indefinite) {
				log.debug("Attempting to release lock ${buildKey(name, ns)}")
				try {
					def success = Promises.tasks {
						Long now = new Date().time
						String keyName = buildKey(name,ns)
						DistributedLock.withNewTransaction { tx ->
							def lockRow = DistributedLock.executeQuery("select d.value from DistributedLock d where d.name = :name AND (d.timeout IS NULL OR d.timeout >= :date)",[name: buildKey(name,ns),date: now])
							def lockValue = lockRow ? lockRow.first() : null
							
							def val = lockValue
							if(val && id && val != id) {
								log.warn("Someone else has the lock ${name}")
								return false
							}

							if (!lockValue) {
								log.info("Unable to find lock ${buildKey(name, ns)} to release")
								releaseLocalLock(name,val,args)

								return true
							}

							DistributedLock.where{name == keyName}.deleteAll()
							releaseLocalLock(name,val,args)
							return true
						}
					}.get()

					if(!success) {
						log.info("Lock Acquired by someone else, waiting to try again...")
						def randomTimeout = 250 + (int)(Math.random() * 1000)
						timeout -= randomTimeout
						sleep(randomTimeout)
					}
					else {
						return true
					}
				}
				catch (Throwable t){
					// possible db exceptions, make sure to retry until timeout
					log.info("Failed to acquire lock: ${t.message}")
					def randomTimeout = 250 + (int)(Math.random() * 1000)
					timeout -= randomTimeout
					sleep(randomTimeout)
				}
			}
			// this means timeout expired
			log.debug("Timeout expired while trying to release lock ${buildKey(name, ns)}")
			if ((args?.raiseError != null ? args.raiseError :this.raiseError))
				throw new RuntimeException("Timeout expired while trying to release lock ${buildKey(name, ns)}")
			else
				return false
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
			def result = Promises.tasks {
				def now = new Date().time
				DistributedLock.withNewSession { session ->
					def localValue = checkLocalLock(name,args)
					if(localValue) {
						return [value: localValue]
					}
					def lock = DistributedLock.withCriteria(uniqueResult:true,cache:false) {
						eq('name',buildKey(name,ns))
						or {
							isNull('timeout')
							gte('timeout',now)
						}
						maxResults(1)
					}
					return lock ?: false
				}
			}.get()
			if(result == false) {
				return null
			} else {
				return result?.value	
			}
			
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

			def result = Promises.tasks {
				def now = new Date().time
				DistributedLock.withNewSession { session ->
					def lock = DistributedLock.withCriteria(uniqueResult:true, cache:false) {
						eq('name',buildKey(name,ns))
						or {
							isNull('timeout')
							gte('timeout',now)
						}
						maxResults(1)
					}
					if(lock) {
						if (expires > 0)
							DistributedLock.where { name == buildKey(name,ns) && (timeout == null || timeout > now)}.updateAll(timeout:now + expires)
						else
							DistributedLock.where{ name == buildKey(name,ns) && (timeout == null || timeout > now)}.updateAll(timeout:null)

						renewLocalLock(name,args)
						return true
					} else {
						return false
					}
					
				}
			}.get()
			return result
		} catch (Throwable t) {
			log.error("Unable to renew lock ${name}: ${t.message}", t)
			if ((args?.raiseError != null ? args.raiseError :this.raiseError))
				throw t
		}
		return false
	}

	/**
	 * Returns a list of currently held locks
	 * @return
	 */
	Set getLocks() {
		try {
			return Promises.tasks {
				DistributedLock.withNewSession { session ->
					return DistributedLock.executeQuery("select name from DistributedLock distributedlock where distributedlock.name like ${namespace + '.%'} distributedlock.timeout IS NULL OR distributedlock.timeout < ${new Date().time}")
				}
			}.get()
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
	private persistenceInterceptor() {
		return grailsApp.mainContext.getBean(redisBeanName)
	}
}
