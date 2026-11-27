# Project kibitzer

- always build and run the unit tests after making code changes
- always look for opportunities to add more unit tests
- avoid writing overly long functions, break functionality into separate smaller helper modules
- the maximum allowed line length is 120 characters

## Dual objectives

- Building project kibitzer
- Testing Hali

## Hali tool gaps
- if you notice a gap in the hali tool set, please report this to the user

## Code style

- 2 spaces, no tabs
- enforced by ktlint via `org.jmailen.kotlinter` Gradle plugin
- check: `./gradlew :app:lintKotlin`
- auto-fix: `./gradlew :app:formatKotlin`
- rules defined in `.editorconfig` at project root
