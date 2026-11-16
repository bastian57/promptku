package com.daftech.promptku.engine

// JsonLite — parser + serializer JSON minimal, Kotlin murni tanpa
// dependensi. Dipakai v2.0 untuk membaca jawaban LLM (Groq/Gemini) dan
// membangun body request.
//
// Kenapa tidak org.json: org.json hanya ada di android.jar — file engine
// yang memakainya TIDAK BISA dikompilasi kotlinc lokal (verifikasi
// offline mati). JsonLite membuat SELURUH lapisan LLM v2.0 bisa diuji
// lokal dengan JUnit. CI tetap memakai kode yang sama, bukan duplikat.
//
// Cakupan: object, array, string (escape \" \\ \/ \b \f \n \r \t dan
// \uXXXX incl. surrogate pair), number (double), true/false/null.
// Bukan cakupan: komentar, trailing comma, angka presisi arbitrary
// (double cukup untuk kuota/temperatur LLM).

sealed class Json {
    data class Obj(val map: LinkedHashMap<String, Json>) : Json() {
        fun get(key: String): Json? = map[key]
        fun str(key: String): String? = (map[key] as? Str)?.value
        fun num(key: String): Double? = (map[key] as? Num)?.value
        fun bool(key: String): Boolean? = (map[key] as? Bool)?.value
        fun arr(key: String): List<Json>? = (map[key] as? Arr)?.items
        fun obj(key: String): Obj? = map[key] as? Obj
    }

    data class Arr(val items: List<Json>) : Json()
    data class Str(val value: String) : Json()
    data class Num(val value: Double) : Json()
    data class Bool(val value: Boolean) : Json()
    object Null : Json()

    companion object {
        fun objOf(vararg pairs: Pair<String, Json>): Obj =
            Obj(LinkedHashMap<String, Json>().apply { pairs.forEach { put(it.first, it.second) } })

        fun arrOf(vararg items: Json): Arr = Arr(items.toList())
        fun strOf(value: String): Str = Str(value)
        fun numOf(value: Double): Num = Num(value)
        fun numOf(value: Int): Num = Num(value.toDouble())
        fun boolOf(value: Boolean): Bool = Bool(value)
    }
}

object JsonLite {

    class ParseError(message: String) : Exception(message)

    fun parse(text: String): Json {
        val parser = Parser(text)
        val value = parser.parseValue()
        parser.skipWs()
        if (!parser.atEnd()) throw ParseError("trailing data at ${parser.pos}")
        return value
    }

    /** Ambil objek JSON pertama dari teks (toleran terhadap teks di
     *  sekitarnya — LLM kadang membungkus JSON dengan kata-kata). */
    fun extractFirstObject(text: String): Json.Obj? {
        val start = text.indexOf('{')
        if (start < 0) return null
        var depth = 0
        var inStr = false
        var esc = false
        var i = start
        while (i < text.length) {
            val c = text[i]
            if (inStr) {
                if (esc) esc = false else if (c == '\\') esc = true else if (c == '"') inStr = false
            } else {
                when (c) {
                    '"' -> inStr = true
                    '{' -> depth++
                    '}' -> {
                        depth--
                        if (depth == 0) {
                            return try {
                                parse(text.substring(start, i + 1)) as? Json.Obj
                            } catch (e: Exception) {
                                null
                            }
                        }
                    }
                }
            }
            i++
        }
        return null
    }

    /** Ambil array JSON pertama dari teks (untuk jawaban Prompter). */
    fun extractFirstArray(text: String): Json.Arr? {
        val start = text.indexOf('[')
        if (start < 0) return null
        var depth = 0
        var inStr = false
        var esc = false
        var i = start
        while (i < text.length) {
            val c = text[i]
            if (inStr) {
                if (esc) esc = false else if (c == '\\') esc = true else if (c == '"') inStr = false
            } else {
                when (c) {
                    '"' -> inStr = true
                    '[' -> depth++
                    ']' -> {
                        depth--
                        if (depth == 0) {
                            return try {
                                parse(text.substring(start, i + 1)) as? Json.Arr
                            } catch (e: Exception) {
                                null
                            }
                        }
                    }
                }
            }
            i++
        }
        return null
    }

    fun stringify(value: Json): String {
        val sb = StringBuilder()
        write(value, sb)
        return sb.toString()
    }

    fun escape(text: String): String {
        val sb = StringBuilder()
        for (c in text) {
            when (c) {
                '"' -> sb.append("\\\"")
                '\\' -> sb.append("\\\\")
                '\n' -> sb.append("\\n")
                '\r' -> sb.append("\\r")
                '\t' -> sb.append("\\t")
                '\b' -> sb.append("\\b")
                '\u000C' -> sb.append("\\f")
                else -> {
                    if (c.code < 0x20) sb.append("\\u%04x".format(c.code))
                    else sb.append(c)
                }
            }
        }
        return sb.toString()
    }

    private fun write(value: Json, sb: StringBuilder) {
        when (value) {
            is Json.Obj -> {
                sb.append('{')
                var first = true
                for ((k, v) in value.map) {
                    if (!first) sb.append(',')
                    first = false
                    sb.append('"').append(escape(k)).append("\":")
                    write(v, sb)
                }
                sb.append('}')
            }
            is Json.Arr -> {
                sb.append('[')
                value.items.forEachIndexed { i, item ->
                    if (i > 0) sb.append(',')
                    write(item, sb)
                }
                sb.append(']')
            }
            is Json.Str -> sb.append('"').append(escape(value.value)).append('"')
            is Json.Num -> {
                val d = value.value
                if (d == kotlin.math.floor(d) && kotlin.math.abs(d) < 1e15) {
                    sb.append(d.toLong())
                } else {
                    sb.append(d.toString())
                }
            }
            is Json.Bool -> sb.append(if (value.value) "true" else "false")
            is Json.Null -> sb.append("null")
        }
    }

    private class Parser(private val text: String) {
        var pos = 0

        fun atEnd(): Boolean = pos >= text.length

        fun skipWs() {
            while (pos < text.length && text[pos] in " \t\n\r") pos++
        }

        fun parseValue(): Json {
            skipWs()
            if (pos >= text.length) throw ParseError("unexpected end at $pos")
            return when (val c = text[pos]) {
                '{' -> parseObject()
                '[' -> parseArray()
                '"' -> Json.Str(parseString())
                't' -> {
                    expect("true")
                    Json.Bool(true)
                }
                'f' -> {
                    expect("false")
                    Json.Bool(false)
                }
                'n' -> {
                    expect("null")
                    Json.Null
                }
                '-', in '0'..'9' -> parseNumber()
                else -> throw ParseError("unexpected char '$c' at $pos")
            }
        }

        private fun expect(word: String) {
            if (!text.startsWith(word, pos)) throw ParseError("expected $word at $pos")
            pos += word.length
        }

        private fun parseObject(): Json.Obj {
            pos++ // {
            val map = LinkedHashMap<String, Json>()
            skipWs()
            if (pos < text.length && text[pos] == '}') {
                pos++
                return Json.Obj(map)
            }
            while (true) {
                skipWs()
                if (pos >= text.length || text[pos] != '"') throw ParseError("expected key at $pos")
                val key = parseString()
                skipWs()
                if (pos >= text.length || text[pos] != ':') throw ParseError("expected : at $pos")
                pos++
                map[key] = parseValue()
                skipWs()
                if (pos >= text.length) throw ParseError("unterminated object")
                when (text[pos]) {
                    ',' -> pos++
                    '}' -> {
                        pos++
                        return Json.Obj(map)
                    }
                    else -> throw ParseError("expected , or } at $pos")
                }
            }
        }

        private fun parseArray(): Json.Arr {
            pos++ // [
            val items = mutableListOf<Json>()
            skipWs()
            if (pos < text.length && text[pos] == ']') {
                pos++
                return Json.Arr(items)
            }
            while (true) {
                items.add(parseValue())
                skipWs()
                if (pos >= text.length) throw ParseError("unterminated array")
                when (text[pos]) {
                    ',' -> pos++
                    ']' -> {
                        pos++
                        return Json.Arr(items)
                    }
                    else -> throw ParseError("expected , or ] at $pos")
                }
            }
        }

        private fun parseString(): String {
            pos++ // opening "
            val sb = StringBuilder()
            while (pos < text.length) {
                val c = text[pos++]
                when (c) {
                    '"' -> return sb.toString()
                    '\\' -> {
                        if (pos >= text.length) throw ParseError("unterminated escape")
                        when (val e = text[pos++]) {
                            '"' -> sb.append('"')
                            '\\' -> sb.append('\\')
                            '/' -> sb.append('/')
                            'b' -> sb.append('\b')
                            'f' -> sb.append('\u000C')
                            'n' -> sb.append('\n')
                            'r' -> sb.append('\r')
                            't' -> sb.append('\t')
                            'u' -> {
                                if (pos + 4 > text.length) throw ParseError("bad unicode escape")
                                val code = text.substring(pos, pos + 4).toIntOrNull(16)
                                    ?: throw ParseError("bad unicode escape")
                                pos += 4
                                if (code in 0xD800..0xDBFF && pos + 6 <= text.length &&
                                    text[pos] == '\\' && text[pos + 1] == 'u'
                                ) {
                                    val low = text.substring(pos + 2, pos + 6).toIntOrNull(16)
                                    if (low != null && low in 0xDC00..0xDFFF) {
                                        pos += 6
                                        sb.append(
                                            String(
                                                Character.toChars(
                                                    0x10000 + ((code - 0xD800) shl 10) + (low - 0xDC00)
                                                )
                                            )
                                        )
                                    } else {
                                        sb.append(code.toChar())
                                    }
                                } else {
                                    sb.append(code.toChar())
                                }
                            }
                            else -> throw ParseError("bad escape \\$e")
                        }
                    }
                    else -> sb.append(c)
                }
            }
            throw ParseError("unterminated string")
        }

        private fun parseNumber(): Json.Num {
            val start = pos
            if (pos < text.length && text[pos] == '-') pos++
            while (pos < text.length && (text[pos] in '0'..'9' || text[pos] in ".eE+-")) pos++
            val raw = text.substring(start, pos)
            val d = raw.toDoubleOrNull() ?: throw ParseError("bad number $raw")
            return Json.Num(d)
        }
    }
}
