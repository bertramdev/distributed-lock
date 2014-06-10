import example.app.Example

class TestController {
	def lockService

	def testLock() {
		def locked = lockService.acquireLock('lock1', [timeout:3000l])

		render("Lock acquired? ${locked}")
	}

	def testLockByDomain() {
		// setup
		def domain = new Example([name:"My Example", description:"Testing domain class locking"]).save()

		def locked = lockService.acquireLockByDomain(domain)

		if (locked) {
			render("Locked by domain object ${domain} acquired")
		}
		else {
			render("failed to acquire lock for domain object")
		}
	}

	def releaseLockByDomain() {
		def domain = Example.list()[0]

		def released = lockService.releaseLockByDomain(domain)

		if (released) {
			render("Domain lock released for ${domain}")
		}
		else {
			render("failed to release domain lock for ${domain}")
		}
	}
}