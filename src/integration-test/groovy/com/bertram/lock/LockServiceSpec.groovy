package com.bertram.lock

import grails.test.mixin.integration.Integration
import grails.transaction.*
import spock.lang.*
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@Integration
class LockServiceSpec extends Specification {
	def lockService
    def setup() {
    }

    def cleanup() {
    }

    void "test acquireLock() with redis provider"() {
	    when:
	    def lock1 = lockService.acquireLock('lock1', [timeout:8000l, raiseError:true])
	    def lock2 = lockService.acquireLock('lock2', [timeout:8000l, raiseError:true])

	    then:
	    lock1
	    lock2
	    lockService.acquireLock('lock1', [timeout:1000l]) == false
	    lockService.acquireLock('lock2', [timeout:1000l]) == false

	    cleanup:
	    lockService.releaseLock('lock1')
	    lockService.releaseLock('lock2')

    }

	void "test getting all active locks"() {
		when:
		def lock1 = lockService.acquireLock('lock101')
		def lock2 = lockService.acquireLock('lock102')

		then:
		lock1
		lock2
		lockService.locks.size() == 2

		cleanup:
		lockService.releaseLock('lock101')
		lockService.releaseLock('lock102')
	}

    void "test using withLock() basic features"() {
        setup:
        def goodCounter = 0
        def badCounter = 0
        def goodLatch = new CountDownLatch(100)
        def badLatch = new CountDownLatch(100)

        when:
        (1..100).each {
            Thread.start {
                badCounter++
                badLatch.countDown()
            }
        }
        (1..100).each {
            Thread.start {
                lockService.withLock('test-withlock', [timeout:0]) {
                    goodCounter++
                    goodLatch.countDown()
                }
            }
        }
        goodLatch.await(30, TimeUnit.SECONDS)
        badLatch.await(30, TimeUnit.SECONDS)

        then:
        badCounter != 100
        goodCounter == 100
    }
}
