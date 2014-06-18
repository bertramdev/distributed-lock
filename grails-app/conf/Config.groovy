import com.bertram.lock.provider.RedisLockProvider

// configuration for plugin testing - will not be included in the plugin zip

log4j = {
	root {
		info 'stdout'
		additivity = true
	}
    // Example of changing the log pattern for the default console
    // appender:
    //
    //appenders {
    //    console name:'stdout', layout:pattern(conversionPattern: '%c{2} %m%n')
    //}

    error  'org.codehaus.groovy.grails.web.servlet',  //  controllers
           'org.codehaus.groovy.grails.web.pages', //  GSP
           'org.codehaus.groovy.grails.web.sitemesh', //  layouts
           'org.codehaus.groovy.grails.web.mapping.filter', // URL mapping
           'org.codehaus.groovy.grails.web.mapping', // URL mapping
           'org.codehaus.groovy.grails.commons', // core / classloading
           'org.codehaus.groovy.grails.plugins', // plugins
           'org.codehaus.groovy.grails.orm.hibernate', // hibernate integration
           'org.springframework',
           'org.hibernate',
           'net.sf.ehcache.hibernate'

	appenders {
		console name:'stdout', layout: pattern(conversionPattern: '%d{yyyy-MM-dd HH:mm:ss.SSS} | %p | %c | %t | %m | %x%n')
	}
}

// Using a basic redis config to test redis provider
grails {
	redis {
		host = 'localhost'
		port = 6379
	}
}

// Sample config
environments {
	development {
		distributedLock {
			provider {
				type = RedisLockProvider
				// NOTE: Use only if not using the default redis connection
				// connection = 'otherThanDefault'
			}
			raiseError = true
			defaultTimeout = 10000l
			// namespace = 'my.locking.namespace' // USE: if you want a different namespace than the default for your lock keys
		}
	}
	test {
        distributedLock {
            provider {
                type = RedisLockProvider
            }
            raiseError = true
            defaultTimeout = 30 * 1000
            expireTimeout = 0
            // namespace = 'my.locking.namespace' // USE: if you want a different namespace than the default for your lock keys
        }
	}
}
