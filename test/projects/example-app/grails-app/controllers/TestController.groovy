class TestController {
	def lockService

	def testLock() {
		def locked = lockService.acquireLock('lock1', [timeout:3000l])

		render("Lock acquired? ${locked}")
	}
}