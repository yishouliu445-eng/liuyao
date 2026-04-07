import unittest
import os
from unittest.mock import patch

from app.parser.txt_parser import parse_pdf, _resolve_pdf_ocr_max_pages


class PdfParserTest(unittest.TestCase):
    def test_returns_direct_pdf_text_when_it_is_meaningful(self):
        with patch("app.parser.txt_parser._extract_pdf_text", return_value="六爻预测学\n用神总论\n世应章"), \
                patch("app.parser.txt_parser._extract_pdf_text_by_ocr") as ocr_mock:
            text = parse_pdf("/tmp/test.pdf")

        self.assertIn("用神总论", text)
        ocr_mock.assert_not_called()

    def test_falls_back_to_ocr_when_direct_pdf_text_is_blank(self):
        with patch("app.parser.txt_parser._extract_pdf_text", return_value="\n \n\t"), \
                patch("app.parser.txt_parser._extract_pdf_text_by_ocr", return_value="OCR识别出的正文\n用神章") as ocr_mock:
            text = parse_pdf("/tmp/test.pdf")

        self.assertIn("OCR识别出的正文", text)
        ocr_mock.assert_called_once_with("/tmp/test.pdf")

    def test_uses_default_ocr_page_limit_when_env_missing(self):
        with patch.dict(os.environ, {}, clear=False):
            self.assertEqual(80, _resolve_pdf_ocr_max_pages())

    def test_disables_ocr_page_limit_when_env_is_zero(self):
        with patch.dict(os.environ, {"LIUYAO_PDF_OCR_MAX_PAGES": "0"}, clear=False):
            self.assertIsNone(_resolve_pdf_ocr_max_pages())


if __name__ == "__main__":
    unittest.main()
