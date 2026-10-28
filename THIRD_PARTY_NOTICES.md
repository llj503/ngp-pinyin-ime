# Third-party notices

## CC-CEDICT (dictionary data)

`app/assets/dict.tsv.gz` is derived from **CC-CEDICT**, the community-maintained
Chinese-English dictionary published by MDBG.

- Source: https://www.mdbg.net/chinese/dictionary?page=cc-cedict
- License: **Creative Commons Attribution-ShareAlike 4.0 International**
  (CC BY-SA 4.0), https://creativecommons.org/licenses/by-sa/4.0/
- The full license text is in `dict/LICENSE-CC-BY-SA-4.0.txt`; the source
  notice is in `dict/NOTICE-CC-CEDICT.txt`.
- The derived dictionary data (`dict.tsv`, `dict.tsv.gz`) remains available
  under CC BY-SA 4.0.

## jieba (word frequency data)

The dictionary build uses the word-frequency list from **jieba** to rank
candidates.

- Source: https://github.com/fxsjy/jieba
- License: **MIT**; see `dict/LICENSE-JIEBA.txt`.

## Android SDK

The build uses the Android SDK platform/build-tools installed locally. The SDK
is not redistributed in this repository.

## Code

The application source code in this repository is licensed under the Apache
License 2.0; see `LICENSE`.
