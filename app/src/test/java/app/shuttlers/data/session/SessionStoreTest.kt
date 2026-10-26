package app.shuttlers.data.session

import org.junit.Assert.assertEquals
import org.junit.Test

class SessionStoreTest {

    @Test
    fun `courts survive an encode and decode round trip`() {
        val courts = listOf(
            SessionStore.SavedCourt(1, 10 to 11, 12 to 13),
            SessionStore.SavedCourt(3, 20 to 21, 22 to 23)
        )

        val decoded = SessionStore.decodeCourts(SessionStore.encodeCourts(courts))

        assertEquals(courts, decoded)
    }

    @Test
    fun `empty or corrupt data decodes to no courts`() {
        assertEquals(emptyList<SessionStore.SavedCourt>(), SessionStore.decodeCourts(""))
        assertEquals(emptyList<SessionStore.SavedCourt>(), SessionStore.decodeCourts("x:1,2;2:1,2,3"))
    }
}
