package distributed.lock.gorm

import grails.util.Environment
import com.bertram.lock.DistributedLock

class DistributedLockBootStrap {


    def init = { servletContext ->
    	if(Environment.isDevelopmentMode()) {
            try {
                DistributedLock.withNewTransaction { tx ->
                    DistributedLock.executeUpdate('delete from DistributedLock')
                }    
            } catch(ex) {
                log.error("Error Flushing Dev Mode Locks: ${ex.message}",ex)
            }
            
        }
    }
    def destroy = {
    }
}
