package com.bertram.lock

class DistributedLock {
	String name
	String value
	Long timeout //epoch timeout
    static constraints = {
    	name unique:true, nullable:false
    	value nullable:true
    	timeout nullable:true
    }
}
