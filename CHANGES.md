# Changelog

2024-09-30 1.5.0

* Order the jar contents so that the MANIFEST file comes first, then
  all files from the META-INF directory, then all the folders and files in
  "natural" order. It seems there are build tools that rely on the manifest
  file being the first entry in the jar. Fixes #2.

2024-02-11 1.4.0

* make inline output reproducible (support `${roject.build.outputTimestamp}`)

2023-10-22 1.3.0

* fix include/exclude code

2023-10-21 1.2.0

* fix build and site, minor internal fixes

2023-05-19 1.1.0

* dependency updates, silence some maven warnings

2022-02-23 1.0.1

* first release for public consumption
