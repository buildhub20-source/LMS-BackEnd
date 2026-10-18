# Code runner (Java, Python, C, C++)

JavaScript answers run in the student's browser, inside a Web Worker. Other
languages have to be compiled and run somewhere safe, so the backend forwards
"Run" requests to a [Judge0](https://github.com/judge0/judge0) sandbox.

Without a sandbox configured, nothing breaks: the editor tells students that
running that language isn't set up yet, and answers are still saved and graded
after submission.

## How it works

1. The coding editor calls `POST /api/v1/code/run` with the language, the
   source and the sample inputs.
2. `CodeRunService` checks the per-user budget (30 runs a minute by default,
   each input counts) and sends each input to Judge0 with a 3 s CPU limit and
   256 MB of memory.
3. Results come back as `ACCEPTED`, `COMPILE_ERROR`, `RUNTIME_ERROR`,
   `TIME_LIMIT_EXCEEDED` or `SANDBOX_ERROR`. The browser compares stdout with
   the expected output, exactly as it does for JavaScript.

Java answers are usually written as `public class Solution`. Judge0 compiles
`Main.java`, so the backend makes that class package-private and adds a `Main`
that calls it.

## Running Judge0

Judge0 needs Docker with cgroups, so run it on its own VM, never on the
machine that holds tenant data. Follow the
[Judge0 deployment guide](https://github.com/judge0/judge0/blob/master/CHANGELOG.md#deployment-procedure),
then set:

| Variable | Example | Notes |
|---|---|---|
| `CODE_RUNNER_URL` | `http://judge0.internal:2358` | Blank turns the feature off |
| `CODE_RUNNER_API_KEY` | | Judge0's `AUTHN_TOKEN`, sent as `X-Auth-Token` |
| `CODE_RUNNER_RUNS_PER_MINUTE` | `30` | Per user |

Keep Judge0 on a private network; only the backend should reach it.

Language ids default to Judge0 CE 1.13 (`java` 62, `python` 71, `cpp` 54,
`c` 50, `javascript` 63). If your Judge0 image ships other versions, override
`lms.code-runner.language-ids` in `application.yml`.
