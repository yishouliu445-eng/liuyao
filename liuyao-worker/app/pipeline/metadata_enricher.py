import logging
from typing import List, Optional
from pydantic import BaseModel, Field
from openai import OpenAI
from app.schemas.chunk_models import ChunkDraft

LOGGER = logging.getLogger(__name__)

class ChunkMetadataModel(BaseModel):
    applicable_hexagrams: List[str] = Field(default_factory=list, description="Hexagram names that apply to this chunk")
    liu_qin_focus: List[str] = Field(default_factory=list, description="Related six-relatives mentioned in the chunk")
    focus_topic: Optional[str] = Field(None, description="The primary topic of this chunk, e.g. '婚姻', '财运', '寻物'")
    topic_tags: List[str] = Field(default_factory=list, description="Relevant tags for this chunk")
    scenario_types: List[str] = Field(default_factory=list, description="Scenario types such as '求财' or '婚姻'")
    knowledge_type: str = Field(..., description="Classification: 'CASE' (example/divination result), 'RULE' (general principle), or 'CONCEPT' (theoretical explanation)")
    has_timing_prediction: bool = Field(False, description="Does this chunk contain specific timing/timeline predictions (应期)?")

class MetadataEnricher:
    def __init__(self, api_key: str | None, model: str, base_url: str | None):
        self.api_key = api_key
        self.model = model
        self.base_url = base_url
        self.client = OpenAI(api_key=api_key, base_url=base_url) if api_key else None

    def enrich(self, draft: ChunkDraft) -> ChunkDraft:
        if not self.client:
            return draft
            
        system_prompt = """
You are an expert in ancient Chinese divination (Liuyao).
Analyze the provided text chunk and extract structured metadata.
- focus_topic: The main subject (e.g., career, marriage, health).
- topic_tags: List of related concepts (e.g., Use God, Void, Clash, Combine).
- knowledge_type: 'CASE' for historical examples/results, 'RULE' for general principles, 'CONCEPT' for definitions.
- has_timing_prediction: true if it discusses WHEN something will happen (应期).
"""
        
        try:
            response = self.client.beta.chat.completions.parse(
                model=self.model,
                messages=[
                    {"role": "system", "content": system_prompt},
                    {"role": "user", "content": f"Analyze this text:\n\n{draft.content}"}
                ],
                response_format=ChunkMetadataModel,
                temperature=0.1
            )
            
            enrichment = response.choices[0].message.parsed
            if enrichment:
                if enrichment.applicable_hexagrams:
                    existing_hexagrams = list(draft.metadata.get("applicable_hexagrams", []))
                    for hexagram in enrichment.applicable_hexagrams:
                        if hexagram not in existing_hexagrams:
                            existing_hexagrams.append(hexagram)
                    draft.metadata["applicable_hexagrams"] = existing_hexagrams
                if enrichment.liu_qin_focus:
                    existing_liu_qin = list(draft.metadata.get("liu_qin_focus", []))
                    for liu_qin in enrichment.liu_qin_focus:
                        if liu_qin not in existing_liu_qin:
                            existing_liu_qin.append(liu_qin)
                    draft.metadata["liu_qin_focus"] = existing_liu_qin
                if enrichment.scenario_types:
                    existing_scenarios = list(draft.metadata.get("scenario_types", []))
                    for scenario in enrichment.scenario_types:
                        if scenario not in existing_scenarios:
                            existing_scenarios.append(scenario)
                    draft.metadata["scenario_types"] = existing_scenarios
                draft.focus_topic = enrichment.focus_topic or draft.focus_topic
                if draft.focus_topic:
                    draft.metadata["focus_topic"] = draft.focus_topic
                if enrichment.topic_tags:
                    # Merge tags, unique only
                    existing = set(draft.topic_tags)
                    for t in enrichment.topic_tags:
                        existing.add(t)
                    draft.topic_tags = list(existing)
                    draft.metadata["topic_tags"] = list(draft.topic_tags)
                draft.knowledge_type = enrichment.knowledge_type
                draft.has_timing_prediction = enrichment.has_timing_prediction
                draft.metadata["knowledge_type"] = enrichment.knowledge_type
                draft.metadata["has_timing_prediction"] = enrichment.has_timing_prediction
                
        except Exception as e:
            LOGGER.warning(f"Failed to enrich chunk metadata: {e}")
            
        return draft
