import re

OCR_REPLACEMENTS = {
    "六艾": "六爻",
    "六攻": "六爻",
    "六区": "六爻",
    "六受": "六爻",
    "页测": "预测",
    "预出": "预测",
    "填例": "卦例",
    "断填": "断卦",
    "动艾": "动爻",
    "动攻": "动爻",
    "动区": "动爻",
    "月玻": "月破",
    "日玻": "日破",
    "区神": "爻神",
    "受之": "爻之",
    "受克": "爻克",
    "世芝": "世爻",
    "用芝": "用爻",
    "旬才": "旬空",
}


def clean_text(text: str) -> str:
    normalized = text.replace("\r\n", "\n").replace("\r", "\n")
    normalized = re.sub(r"[ \t]+", " ", normalized)
    normalized = re.sub(r"(?<=[\u4e00-\u9fff])\s+(?=[\u4e00-\u9fff])", "", normalized)
    normalized = re.sub(r"\n{3,}", "\n\n", normalized)
    lines = [line.strip() for line in normalized.split("\n")]
    cleaned_lines: list[str] = []
    for line in lines:
        if not line:
            cleaned_lines.append("")
            continue
        if re.fullmatch(r"[0-9一二三四五六七八九十百千]+", line):
            continue
        if "目录" in line and len(line) <= 12:
            continue
        for source, target in OCR_REPLACEMENTS.items():
            line = line.replace(source, target)
        cleaned_lines.append(line)
    text = "\n".join(cleaned_lines)
    text = re.sub(r"\n{3,}", "\n\n", text)
    return text.strip()
