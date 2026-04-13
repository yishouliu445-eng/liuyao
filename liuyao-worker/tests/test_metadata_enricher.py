import unittest
from types import SimpleNamespace

from app.pipeline.metadata_enricher import MetadataEnricher
from app.schemas.chunk_models import ChunkDraft


class _FakeCompletions:
    def __init__(self, response=None, error=None):
        self.response = response
        self.error = error
        self.calls = []

    def parse(self, **kwargs):
        self.calls.append(kwargs)
        if self.error is not None:
            raise self.error
        return self.response


class _FakeClient:
    def __init__(self, response=None, error=None):
        self.beta = SimpleNamespace(
            chat=SimpleNamespace(
                completions=_FakeCompletions(response=response, error=error),
            )
        )


class MetadataEnricherTest(unittest.TestCase):
    def test_enrich_merges_llm_metadata_into_chunk(self):
        response = SimpleNamespace(
            choices=[
                SimpleNamespace(
                    message=SimpleNamespace(
                            parsed=SimpleNamespace(
                                applicable_hexagrams=["天火同人", "天火同人"],
                                liu_qin_focus=["妻财"],
                                focus_topic="婚姻",
                                topic_tags=["空亡", "应期"],
                                scenario_types=["婚姻", "婚姻"],
                                knowledge_type="CASE",
                                has_timing_prediction=True,
                            )
                    )
                )
            ]
        )
        enricher = MetadataEnricher(api_key="sk-test", model="mock", base_url=None)
        enricher.client = _FakeClient(response=response)
        draft = ChunkDraft(
            content="如占婚姻，断曰后期可成。",
            topic_tags=["用神"],
            focus_topic=None,
        )

        enriched = enricher.enrich(draft)

        self.assertIs(enriched, draft)
        self.assertEqual("婚姻", enriched.focus_topic)
        self.assertEqual({"用神", "空亡", "应期"}, set(enriched.topic_tags))
        self.assertEqual("CASE", enriched.knowledge_type)
        self.assertTrue(enriched.has_timing_prediction)
        self.assertEqual(["天火同人"], enriched.metadata["applicable_hexagrams"])
        self.assertEqual(["妻财"], enriched.metadata["liu_qin_focus"])
        self.assertEqual(["婚姻"], enriched.metadata["scenario_types"])
        self.assertEqual("婚姻", enriched.metadata["focus_topic"])
        self.assertEqual("CASE", enriched.metadata["knowledge_type"])
        self.assertTrue(enriched.metadata["has_timing_prediction"])

    def test_enrich_returns_original_chunk_when_llm_call_fails(self):
        enricher = MetadataEnricher(api_key="sk-test", model="mock", base_url=None)
        enricher.client = _FakeClient(error=RuntimeError("boom"))
        draft = ChunkDraft(content="如占求财，断曰财可得。", topic_tags=["用神"])

        enriched = enricher.enrich(draft)

        self.assertIs(enriched, draft)
        self.assertEqual(["用神"], enriched.topic_tags)
        self.assertIsNone(enriched.knowledge_type)
        self.assertFalse(enriched.has_timing_prediction)

    def test_enrich_is_noop_without_client(self):
        enricher = MetadataEnricher(api_key=None, model="mock", base_url=None)
        draft = ChunkDraft(content="如占求财，断曰财可得。")

        enriched = enricher.enrich(draft)

        self.assertIs(enriched, draft)
        self.assertEqual([], enriched.topic_tags)


if __name__ == "__main__":
    unittest.main()
