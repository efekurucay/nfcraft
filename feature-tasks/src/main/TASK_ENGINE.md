# Task engine integration

All Kotlin APIs below are in `com.nfcraft.feature.tasks`.

```kotlin
val factory = createDefaultTaskExecutorFactory(
    guard = DangerousTaskGuard { descriptor ->
        confirmationUi.confirm(descriptor)
    },
)
val codec = DefaultTaskCodec(factory)
val runner = TaskSequenceRunner(factory)
// In a coroutine, after decoding records:
val result = runner.execute(context, records)
```

`DangerousTaskGuard` declares `suspend fun confirm(descriptor: TaskDescriptor): Boolean`.
The assembly function is
`fun createDefaultTaskExecutorFactory(guard: DangerousTaskGuard = DangerousTaskGuard.Deny): DefaultTaskExecutorFactory`.
The zero-argument `DefaultTaskExecutorFactory()` also assembles executors, with confirmation denied.
No task runs during construction, catalog access, decoding, or executor lookup.

Descriptors are canonicalized by MIME identity at execution. All executable dangerous tasks,
including app uninstall and every root task, call the guard and return `Aborted` on denial.
Root tasks execute arbitrary privileged commands. `RootShell` is a low-level primitive whose
caller must authorize execution; use `RootTaskExecutor` for guarded task execution.
Root shell candidates are attempted in order when process creation fails. A started command
is never retried after a nonzero exit, because doing so could repeat a destructive action.
Both output streams are drained concurrently, retaining up to 64 KiB each, with a 30-second
deadline and process cleanup. Cancelling the calling coroutine propagates cancellation.

## Catalog and support

Numbers in each category are assigned explicitly and must stay stable. The supplied protocol
reference documents categories but does not enumerate task numbers or parameter schemas.
These identities therefore define this module's catalog; interoperability with another suite's
individual task identities cannot be established from that reference.

| Category | Descriptors | Implemented task identities |
| --- | ---: | ---: |
| Network | 24 | 14 |
| Bluetooth | 16 | 5 |
| Phone | 20 | 9 |
| Alarm & files | 24 | 10 |
| Sound | 24 | 17 |
| Screen | 20 | 13 |
| Configuration | 24 | 21 |
| Apps & misc | 28 | 20 |
| Conditions | 20 | 20 |
| Root | 20 | 20 |
| Total | 220 | 149 |

There are eleven executor families. `executorFor` returns null for the 71 catalog-only
identities. Unsupported operations never silently succeed. Intent tasks report successful
handoff, which does not imply that a user completed the requested action in the receiving app.

Volume parameters are absolute stream indices; brightness is 0–255. Ringer mode accepts
`normal`, `silent`, or `vibrate`; booleans accept `true` or `false`. Coordinates are degrees.
Hours use 0–23; a condition's hour interval includes both endpoints and can cross midnight.
Delays are milliseconds and are capped at one day; vibrations are capped at one minute.
Optional parameters can be omitted at the end or represented by empty slots in the middle.

## Conditions and model limits

`TaskSequenceRunner` validates balanced if/else/end blocks before running actions, supports
nested blocks, skips inactive branches, and stops on failure or abort. Conditions 1–18 open
blocks, 19 means else, and 20 closes a block. Evaluated conditions return `ConditionBranch`;
else/end return a structural `ConditionBranch(true)` when called individually. They need the
runner for sequence semantics. The runner keeps no state between executions.

`Tasks.kt` provides neither structured condition markers nor sequence execution state.
It also has no result value for HTTP bodies, shell output, or other query results. This
implementation discards HTTP bodies and exposes shell output through `RootShell.Result`;
root task failures include a bounded stderr excerpt. The model has no enum choices, units,
range metadata, permission requirements, or explicit catalog-only capability flag.

The pipe protocol has no escaping or argument-count field. Serialization rejects pipes
inside arguments and an empty single argument, which cannot round-trip distinctly from
zero arguments. Parsing checks required slots, maximum arity, numeric values, and booleans.
Execution also validates ranges specific to each action.

The guard receives only a descriptor, not arguments. A confirmation UI that displays the
actual privileged command must obtain the current record from its calling application.

## Device requirements and networking

The module manifest declares required permissions, but executors never prompt for runtime
or special access. The host must manage camera and Bluetooth runtime grants, system-settings
write access, and notification policy access. Missing access yields `Failure`. WiFi mutation
is restricted on Android 10 and above, and Bluetooth mutation on Android 13 and above;
settings/request intents remain available. Package visibility can limit app lookup and
installed-app conditions. Intent handlers, camera flash, vibration, alarms, clipboard access,
background activity launches, and manufacturer restrictions need validation on a device.
File sharing requires a content URI and a grant from its provider. Picker tasks launch UI;
the TaskExecutor contract has no mechanism to return the selected document.

HTTP uses HttpURLConnection on the IO dispatcher, blocks local/private resolved addresses
with `h(host)`, disables proxies and redirects, bounds request/response bodies to 1 MiB,
and sets 15-second connect/read timeouts. Basic authentication requires HTTPS. HTTP status
2xx succeeds; other status codes fail. Android network security policy can reject cleartext
HTTP; no application-wide cleartext exemption is enabled here. DNS validation is a preflight
check: HttpURLConnection resolves again, so DNS rebinding protection requires a trusted
resolver/network or an additional transport that pins the validated destination.

Root operations require a rooted device and explicit guard approval. None were executed.
No build, test command, or device validation was run for this implementation.
