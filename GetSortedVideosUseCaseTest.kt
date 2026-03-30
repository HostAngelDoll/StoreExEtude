class GetSortedVideosUseCaseTest {
    private lateinit var getSortedVideosUseCase: GetSortedVideosUseCase

    @Before
    fun setUp() {
        // initialization logic
    }

    @Test
    fun testMethod1() {
        getSortedVideosUseCase = lazy { GetSortedVideosUseCase() }.value
        // test logic
    }

    @Test
    fun testMethod2() {
        getSortedVideosUseCase = lazy { GetSortedVideosUseCase() }.value
        // test logic
    }
}