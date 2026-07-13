package com.dreamjournal.app.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EntryContentCodecTest {
    @Test
    fun emptyStoredBlockRecoversLegacyTranscript() {
        val raw = EntryContentCodec.encode(listOf(EntryContentBlock.text(value = "")))

        val blocks = EntryContentCodec.decode(raw, legacyContent = "这是语音转写结果")

        assertEquals("这是语音转写结果", EntryContentCodec.plainText(blocks))
    }

    @Test
    fun transcriptAppendsToTextAlreadyTypedByUser() {
        val typed = listOf(EntryContentBlock.text(value = "我先手动写了一句"))

        val blocks = EntryContentCodec.appendTextIfMissing(typed, "稍后到达的语音转写")

        assertEquals("我先手动写了一句\n\n稍后到达的语音转写", EntryContentCodec.plainText(blocks))
    }

    @Test
    fun sameTranscriptIsNotInsertedTwice() {
        val initial = listOf(EntryContentBlock.text(value = "不要重复我"))

        val blocks = EntryContentCodec.appendTextIfMissing(initial, "不要重复我")

        assertEquals("不要重复我", EntryContentCodec.plainText(blocks))
        assertTrue(blocks.count { it.type == ContentBlockType.TEXT } >= 1)
    }
}
