import Testing
import Runfiles

@Suite("Runfiles Swift Export Tests")
struct RunfilesExportTests {
    @Test("Swift module loads cleanly")
    func testSwiftModuleLoads() {
        #expect(Bool(true), "Runfiles swift module imported cleanly")
    }
}
