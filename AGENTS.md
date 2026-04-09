Follow Plan.md milestone by milestone.
Operate as non-interactively as current permissions allow.
Do not pause between milestones unless one of these is true:
1. approval is required by the environment,
2. a secret / credential is missing,
3. requirements are ambiguous,
4. validation fails and multiple valid fixes exist.

Within the workspace, make edits, run tests, and continue automatically.
After each milestone, run the listed validation commands and immediately fix failures before proceeding.
If the task can be parallelized, explicitly spawn subagents for independent workstreams.