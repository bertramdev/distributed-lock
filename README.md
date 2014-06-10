Grails Distributed Lock
================
This plugin provides a framework and interface for a synchronization mechanism that can be distributed outside the context of the app container it runs in.  In today's world of horizontal computational
scale and massive concurrency, it becomes increasingly difficult to synchronize operations outside the context of a single computational space (server/process/container).  This plugin aims to make that
easier by providing a simple service to facilitate this, as well as defining an interface for adding low level providers.

In the current release, only a provider for [redis][redis] currently exists, which depends on the [grails-redis][grails-redis] plugin. Any other providers are welcome contributions.

Things to be Done
-----------------
* Add Provider for [memcached][memcached]
* Add Provider for [gorm][gorm]
* Add Provider for [riak][riak]

Installation
------------
Add the plugin to your __BuildConfig.groovy__:

	plugins {
        compile ":distributed-lock:0.1.0"
    }
    
Configuration
-------------
First thing is to configure your redis store.  Add sample config to your __Config.groovy__:

	grails {
		redis {
			host = 'localhost'
			port = 6379
		}
	}
	
__NOTE:__ Please see [grails-redis][grails-redis] for more configuration details for your redis store

Next, configure your __distributed-lock__ options:

	distributedLock {
		provider {
			type = RedisLockProvider // Currently the only available provider
			// NOTE: Use only if not using the default redis connection
			// connection = 'otherThanDefault'
		}
		raiseError = true //optional
		defaultTimeout = 10000l //optional
		defaultTTL = 1000l * 60l * 60l //optional (1 hour)
		namespace = 'example-app' //optional
	}
	
Configuration options:

- __provider:__ This block is used to describe your implementation provider

	- __type:__ The implementation class of a low level provider (currently only __RedisLockProvider__ avail)
	- __connection:__ Used by redis provider to specify specific connection (if using [grails-redis] multi connection)
	
- __raiseError:__ Config option to throw exceptions on failures as opposed to just returning boolean status (defaults to 'true')
- __namespace:__ Specify a namespace for your lock keys (defaults to 'distributed-lock')
- __defaultTimeout:__ The default time (in millis) to wait for a lock acquire before failing (defaults to '30000')
- __defaultTTL:__ The TTL (in millis) for an active lock. If its not released in this much time, it will be force released when expired. Value defaults to 0 (never expires)

Usage
-----
The plugin provides a single non transactional service that handles all lock negotiation that you can inject in any of your services

	class MyService {
		def lockService
		
		def someMethod() {
			lockService.acquireLock('/lock/a/shared/fs')
		}
	}

__LockService Methods__

- __acquireLock(__ _String lockName, Map options = null_ __):__ attempts to acquire a lock of _lockName_ with the optional _options_. Returns true on success.
- __acquireLockByDomain(__ _Object domainInstance, Map options = null_ __):__ Convenience method to acquire a lock derived from a domain class instance. Returns true on success..
- __releaseLock(__ _String lockName, Map options = null_ __):__ release a lock _lockName_ when no longer needed. Returns true on success.
- __releaseLockByDomain(__ _Object domainInstance, Map options = null_ __):__ release a lock derived from a domain class instance. Returns true on success.
- __renewLock(__ _String lockName, Map options = null_ __):__ Can renew the lease on an expiring active lock. If no ttl specified in options, lock ceases to become volatile. Returns true on success.
- __renewLockByDomain(__ Object domainInstance, Map options = null_ __):__ Renew a lock derived from a domain class instance. Returns true on success.
- __getLocks()__: Returns a Set&lt;String&gt; of lock names that are currently active in the system.

__Options__

The optional Map allows you to override certain configuration settings just for the context of your method call.  Options include:

- __timeout:__ time in millis to wait for for the operation to complete before returning failure
- __ttl:__ time in millis for an acquired lock to expire itself if not released
- __raiseError:__ boolean instructing whether to throw an exception on failure or just return boolean status

Examples
--------
Simple usages of __LockService__:

	def acquired = lockService.acquireLock('mylock')
	
	if (acquired) {
		// Perform on operation we want synchronized
	}
	else {
		println("Unable to obtain lock")
	}
	
	// try/finally to release lock
	try {
		def lock = lockService.acquireLock('lock2', [timeout:2000l, ttl:10000l, raiseError:false])
		if (lock) {
			// DO SOME SYNCHRONIZED STUFF
		}
	}
	finally {
		lockService.releaseLock('lock2')
	}
	
Threaded sample using [executor][executor] plugin:
	
	def lockService
	
	(0..10).each { i ->
		runAsync {
			try {
				if (lockService.acquireLock('test-run', [timeout:5000l]))
					println("Lock acquired for thread ${i}")
				else
					println("Failed to acquire lock for thread ${i}")
					
				// Sleep for random amount of time
				sleep(new Random().nextInt(1000) as Long)
			}
			finally {
				lockService.releaseLock('test-run')
			}
		}
	}
	
Extending/Contributing
----------------------
To add additional providers is simple.  Simply extend the abstract __com.bertram.lock.LockProvider__ class and implement its abstract methods.  Once the new provider is implemented, it must be added to the __LockServiceConfigurer__ configuration method.  Please submit contributions via pull request.
	
[redis]: http://redis.io
[grails-redis]: http://grails.org/plugin/redis
[riak]: http://basho.com/riak
[memcached]: http://memcached.org/
[gorm]: http://grails.org/doc/latest/guide/GORM.html
[executor]: http://grails.org/plugin/executor
