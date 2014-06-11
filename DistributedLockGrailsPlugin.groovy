import com.bertram.lock.conf.LockServiceConfigurer

class DistributedLockGrailsPlugin {
    // the plugin version
    def version = "0.1.0"
    // the version or versions of Grails the plugin is designed for
    def grailsVersion = "2.2 > *"
    // resources that are excluded from plugin packaging
    def pluginExcludes = [
        "grails-app/views/error.gsp"
    ]

    // TODO Fill in these fields
    def title = "Distributed Lock Plugin" // Headline display name of the plugin
    def author = "Jordon Saardchit"
    def authorEmail = "jsaardchit@bcap.com"
	def organization = [ name: "Bertram Labs", url: "http://www.bertramlabs.com/" ]
	def developers = [
		[name: "Brian Wheeler"]
	]
    def description = '''\
This plugin provides a framework and interface for a synchronization mechanism distributed to multiple server instances.  In today's world of horizontal computational
scale and massive concurrency, it becomes increasingly difficult to synchronize operations outside the context of a single computational space (server/process).  This plugin aims to make that
easier by providing a simple service to facilitate this, as well as defining an interface for adding low level providers.
'''

    // URL to the plugin's documentation
    def documentation = "http://grails.org/plugin/distributed-lock"
	def issueManagement = [system: 'github', url: 'https://github.com/bertramdev/distributed-lock/issues']
	def license = "APACHE"
	def scm = [url: "https://github.com/bertramdev/distributed-lock"]

    def doWithWebDescriptor = { xml ->
        // TODO Implement additions to web.xml (optional), this event occurs before
    }

    def doWithSpring = {
	    def conf = application.config.distributedLock

	    if (!conf) {
		    log.warn("No configuration for distributed lock has been found")
	    }

	    // Configure service
	    def configureService = new LockServiceConfigurer(conf)
	    configureService.configure(delegate, application)

    }

    def doWithDynamicMethods = { ctx ->
        // TODO Implement registering dynamic methods to classes (optional)
    }

    def doWithApplicationContext = { ctx ->
        // TODO Implement post initialization spring config (optional)
    }

    def onChange = { event ->
        // TODO Implement code that is executed when any artefact that this plugin is
        // watching is modified and reloaded. The event contains: event.source,
        // event.application, event.manager, event.ctx, and event.plugin.
    }

    def onConfigChange = { event ->
        // TODO Implement code that is executed when the project configuration changes.
        // The event is the same as for 'onChange'.
    }

    def onShutdown = { event ->
        // TODO Implement code that is executed when the application shuts down (optional)
    }
}
