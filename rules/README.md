# Rules Resource Governance

`rules/v1/` is the current versioned rule directory for maintained JSON resources.

`rule_definitions.json` is the version manifest for configuration rules. Category-specific rule files in the same directory are the maintained source files for that bundle.

`metadata.json` records the bundle-level version identifiers used by the current rule set.

Runtime resources live under `liuyao-app/src/main/resources/rules/v1/`. The files in `rules/v1/` mirror that active version so the repository root keeps a reviewable rule snapshot outside the packaged application tree.
