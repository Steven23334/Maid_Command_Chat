# Touhou Little Maid: Maid Command Chat
[![Available on GitHub](https://wsrv.nl/?url=https%3A%2F%2Fcdn.jsdelivr.net%2Fnpm%2F%40intergrav%2Fdevins-badges%403%2Fassets%2Fcozy%2Favailable%2Fgithub_vector.svg&n=-1) ](https://github.com/Steven23334/Maid_Command_Chat)
[![Available for Touhou Little Maid](https://cdn.modrinth.com/data/cached_images/ea5dc160571134bd0ea89ac52075b542fb331e46_0.webp)](https://modrinth.com/project/R0bDWFAW)

This is an addon for the **Touhou Little Maid** mod. It adds a `/tellmaid` command that lets you chat with your maid's AI directly from the chat bar, without opening any GUI.

## 📦 New Content

- **`/tellmaid` command**
  Talk to your maid's AI from the chat bar, no GUI needed. No OP privileges needed.
  - `/tellmaid <maid_name> <message>` — talk to one maid.
  - `/tellmaid @m <message>` — talk to every maid you own within 128 blocks at once.
  - `/tellmaid @m[<name_a>,<name_b>,...] <message>` — talk to the listed maids at once.

  Each maid keeps her own AI memory, so every maid in a multi-maid chat replies independently.

- **Tab completion**
  Type `/tellmaid ` and press Tab to list nearby maids that belong to you, plus the `@m` option. Inside `@m[...]` Tab keeps suggesting your maids so you can build the list. Names with spaces (or, inside a list, commas or brackets) are auto-wrapped in quotes.

- **Chat feedback**
  The message you sent is echoed in gray in your chat bar, including the names of all maids it was sent to. If the command cannot find a maid (or the maid does not belong to you, or no maid is within 128 blocks), the reason is reported in chat instead of failing silently. Missing or incomplete arguments (for example only a maid name, or an unclosed `@m[...]`) show the vanilla red incomplete-command/error prompt and are not executed.

## 🎯 Purpose

- Lets players talk to their maid's AI without leaving the chat bar.
- Lets players talk to one maid, several maids, or every nearby maid at once.
- Provides a convenient command-friendly way to trigger AI conversations.
- Adds a lightweight alternative for players who prefer typing over clicking.

## 🔧 Installation

1. Make sure the **Touhou Little Maid** mod is installed.
2. Place the `.jar` file of this mod into the `.minecraft/mods` folder.
3. Launch the game.

## ⚠️ Requirements

- **Minecraft Versions**: 1.21.1
- **Required Mod**: [Touhou Little Maid](https://modrinth.com/project/R0bDWFAW)

## 📜 License

- Code: [MIT License](https://mit-license.org/)

## 🙏 Authors

- Programmer: Steven23334
---------
# 车万女仆：女仆指令聊天

[![可在 GitHub 上获取](https://wsrv.nl/?url=https%3A%2F%2Fcdn.jsdelivr.net%2Fnpm%2F%40intergrav%2Fdevins-badges%403%2Fassets%2Fcozy%2Favailable%2Fgithub_vector.svg&n=-1) ](https://github.com/Steven23334/Maid_Command_Chat)
[![适用于车万女仆](https://cdn.modrinth.com/data/cached_images/ea5dc160571134bd0ea89ac52075b542fb331e46_0.webp)](https://modrinth.com/project/R0bDWFAW)

这是一个为 **车万女仆 (Touhou Little Maid)** 模组开发的拓展，添加了一条 `/tellmaid` 指令，让你可以直接从聊天栏和女仆的 AI 对话，无需打开任何界面。

## 📦 新增内容

- **`/tellmaid` 指令**
  从聊天栏直接和女仆 AI 说话，无需打开界面。无需管理员权限。
  - `/tellmaid <女仆名> <消息>` —— 和单个女仆对话。
  - `/tellmaid @m <消息>` —— 同时和附近 128 格内所有属于你的女仆对话。
  - `/tellmaid @m[<女仆a>,<女仆b>,……] <消息>` —— 同时和列表中的女仆对话。

  每个女仆的 AI 记忆是独立的，所以多人对话时每位女仆都会各自回复。

- **Tab 补全**
  输入 `/tellmaid ` 后按 Tab，会列出附近属于你的女仆，以及 `@m` 选项；在 `@m[...]` 中按 Tab 会继续补全女仆名。名字含空格（在列表中还包括逗号、方括号）时会自动补全为带引号的形式。

- **聊天栏回显**
  发出的消息会以灰色显示在聊天栏，并列出所有接收消息的女仆。找不到女仆、女仆不属于你、128 格内没有女仆、语法错误等情况都会在聊天栏给出红色提示，不会静默失败。参数不完整（例如只写了女仆名、`@m[...]` 没闭合）时会显示原版的红色“命令不完整/错误”提示，并拒绝执行。

## 🎯 模组用途

- 让玩家不用离开聊天栏就能和女仆 AI 对话。
- 支持和单个女仆、指定的多个女仆或附近所有女仆同时对话。
- 提供一个命令友好的方式触发 AI 对话。
- 为更喜欢打字的玩家提供一个轻量级的替代方案。

## 🔧 安装方法

1. 确保已安装 **车万女仆 (Touhou Little Maid)** 模组。
2. 将本模组的 `.jar` 文件放入 `.minecraft/mods` 文件夹。
3. 启动游戏即可。

## ⚠️ 前置要求

- **Minecraft 版本**：1.21.1
- **必需模组**：[车万女仆 (Touhou Little Maid)](https://modrinth.com/project/R0bDWFAW)

## 📜 许可证

- 代码：[MIT License](https://mit-license.org/)

## 🙏 作者

- 程序：Steven23334