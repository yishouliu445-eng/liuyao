import json
import logging
from typing import Any

from openai import OpenAI
from pydantic import BaseModel, Field

from app.schemas.rule_models import RuleCandidateDraft

LOGGER = logging.getLogger(__name__)

class RuleCandidateModel(BaseModel):
    rule_title: str = Field(description="Short, descriptive title of the rule")
    category: str | None = Field(None, description="Theme or category of the rule, e.g. YONGSHEN_STATE, USE_GOD_SELECTION")
    condition_desc: str | None = Field(None, description="Description of the trigger condition")
    effect_direction: str | None = Field(None, description="Effect direction, e.g. 'POSITIVE', 'NEGATIVE', 'NEUTRAL'")
    evidence_text: str | None = Field(None, description="The original text evidence from the source text")
    confidence: float = Field(0.0, description="Confidence score from 0.0 to 1.0 of the extraction accuracy")

class RuleExtractionResult(BaseModel):
    rules: list[RuleCandidateModel] = Field(description="Extracted rule candidates")

class RuleExtractor:
    def __init__(self, api_key: str | None, model: str, base_url: str | None):
        self.api_key = api_key
        self.model = model
        self.base_url = base_url
        self.client = OpenAI(api_key=api_key, base_url=base_url) if api_key else None

    def extract_rules(self, text: str) -> list[RuleCandidateDraft]:
        if not self.client:
            LOGGER.warning("No LLM API key configured. Skipping extraction.")
            return []
        
        system_prompt = """
You are an expert in ancient Chinese divination (Liuyao).
Your task is to extract divination rules from the provided classic text chunks.
Extract any identifiable rules, focusing on how a specific condition affects an outcome.
If there are no rules, return an empty list.

Extract the title, category, condition description, effect direction, and the precise original text as evidence.
"""
        
        try:
            response = self.client.beta.chat.completions.parse(
                model=self.model,
                messages=[
                    {"role": "system", "content": system_prompt},
                    {"role": "user", "content": f"Extract rules from the following text:\n\n{text}"}
                ],
                response_format=RuleExtractionResult,
                temperature=0.1
            )
            
            parsed = response.choices[0].message.parsed
            if not parsed or not parsed.rules:
                return []
            
            drafts = []
            for r in parsed.rules:
                drafts.append(RuleCandidateDraft(
                    rule_title=r.rule_title,
                    category=r.category,
                    condition_desc=r.condition_desc,
                    effect_direction=r.effect_direction,
                    evidence_text=r.evidence_text,
                    confidence=r.confidence
                ))
            return drafts
        except Exception as e:
            LOGGER.exception("Failed to extract rules using LLM")
            return []
