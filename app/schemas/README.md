# Room schema gate

The first Android build generates the authoritative version-1 JSON schema here.
It has **not** been fabricated by hand. Commit the generated JSON after the first green
Android build, before releasing 1.0. Future database versions require explicit migrations
and MigrationTestHelper tests. Destructive fallback is prohibited.
