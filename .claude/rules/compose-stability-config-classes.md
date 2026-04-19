---
description: >
  android/app/compose-stability.conf explicitly declares domain model classes
  as stable to prevent spurious Compose recompositions. All classes listed
  there MUST be immutable data classes.
globs: ["android/app/compose-stability.conf", "android/app/build.gradle.kts", "android/domain/src/main/java/com/rasoiai/domain/model/*.kt"]
synthesized: true
private: false
version: "1.0.0"
---

# Compose Stability Configuration

Jetpack Compose uses class stability to decide whether to skip recomposition
when a parameter is "the same" as last frame. The compiler can *infer* stability
for `data class` with all-val primitives, but it punts on anything defined
outside the current module — including our `domain` module classes when used
from `app`.

RasoiAI solves this with `android/app/compose-stability.conf`, wired into the
Compose compiler via `build.gradle.kts`. This rule governs what goes there
and how to keep it consistent.

## Wiring

`android/app/build.gradle.kts` MUST include:

```kotlin
composeCompiler {
    stabilityConfigurationFile =
        rootProject.layout.projectDirectory.file("app/compose-stability.conf")
}
```

MUST NOT duplicate this config into individual module build files — the `app`
module is the composition root and the single place stability is declared.

## What belongs in the file

The file MUST contain one fully-qualified class name per line. All listed
classes MUST satisfy BOTH:

1. Defined entirely with `val` properties (no `var`) — verified by reading
   the source.
2. Every property type is itself stable: primitives, `String`, immutable
   collections (`kotlinx.collections.immutable.*`), enums, or another class
   that appears in the config file.

Canonical list (as of this synthesis):

```
com.rasoiai.domain.model.Recipe
com.rasoiai.domain.model.Ingredient
com.rasoiai.domain.model.MealPlan
com.rasoiai.domain.model.MealPlanDay
com.rasoiai.domain.model.MealItem
com.rasoiai.domain.model.User
com.rasoiai.domain.model.UserPreferences
com.rasoiai.domain.model.Festival
com.rasoiai.domain.model.RecipeRule
com.rasoiai.domain.model.NutritionGoal
kotlinx.collections.immutable.*
```

## Adding a new domain class

When creating a new `com.rasoiai.domain.model.X`:

1. Declare it as a `data class` with all `val` properties.
2. Use `kotlinx.collections.immutable.ImmutableList` / `PersistentList` for
   any collection property (NEVER `List<Foo>` from stdlib — the compiler
   treats that as unstable).
3. Add the FQN to `compose-stability.conf`.
4. Run `./gradlew :app:assembleDebug` and scan compile output for
   "unstable param" warnings referencing your type — if any remain, fix them
   before merging.

MUST NOT declare a class stable in the config when its properties include
`MutableList`, `var`, or a non-listed type. Lying to the compiler produces
stale UI — the screen thinks nothing changed and fails to redraw.

## Collections

All list-valued properties on stable classes MUST use
`kotlinx.collections.immutable`:

```kotlin
// GOOD
data class MealPlan(
    val id: String,
    val days: ImmutableList<MealPlanDay>,
)

// BAD — List<T> makes MealPlan unstable regardless of config
data class MealPlan(
    val id: String,
    val days: List<MealPlanDay>,
)
```

## Critical constraints

- MUST NOT add a class to the config without verifying it is actually immutable.
  The config is a *claim*, not a fix — if the class has a `var`, Compose trusts
  the lie and UI bugs follow.
- MUST NOT remove `kotlinx.collections.immutable.*` from the file. Many
  domain classes carry `ImmutableList` fields; removing the wildcard breaks
  stability transitively.
- When refactoring a domain class to add mutability, REMOVE it from the config
  in the same commit. Otherwise recomposition is suppressed on a class that
  can now change silently.
- If a new field on a stable class is another custom class, that class MUST
  also be stable (listed or inferable) — transitivity is required, not just
  direct stability.
