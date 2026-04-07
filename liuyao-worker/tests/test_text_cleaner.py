import unittest

from app.cleaner.text_cleaner import clean_text


class TextCleanerTest(unittest.TestCase):
    def test_compacts_spaced_cjk_ocr_lines(self):
        text = "六 艾 预 测 学\n用 神 总 论\n世 应 之 法"

        cleaned = clean_text(text)

        self.assertIn("六爻预测学", cleaned)
        self.assertIn("用神总论", cleaned)
        self.assertIn("世应之法", cleaned)

    def test_replaces_common_ocr_variants(self):
        text = "六攻预测学\n动艾发动\n月玻逢破"

        cleaned = clean_text(text)

        self.assertIn("六爻预测学", cleaned)
        self.assertIn("动爻发动", cleaned)
        self.assertIn("月破逢破", cleaned)


if __name__ == "__main__":
    unittest.main()
