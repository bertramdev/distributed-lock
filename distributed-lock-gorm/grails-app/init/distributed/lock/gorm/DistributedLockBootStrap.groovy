package distributed.lock.gorm

import grails.util.Environment
import com.bertram.lock.DistributedLock

class DistributedLockBootStrap {


    def init = { servletContext ->
    	if(Environment.isDevelopmentMode()) {
            DistributedLock.withNewTransaction { tx ->
                DistributedLock.list().each { dl ->
                    dl.delete(flush:true)
                }
            }
        }
    }
    def destroy = {
    }
}
