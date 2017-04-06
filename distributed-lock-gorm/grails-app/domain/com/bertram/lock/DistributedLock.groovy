package com.bertram.lock

class DistributedLock {
	String key
	String value
	Long timeout //epoch timeout
    static constraints = {
    	key unique:true, nullable:false
    	value nullable:true
    	timeout nullable:true
    }
}
