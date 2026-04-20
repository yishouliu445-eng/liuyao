import unittest

from app.chunker.classic_text_chunker import ClassicTextChunker


class ClassicTextChunkerTest(unittest.TestCase):
    def setUp(self):
        self.chunker = ClassicTextChunker()

    def test_marks_unsplit_block_as_coarse_block(self):
        chunks = self.chunker.chunk("用神宜旺相。", {})

        self.assertEqual(1, len(chunks))
        self.assertEqual("coarse_block", chunks[0].metadata["split_reason"])
        self.assertEqual("none", chunks[0].metadata["split_strategy"])
        self.assertIsNone(chunks[0].metadata["split_trigger"])

    def test_marks_trigger_split_with_trigger_name(self):
        text = "用神总论。野鹤曰用神宜旺相。断曰世应宜分看。"

        chunks = self.chunker.chunk(text, {})

        self.assertEqual(3, len(chunks))
        self.assertTrue(all(chunk.metadata["split_reason"] == "trigger_split" for chunk in chunks))
        self.assertTrue(all(chunk.metadata["split_strategy"] == "trigger" for chunk in chunks))
        self.assertIsNone(chunks[0].metadata["split_trigger"])
        self.assertEqual("野鹤曰", chunks[1].metadata["split_trigger"])
        self.assertEqual("断曰", chunks[2].metadata["split_trigger"])

    def test_marks_punctuation_split_without_trigger(self):
        text = "用神宜旺相。" * 60

        chunks = self.chunker.chunk(text, {})

        self.assertGreater(len(chunks), 1)
        self.assertTrue(all(chunk.metadata["split_reason"] == "punctuation_split" for chunk in chunks))
        self.assertTrue(all(chunk.metadata["split_strategy"] == "punctuation" for chunk in chunks))
        self.assertTrue(all(chunk.metadata["split_trigger"] is None for chunk in chunks))

    def test_uses_standalone_heading_block_as_chapter_title(self):
        text = "用神总论\n\n用神宜旺相。"

        chunks = self.chunker.chunk(text, {})

        self.assertEqual(1, len(chunks))
        self.assertEqual("用神总论", chunks[0].chapter_title)

    def test_uses_first_line_heading_and_excludes_it_from_chunk_content(self):
        text = "世应要诀\n世应之法，世为自己，应为对方。"

        chunks = self.chunker.chunk(text, {})

        self.assertEqual(1, len(chunks))
        self.assertEqual("世应要诀", chunks[0].chapter_title)
        self.assertEqual("世应之法，世为自己，应为对方。", chunks[0].content)

    def test_keeps_untagged_content_when_explicitly_allowed(self):
        text = "六爻预测学源远流长，内容丰富，适合系统学习。"

        chunks = self.chunker.chunk(text, {"allow_untagged": True})

        self.assertEqual(1, len(chunks))
        self.assertEqual([], chunks[0].topic_tags)
        self.assertIsNone(chunks[0].focus_topic)

    def test_detects_topics_from_spaced_ocr_text(self):
        text = "用 神 宜 旺 相 。 世 应 宜 分 看 。 六 亲 亦 要 分 明 。"

        chunks = self.chunker.chunk(text, {})

        self.assertEqual(1, len(chunks))
        self.assertEqual(["用神", "世应", "六亲"], chunks[0].topic_tags)

    def test_detects_topics_from_common_ocr_variants(self):
        text = "月 玻 与 动 艾 皆 要 细 看 。"

        chunks = self.chunker.chunk(text, {})

        self.assertEqual(1, len(chunks))
        self.assertEqual(["月破", "动爻"], chunks[0].topic_tags)

    def test_detects_topics_from_related_terms(self):
        text = "取用爻之后，还要看世爻、旬空与回头动化。"

        chunks = self.chunker.chunk(text, {})

        self.assertEqual(1, len(chunks))
        self.assertEqual(["用神", "世应", "旬空", "动爻"], chunks[0].topic_tags)

    def test_detects_phase_two_and_timing_topics(self):
        text = "若见伏吟反吟，当参考应期早晚。"

        chunks = self.chunker.chunk(text, {})

        self.assertEqual(1, len(chunks))
        self.assertEqual(["伏吟", "反吟", "应期"], chunks[0].topic_tags)

    def test_detects_shen_sha_topics(self):
        text = "天乙贵人与驿马、桃花、文昌、将星、劫煞、灾煞皆可入神煞参考。"

        chunks = self.chunker.chunk(text, {})

        self.assertEqual(1, len(chunks))
        self.assertEqual(["神煞", "驿马", "桃花", "贵人", "文昌", "将星", "劫煞", "灾煞"], chunks[0].topic_tags)

    def test_backfills_topic_from_chapter_title_for_ocr_pdf_blocks(self):
        text = "第二章、用神总论\n这是OCR识别出的正文，但正文里没有明确打出关键字。"

        chunks = self.chunker.chunk(text, {"allow_untagged": True})

        self.assertEqual(1, len(chunks))
        self.assertEqual(["用神"], chunks[0].topic_tags)
        self.assertEqual("用神", chunks[0].focus_topic)

    def test_skips_low_signal_ocr_noise_even_when_untagged_is_allowed(self):
        text = "尽让国神秘文化大系\n\n《六爻预测学》\n\n这是一本讲六爻预测的书，适合入门。"

        chunks = self.chunker.chunk(text, {"allow_untagged": True})

        self.assertEqual(1, len(chunks))
        self.assertEqual("这是一本讲六爻预测的书，适合入门。", chunks[0].content)

    def test_skips_table_of_contents_like_ocr_blocks_when_untagged_is_allowed(self):
        text = "目录\n第一章 用神总论........1\n第二章 世应详解........12\n第三章 动爻章法........25"

        chunks = self.chunker.chunk(text, {"allow_untagged": True})

        self.assertEqual([], chunks)


if __name__ == "__main__":
    unittest.main()
