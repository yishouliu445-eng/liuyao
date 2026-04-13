import re
from app.chunker.classic_text_chunker import ClassicTextChunker
from app.schemas.chunk_models import ChunkDraft

CASE_TRIGGERS = ["断曰", "占曰", "如占", "即如", "余占", "凡占"]
CASE_ENDINGS = ["应在", "此即", "后果然", "断为", "其验", "此乃"]
CASE_START_PATTERN = re.compile(
    "|".join(
        [
            r"占.{1,10}得.{2,8}卦",
            r"某人占",
            r"如占",
            r"余测",
            r"余占",
            r"有人占",
            r"一人占",
            r"又占",
        ]
    )
)

class CaseExampleChunker(ClassicTextChunker):
    """
    专门用于提取古籍中完整卦例的分块器。
    它识别卦例触发词（如'如占'），并尽可能保持卦例内容的完整性，不被通用标点强行切断。
    """

    def chunk(self, text: str, metadata: dict) -> list[ChunkDraft]:
        chunks: list[ChunkDraft] = []
        current_chapter_title = metadata.get("chapter_title")
        allow_untagged = bool(metadata.get("allow_untagged"))
        blocks = self._coarse_blocks(text)
        block_index = 0

        while block_index < len(blocks):
            source_block_index = block_index + 1
            block = blocks[block_index]
            block_index += 1

            chapter_title, block_content = self._extract_chapter_title(block)
            if chapter_title:
                current_chapter_title = chapter_title
            if not block_content:
                continue

            if self._has_case_start(block_content):
                case_content, consumed_count = self._collect_case_content(blocks, block_index - 1, block_content)
                block_index += consumed_count
                chunks.extend(
                    self._build_case_chunks(
                        case_content,
                        current_chapter_title,
                        source_block_index,
                        allow_untagged,
                    )
                )
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
                            "source_block_index": source_block_index,
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

    def _refine_block(self, block: str) -> list[dict[str, str | None]]:
        # 识别是否包含卦例触发词
        has_case_trigger = any(trigger in block for trigger in CASE_TRIGGERS)
        
        # 如果包含卦例触发词，且块长度在合理范围（例如 1000 字以内），则尝试保持作为一个整体
        if has_case_trigger and len(block) <= 1000:
            return [self._build_piece(block, "case_preservation", "case_dedicated", self._leading_case_trigger(block))]
        
        # 否则回退到父类的通用切割逻辑
        return super()._refine_block(block)

    def _detect_content_type(self, content: str) -> str:
        if any(trigger in content for trigger in CASE_TRIGGERS):
            return "case_example"
        return super()._detect_content_type(content)

    def _has_case_start(self, content: str) -> bool:
        return bool(CASE_START_PATTERN.search(content))

    def _collect_case_content(self, blocks: list[str], start_index: int, first_block: str) -> tuple[str, int]:
        collected = [first_block.strip()]
        consumed = 0
        index = start_index + 1
        while index < len(blocks):
            next_block = blocks[index]
            next_title, next_content = self._extract_chapter_title(next_block)
            if next_title:
                break
            if self._has_case_start(next_block):
                break
            if next_content:
                collected.append(next_content.strip())
            consumed += 1
            index += 1
        return "\n\n".join(part for part in collected if part), consumed

    def _build_case_chunks(self,
                           content: str,
                           chapter_title: str | None,
                           source_block_index: int,
                           allow_untagged: bool) -> list[ChunkDraft]:
        normalized = re.sub(r"\s+", "", content)
        if not normalized:
            return []
        if self._looks_like_table_of_contents(normalized):
            return []
        topic_tags = self._detect_topic_tags(content)
        if not topic_tags and chapter_title:
            topic_tags = self._detect_topic_tags(chapter_title)
        if not topic_tags and not allow_untagged:
            # 卦例优先保留，即便没有明确主题标签，也不要丢失内容
            topic_tags = []
        return [
            ChunkDraft(
                content=content,
                chapter_title=chapter_title,
                split_level=1,
                content_type="case_example",
                topic_tags=topic_tags,
                focus_topic=topic_tags[0] if topic_tags else None,
                knowledge_type="CASE",
                metadata={
                    "source_block_index": source_block_index,
                    "source_piece_index": 1,
                    "split_reason": "case_preservation",
                    "split_strategy": "case_dedicated",
                    "split_trigger": self._leading_case_trigger(content),
                    "coarse_length": len(content),
                    "refined_length": len(content),
                    "topic_hit_count": len(topic_tags),
                },
            )
        ]

    def _leading_case_trigger(self, content: str) -> str | None:
        earliest_trigger = None
        earliest_index = None
        for trigger in CASE_TRIGGERS:
            index = content.find(trigger)
            if index < 0:
                continue
            if earliest_index is None or index < earliest_index:
                earliest_index = index
                earliest_trigger = trigger
        return earliest_trigger
