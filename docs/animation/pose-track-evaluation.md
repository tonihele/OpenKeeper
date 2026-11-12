# KMF pose-track evaluation

OpenKeeper's KMF creature and object animations are vertex-pose animations. They
are not skeletal skinning tracks. `KmfModelLoader` converts sparse KMF vertex
poses into `PoseTrack` instances attached to jME's `AnimComposer`; gameplay
continues to select and advance actions through the existing jME animation
controls.

## Responsibilities

- `PoseTrack` remains the jME `MorphTrack` integration point. It owns the target
  geometry, pose frames, and the last evaluated stable pose key.
- `PoseTrackEvaluator` defines the replaceable evaluation strategy. It is
  injected into `PoseTrack` and `KmfModelLoader` through constructors, without
  adding a dependency-injection framework.
- `DefaultPoseTrackEvaluator` is the stateless standard implementation. It
  resolves a KMF frame, restores the mesh's bind-pose positions, applies sparse
  vertex interpolation, and marks the position buffer for upload.

The loader's no-argument constructor selects the standard evaluator so jME and
existing asset-conversion callers keep their current construction path. Custom
evaluators may opt into caching by returning a non-negative `evaluationKey`
only when that key guarantees identical output for the same track. The default
strategy keys output by the resolved KMF frame index. Evaluators that cannot
make that guarantee inherit the interface's negative-key default and are always
run.

## Correctness and lifecycle

Frame selection preserves the original KMF boundary behavior, including exact
keyframe times, times outside the clip, infinities, and NaN. Each evaluated
frame begins from the bind pose so sparse vertices do not retain values from a
previous frame. Single-frame clips resolve to their sole frame. The evaluation
cache is cleared when tracks are cloned or deserialized; clones therefore
perform their first evaluation independently.

The cache assumes no other system writes the target mesh's `Position` buffer
while this track owns its animation. If that ownership changes, disable caching
for the evaluator or add an explicit invalidation point before relying on the
cached output. No off-screen throttling, animation LOD, or parallel scene-graph
updates are enabled: those policies can affect gameplay-visible timing and have
not been shown necessary by the current captures.

## Validation and measured scope

`PoseTrackTest` compares standard evaluator output with the pre-change frame
selection and interpolation algorithm, covers sparse-pose resets and edge
times, and checks evaluator injection and clone isolation. The local-only
benchmark harness, raw captures, and comparative report are held in a separate
local performance worktree, outside the production source tree.

The measured optimization is avoiding repeated CPU pose evaluation and position
buffer updates when a 30 Hz KMF pose is sampled again at a higher render rate.
It does not change animation state transitions, AI/gameplay clocks, jME clip
selection, shaders, or the GPU skinning path.
