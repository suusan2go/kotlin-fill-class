[![CircleCI](https://circleci.com/gh/suusan2go/kotlin-fill-class.svg?style=svg)](https://circleci.com/gh/suusan2go/kotlin-fill-class)
[![jetBrains](https://img.shields.io/jetbrains/plugin/d/10942-kotlin-fill-class.svg)](https://plugins.jetbrains.com/plugin/10942-kotlin-fill-class)

# kotlin-fill-class plugin
Intellij plugin that provide intention action for empty constructor or function to fill property with default value.
Inspired by Go [fillstruct](https://github.com/davidrjenni/reftools/tree/master/cmd/fillstruct)

## Usage
This plugin add intention action for invalid constructor or function expression.
![kotlin fill class demo](https://user-images.githubusercontent.com/8841470/59397528-e61a4380-8dc7-11e9-9684-d82d225316fe.gif)

### Configure settings
You can configure the plugin settings by `Edit inspection profile setting`
![Edit inspection profile setting](https://user-images.githubusercontent.com/1121855/107631811-f4a9b400-6ca8-11eb-9ea8-1b0b56f0fda9.png)

Currently this plugins supports two options.
- Fill arguments without default values
- Do not fill default arguments

## How to install
Install from jetbrains plugins repository.
https://plugins.jetbrains.com/plugin/10942-kotlin-fill-class

## Thanks
- [@t-kameyama](https://github.com/t-kameyama) Fill function call arguments [#17](https://github.com/suusan2go/kotlin-fill-class/pull/17)
- [@shiraji](https://github.com/shiraji) Convert to intention & Support non-empty constructor [#6](https://github.com/suusan2go/kotlin-fill-class/pull/6)
- [@Pluu](https://github.com/Pluu) [@naofumi-fujii](https://github.com/naofumi-fujii]) fix #2 Double type fill error [#3](https://github.com/suusan2go/kotlin-fill-class/pull/3)
