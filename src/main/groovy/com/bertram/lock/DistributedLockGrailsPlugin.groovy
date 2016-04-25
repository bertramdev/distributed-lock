package com.bertram.lock

import grails.plugins.*
import com.bertram.lock.conf.LockServiceConfigurer

class DistributedLockGrailsPlugin extends Plugin {

    // the version or versions of Grails the plugin is designed for
    def grailsVersion = "3.1.5 > *"
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
        [name: "Brian Wheeler"],
        [name: 'David Estes']
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

    Closure doWithSpring() { {->
        def application = grailsApplication
        def conf = application.config.distributedLock

        if (!conf) {
            log.warn("No configuration for distributed lock has been found")
        }

        // Configure service
        def configureService = new LockServiceConfigurer(conf)
        configureService.configure(delegate, application)
        }
    }

    void doWithDynamicMethods() {
        // TODO Implement registering dynamic methods to classes (optional)
    }

    void doWithApplicationContext() {
        // TODO Implement post initialization spring config (optional)
    }

    void onChange(Map<String, Object> event) {
        // TODO Implement code that is executed when any artefact that this plugin is
        // watching is modified and reloaded. The event contains: event.source,
        // event.application, event.manager, event.ctx, and event.plugin.
    }

    void onConfigChange(Map<String, Object> event) {
        // TODO Implement code that is executed when the project configuration changes.
        // The event is the same as for 'onChange'.
    }

    void onShutdown(Map<String, Object> event) {
        // TODO Implement code that is executed when the application shuts down (optional)
    }
}
