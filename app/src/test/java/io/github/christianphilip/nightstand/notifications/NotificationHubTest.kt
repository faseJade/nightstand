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
    fun `seenApps tracks published notification apps in alphabetical order`() {
        val item1 = NotificationItem(
            key = "k1",
            packageName = "com.b.app",
            appName = "B App",
            appIcon = null,
            title = "T1",
            text = "Txt1",
            postTime = 100L,
            contentIntent = null,
            clearable = true,
            autoCancel = true
        )
        val item2 = NotificationItem(
            key = "k2",
            packageName = "com.a.app",
            appName = "A App",
            appIcon = null,
            title = "T2",
            text = "Txt2",
            postTime = 200L,
            contentIntent = null,
            clearable = true,
            autoCancel = true
        )
        NotificationHub.publish(listOf(item1, item2))

        val seen = NotificationHub.seenApps.value
        assertEquals(2, seen.size)
        assertEquals("com.a.app", seen[0].packageName)
        assertEquals("A App", seen[0].appName)
        assertEquals("com.b.app", seen[1].packageName)
        assertEquals("B App", seen[1].appName)
    }

    @Test
    fun `filtering app hides its notifications immediately`() {
        val item1 = NotificationItem(
            key = "k1",
            packageName = "com.app.one",
            appName = "App One",
            appIcon = null,
            title = "T1",
            text = "Txt1",
            postTime = 100L,
            contentIntent = null,
            clearable = true,
            autoCancel = true
        )
        val item2 = NotificationItem(
            key = "k2",
            packageName = "com.app.two",
            appName = "App Two",
            appIcon = null,
            title = "T2",
            text = "Txt2",
            postTime = 200L,
            contentIntent = null,
            clearable = true,
            autoCancel = true
        )
        NotificationHub.publish(listOf(item1, item2))

        assertEquals(2, NotificationHub.items.value.size)

        NotificationHub.setAppHidden("com.app.one", true)

        assertEquals(1, NotificationHub.items.value.size)
        assertEquals("k2", NotificationHub.items.value[0].key)

        NotificationHub.setAppHidden("com.app.one", false)

        assertEquals(2, NotificationHub.items.value.size)
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
