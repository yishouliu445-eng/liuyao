from pathlib import Path
import os


def parse_txt(file_path: str) -> str:
    path = Path(file_path)
    for encoding in ("utf-8", "utf-8-sig", "gb18030"):
        try:
            return path.read_text(encoding=encoding)
        except UnicodeDecodeError:
            continue
    return path.read_text(encoding="utf-8", errors="ignore")


def parse_pdf(file_path: str) -> str:
    text = _extract_pdf_text(file_path)
    if _has_meaningful_text(text):
        return text
    return _extract_pdf_text_by_ocr(file_path)


def _extract_pdf_text(file_path: str) -> str:
    from pypdf import PdfReader

    reader = PdfReader(file_path)
    pages: list[str] = []
    for page in reader.pages:
        pages.append(page.extract_text() or "")
    return "\n".join(pages)


def _extract_pdf_text_by_ocr(file_path: str) -> str:
    import fitz
    import pytesseract

    tessdata_prefix = _resolve_tessdata_prefix()
    if tessdata_prefix:
        os.environ["TESSDATA_PREFIX"] = tessdata_prefix

    max_pages = _resolve_pdf_ocr_max_pages()
    document = fitz.open(file_path)
    pages: list[str] = []
    try:
        for index, page in enumerate(document):
            if max_pages is not None and index >= max_pages:
                break
            pixmap = page.get_pixmap(matrix=fitz.Matrix(2, 2), alpha=False)
            text = pytesseract.image_to_string(
                pixmap.pil_image(),
                lang="chi_sim+eng"
            )
            pages.append(text or "")
    finally:
        document.close()
    return "\n".join(pages)


def _has_meaningful_text(text: str) -> bool:
    compact = "".join(text.split())
    return len(compact) >= 8


def _resolve_tessdata_prefix() -> str | None:
    env_value = os.getenv("TESSDATA_PREFIX")
    if env_value:
        return env_value
    default_path = Path(__file__).resolve().parents[2] / ".tessdata"
    if default_path.exists():
        return str(default_path)
    return None


def _resolve_pdf_ocr_max_pages() -> int | None:
    raw_value = os.getenv("LIUYAO_PDF_OCR_MAX_PAGES", "80").strip()
    if not raw_value:
        return None
    try:
        value = int(raw_value)
    except ValueError:
        return 80
    if value <= 0:
        return None
    return value
