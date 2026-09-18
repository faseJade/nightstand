package io.github.christianphilip.nightstand.notifications

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NotificationHubTest {

    private fun createItem(key: String, clearable: Boolean) = NotificationItem(
        key = key,
        packageName = "com.pkg",
        appName = "App",
        appIcon = null,
        title = "Title",
        text = "Text",
        postTime = 0L,
        contentIntent = null,
        clearable = clearable,
        autoCancel = true
    )

    @Test
    fun `initial state is empty and disconnected`() {
        // NotificationHub is a singleton, let's reset it first just in case
        NotificationHub.publish(emptyList())

        assertTrue(NotificationHub.items.value.isEmpty())
        assertFalse(NotificationHub.connected.value)
    }

    @Test
    fun `publish updates items`() {
        val list = listOf(createItem("k1", true))
        NotificationHub.publish(list)

        assertEquals(1, NotificationHub.items.value.size)
        assertEquals("k1", NotificationHub.items.value[0].key)
    }

    @Test
    fun `dismiss removes the correct item`() {
        val list = listOf(createItem("k1", true), createItem("k2", true))
        NotificationHub.publish(list)

        NotificationHub.dismiss("k1")

        assertEquals(1, NotificationHub.items.value.size)
        assertEquals("k2", NotificationHub.items.value[0].key)
    }

    @Test
    fun `clearAll removes only clearable items`() {
        val list = listOf(
            createItem("k1", true),
            createItem("k2", false)
        )
        NotificationHub.publish(list)

        NotificationHub.clearAll()

        assertEquals(1, NotificationHub.items.value.size)
        assertEquals("k2", NotificationHub.items.value[0].key)
    }
}
