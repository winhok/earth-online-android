# Room schema baseline

`xyz.winhok.earthonline.data.EarthDatabase/1.json` is the real Room-exported schema from the successful 1.0.0 build, not a handwritten reconstruction.

Source: `cd3fc2162e819d3a3f5b2513a82b4bbd47e12d94`; Actions run `34195496596`, `release-build-evidence` artifact `10043833991`.

Database version: 1. Identity hash: `33a5ff2908984032f2b24765c9d62fc8`.

This file is an archival baseline; adding it after release does not change the published binary. Future entity changes must increment the database version and include migration tests from this schema. Do not silently enable destructive migration.
