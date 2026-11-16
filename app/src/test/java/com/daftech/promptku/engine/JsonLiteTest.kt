package com.daftech.promptku.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class JsonLiteTest {

    @Test
    fun parsesNestedObjectAndArray() {
        val root = JsonLite.parse(
            """{"title":"Bejo","n":18,"ok":true,"x":null,"list":[{"a":1},{"a":2}]}"""
        ) as Json.Obj
        assertEquals("Bejo", root.str("title"))
        assertEquals(18.0, root.num("n"))
        assertEquals(true, root.bool("ok"))
        assertEquals(2, root.arr("list")?.size)
        assertEquals(2.0, (root.arr("list")?.get(1) as Json.Obj).num("a"))
    }

    @Test
    fun parsesEscapesAndUnicode() {
        val root = JsonLite.parse(
            """{"s":"a\"b\\c\nd\u0041\uD83D\uDE00"}"""
        ) as Json.Obj
        assertEquals("a\"b\\c\ndA\uD83D\uDE00", root.str("s"))
    }

    @Test
    fun stringifyRoundTrip() {
        val original = Json.objOf(
            "a" to Json.strOf("x\"y"),
            "b" to Json.arrOf(Json.numOf(1), Json.boolOf(false), Json.Null)
        )
        val parsed = JsonLite.parse(JsonLite.stringify(original)) as Json.Obj
        assertEquals("x\"y", parsed.str("a"))
        assertEquals(3, parsed.arr("b")?.size)
    }

    @Test
    fun extractFirstObjectToleratesSurroundingText() {
        val obj = JsonLite.extractFirstObject(
            "Here is the bible: {\"title\": \"T\", \"world_desc_en\": \"w\"} done."
        )
        assertEquals("T", obj?.str("title"))
    }

    @Test
    fun extractFirstArrayToleratesMarkdownFence() {
        val text = "```json\n[{\"scene_number\": 1, \"action_en\": \"runs\"}]\n```"
        val arr = JsonLite.extractFirstArray(text)
        assertEquals(1, arr?.items?.size)
    }

    @Test
    fun malformedJsonThrows() {
        var threw = false
        try {
            JsonLite.parse("{\"a\": }")
        } catch (e: JsonLite.ParseError) {
            threw = true
        }
        assertTrue(threw)
    }

    @Test
    fun extractFirstObjectReturnsNullWithoutBraces() {
        assertNull(JsonLite.extractFirstObject("no json here"))
    }
}
