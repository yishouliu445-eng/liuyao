import re

from app.chunker.base_chunker import BaseChunker
from app.schemas.chunk_models import ChunkDraft

DEFAULT_TOPICS = ["用神", "世应", "六亲", "月破", "日破", "空亡", "旬空", "动爻", "伏神", "飞神", "入墓", "冲开", "化进", "化退", "伏吟", "反吟", "应期", "神煞", "驿马", "桃花", "贵人", "文昌", "将星", "劫煞", "灾煞"]
TOPIC_ALIASES = {
    "用神": ["用神", "取用神", "用爻"],
    "世应": ["世应", "世爻", "应爻"],
    "六亲": ["六亲"],
    "月破": ["月破", "月玻"],
    "日破": ["日破", "日玻"],
    "空亡": ["空亡"],
    "旬空": ["旬空", "空亡"],
    "动爻": ["动爻", "动艾", "动区", "动攻", "动化", "发动"],
    "伏神": ["伏神", "伏藏"],
    "飞神": ["飞神", "飞伏"],
    "入墓": ["入墓", "墓库"],
    "冲开": ["冲开", "冲起"],
    "化进": ["化进", "进神"],
    "化退": ["化退", "退神"],
    "伏吟": ["伏吟"],
    "反吟": ["反吟"],
    "应期": ["应期", "应验期", "何时应", "何日应", "何时成"],
    "神煞": ["神煞", "神杀"],
    "驿马": ["驿马"],
    "桃花": ["桃花", "咸池"],
    "贵人": ["贵人", "天乙贵人"],
    "文昌": ["文昌"],
    "将星": ["将星"],
    "劫煞": ["劫煞"],
    "灾煞": ["灾煞"],
}
SPLIT_TRIGGERS = ["野鹤曰", "断曰", "占曰", "余曰", "或曰", "如占", "凡", "若", "又"]


class ClassicTextChunker(BaseChunker):
    def chunk(self, text: str, metadata: dict) -> list[ChunkDraft]:
        chunks: list[ChunkDraft] = []
        current_chapter_title = metadata.get("chapter_title")
        allow_untagged = bool(metadata.get("allow_untagged"))
        for block_index, block in enumerate(self._coarse_blocks(text), start=1):
            chapter_title, block_content = self._extract_chapter_title(block)
            if chapter_title:
                current_chapter_title = chapter_title
            if not block_content:
                continue
            for piece_index, refined in enumerate(self._refine_block(block_content), start=1):
                content = refined["content"].strip()
                if not content:
                    continue
                normalized = re.sub(r"\s+", "", content)
                if self._looks_like_table_of_contents(normalized):
                    continue
                topic_tags = self._detect_topic_tags(content)
                if not topic_tags and current_chapter_title:
                    topic_tags = self._detect_topic_tags(current_chapter_title)
                if not topic_tags and allow_untagged and self._should_skip_low_signal_untagged(content, current_chapter_title):
                    continue
                if not topic_tags and not allow_untagged:
                    continue
                content_type = self._detect_content_type(content)
                split_level = 1 if refined["split_reason"] == "coarse_block" else 2
                chunks.append(
                    ChunkDraft(
                        content=content,
                        chapter_title=current_chapter_title,
                        split_level=split_level,
                        content_type=content_type,
                        topic_tags=topic_tags,
                        focus_topic=topic_tags[0] if topic_tags else None,
                        metadata={
                            "source_block_index": block_index,
                            "source_piece_index": piece_index,
                            "split_reason": refined["split_reason"],
                            "split_strategy": refined["split_strategy"],
                            "split_trigger": refined["split_trigger"],
                            "coarse_length": len(block_content),
                            "refined_length": len(content),
                            "topic_hit_count": len(topic_tags),
                        },
                    )
                )
        return chunks

    def _coarse_blocks(self, text: str) -> list[str]:
        return [block.strip() for block in re.split(r"\n\s*\n", text) if block.strip()]

    def _refine_block(self, block: str) -> list[dict[str, str | None]]:
        if len(block) <= 280 and sum(1 for topic in DEFAULT_TOPICS if topic in block) <= 1:
            return [self._build_piece(block, "coarse_block", "none", None)]
        pattern = "(" + "|".join(re.escape(trigger) for trigger in SPLIT_TRIGGERS) + ")"
        pieces = re.split(pattern, block)
        if len(pieces) == 1:
            return self._split_by_punctuation(block)
        refined: list[dict[str, str | None]] = []
        current = ""
        for piece in pieces:
            if not piece:
                continue
            if piece in SPLIT_TRIGGERS:
                if current.strip():
                    refined.append(self._build_piece(current.strip(), "trigger_split", "trigger", self._leading_trigger(current.strip())))
                current = piece
            else:
                current += piece
        if current.strip():
            refined.append(self._build_piece(current.strip(), "trigger_split", "trigger", self._leading_trigger(current.strip())))
        return refined

    def _split_by_punctuation(self, block: str) -> list[dict[str, str | None]]:
        parts = re.split(r"(?<=[。；！？：])", block)
        merged: list[dict[str, str | None]] = []
        current = ""
        for part in parts:
            if len(current) + len(part) <= 220:
                current += part
            else:
                if current.strip():
                    merged.append(self._build_piece(current.strip(), "punctuation_split", "punctuation", None))
                current = part
        if current.strip():
            merged.append(self._build_piece(current.strip(), "punctuation_split", "punctuation", None))
        return merged or [self._build_piece(block, "coarse_block", "none", None)]

    def _build_piece(self,
                     content: str,
                     split_reason: str,
                     split_strategy: str,
                     split_trigger: str | None) -> dict[str, str | None]:
        return {
            "content": content,
            "split_reason": split_reason,
            "split_strategy": split_strategy,
            "split_trigger": split_trigger,
        }

    def _leading_trigger(self, content: str) -> str | None:
        for trigger in SPLIT_TRIGGERS:
            if content.startswith(trigger):
                return trigger
        return None

    def _extract_chapter_title(self, block: str) -> tuple[str | None, str]:
        lines = [line.strip() for line in block.split("\n") if line.strip()]
        if not lines:
            return None, ""
        first_line = lines[0]
        if not self._is_heading(first_line):
            return None, block
        remaining = "\n".join(lines[1:]).strip()
        return first_line, remaining

    def _is_heading(self, line: str) -> bool:
        if len(line) > 24:
            return False
        if any(token in line for token in ("。", "；", "！", "？", "：", "，")):
            return False
        if re.search(r"第[一二三四五六七八九十百千0-9]+[章篇节卷]", line):
            return True
        if re.search(r"[总论总诀要诀提纲纲目篇章节卷]$", line):
            return True
        if line.startswith(("用神", "世应", "六亲", "月破", "日破", "空亡", "动爻")) and len(line) <= 8:
            return True
        return len(line) <= 12 and any(topic in line for topic in DEFAULT_TOPICS)

    def _detect_content_type(self, content: str) -> str:
        if "断曰" in content or "占曰" in content or "如占" in content:
            return "example"
        if "野鹤曰" in content or "凡" in content or "若" in content:
            return "rule"
        return "mixed"

    def _detect_topic_tags(self, content: str) -> list[str]:
        normalized = re.sub(r"\s+", "", content)
        topic_tags: list[str] = []
        for topic in DEFAULT_TOPICS:
            aliases = TOPIC_ALIASES.get(topic, [topic])
            if any(alias in normalized for alias in aliases):
                topic_tags.append(topic)
        return topic_tags

    def _should_skip_low_signal_untagged(self, content: str, chapter_title: str | None) -> bool:
        normalized = re.sub(r"\s+", "", content)
        if chapter_title:
            return False
        if self._looks_like_table_of_contents(normalized):
            return True
        if len(normalized) < 16:
            return True
        if re.fullmatch(r"[\W0-9A-Za-z]+", normalized):
            return True
        if "目录" in normalized:
            return True
        return False

    def _looks_like_table_of_contents(self, normalized: str) -> bool:
        if "目录" not in normalized:
            return False
        heading_count = len(re.findall(r"第[一二三四五六七八九十百千0-9]+[章节篇卷]", normalized))
        dotted_count = len(re.findall(r"[.．。·…]{2,}\d*", normalized))
        return heading_count >= 2 or dotted_count >= 2
