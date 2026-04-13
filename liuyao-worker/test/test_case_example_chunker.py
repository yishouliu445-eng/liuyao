import unittest

from app.chunker.case_example_chunker import CaseExampleChunker


class TestCaseExampleChunker(unittest.TestCase):
    def setUp(self):
        self.chunker = CaseExampleChunker()

    def test_case_preservation(self):
        text = "某人占功名，得天火同人。\n\n断曰：此卦大吉，必得高升。\n\n后果然高中。"

        chunks = self.chunker.chunk(text, {})

        self.assertEqual(1, len(chunks))
        self.assertEqual("case_example", chunks[0].content_type)
        self.assertEqual("CASE", chunks[0].knowledge_type)
        self.assertEqual("case_preservation", chunks[0].metadata["split_reason"])
        self.assertEqual("断曰", chunks[0].metadata["split_trigger"])
        self.assertIn("断曰：此卦大吉，必得高升。", chunks[0].content)
        self.assertIn("后果然高中。", chunks[0].content)

    def test_preserves_multi_paragraph_case_without_topic_tags(self):
        text = "如占求财，得天火同人。\n\n断曰：财可得。\n\n后果然得财。"

        chunks = self.chunker.chunk(text, {})

        self.assertEqual(1, len(chunks))
        self.assertEqual("case_example", chunks[0].content_type)
        self.assertEqual([], chunks[0].topic_tags)
        self.assertEqual("CASE", chunks[0].knowledge_type)
        self.assertIn("后果然得财。", chunks[0].content)

    def test_detect_content_type(self):
        text = "如占求财，得雷雨鼓舞。断曰：财源滚滚。"
        content_type = self.chunker._detect_content_type(text)
        self.assertEqual(content_type, "case_example")

    def test_fallback_for_non_case(self):
        text = "这是一段普通的理论。并没有卦例。这里有个句号。这里还有个。"
        # This should fallback to standard punctuation splitting if it's long enough,
        # but for short texts, ClassicTextChunker might keep it together.
        # Let's use a long text to force a split.
        long_text = "理论开始。" + "理论延续。" * 80
        blocks = self.chunker._refine_block(long_text)
        # Should use punctuation_split
        self.assertTrue(any(b["split_reason"] == "punctuation_split" for b in blocks))


if __name__ == "__main__":
    unittest.main()
