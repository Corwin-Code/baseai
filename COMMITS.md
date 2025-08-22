# Git 提交规范说明

本文档定义了本项目的 Git 提交规范，旨在保持提交历史简洁、可读和一致，方便团队协作与代码追溯。
规范基于 **Conventional Commits** 标准，结合实际需求，提供 **极简 one-liner 风格**的速查表。

---

## 📌 提交消息结构

```
<type>: <short description>
```

### 说明

* **type**: 提交类型（feat, fix, chore, refactor, docs, test, perf 等）
* **short description**: 简短的修改描述（英文，推荐极简 one-liner 风格）

✅ 保持在 **一句话以内**，直观易懂
✅ 动作动词 + 对象（如 `fix: model usage`）
❌ 避免过长、模糊的描述（如 “改了一些问题”）

---

## 📌 常见提交类型

| 类型           | 说明                    | 示例                       |
| ------------ | --------------------- | ------------------------ |
| **feat**     | 新增/增强功能               | `feat: add feature`      |
| **fix**      | 修复问题/缺陷               | `fix: model usage`       |
| **chore**    | 杂项修改（配置/依赖/清理等，不影响功能） | `chore: drop deps`       |
| **refactor** | 代码重构，不影响功能            | `refactor: rename class` |
| **docs**     | 文档修改                  | `docs: update readme`    |
| **test**     | 测试相关                  | `test: add unit tests`   |
| **perf**     | 性能优化                  | `perf: speed up cache`   |
| **style**    | 代码风格（格式化等，无逻辑变更）      | `style: format code`     |

---

## 📌 中文 → 极简 Commit 词典速查表

| 类型           | 中文      | 极简 commit 写法                                   | 说明         |
| ------------ | ------- | ---------------------------------------------- | ---------- |
| **feat**     | 新增功能    | `feat: add feature`                            | 新功能提交      |
| feat         | 增强/加强   | `feat: beef up XXX` / `feat: harden XXX`       | 功能或安全强化    |
| feat         | 扩展配置    | `feat: expand config`                          | 配置项扩展      |
| feat         | 增强安全    | `feat: secure XXX` / `feat: lock down XXX`     | 安全改进       |
| **fix**      | 修复错误    | `fix: bug`                                     | 通用 bug 修复  |
| fix          | 修正用法    | `fix: model usage`                             | 修正调用/使用方式  |
| fix          | 修正逻辑    | `fix: logic`                                   | 修复业务逻辑问题   |
| **perf**     | 改进性能    | `perf: speed up XXX` / `perf: improve perf`    | 性能优化       |
| **chore**    | 小调整/优化  | `chore: tweak XXX`                             | 小幅改进       |
| chore        | 调整配置    | `chore: update config` / `chore: tweak config` | 配置修改       |
| chore        | 清理配置    | `chore: clean config`                          | 配置清理       |
| chore        | 移除日志    | `chore: drop logs`                             | 删除冗余日志     |
| chore        | 删除依赖    | `chore: drop deps`                             | 移除旧依赖      |
| chore        | 移除无用代码  | `chore: drop dead code`                        | 删除无效代码     |
| chore        | 简化逻辑    | `chore: clean logic`                           | 简化实现       |
| **refactor** | 优化实现    | `refactor: refine impl`                        | 代码优化       |
| refactor     | 重命名类/方法 | `refactor: rename XXX`                         | 类、方法、文件重命名 |
| refactor     | 移动代码    | `refactor: move XXX`                           | 代码结构调整     |
| refactor     | 重构逻辑    | `refactor: logic`                              | 重构实现       |
| refactor     | 拆分模块    | `refactor: split module`                       | 模块拆分       |
| refactor     | 合并模块    | `refactor: merge module`                       | 模块合并       |
| **docs**     | 改进文档    | `docs: update docs` / `docs: fix docs`         | 文档改进或修复    |
| **test**     | 完善测试    | `test: refine tests`                           | 测试覆盖率或质量改进 |
| test         | 增加测试    | `test: add tests`                              | 新增测试用例     |

---

## 📌 提交示例

```bash
# 新增功能
git commit -m "feat: add user login API"

# 修复问题
git commit -m "fix: model usage"

# 更新配置
git commit -m "chore: update redis config"

# 性能优化
git commit -m "perf: speed up cache"

# 文档更新
git commit -m "docs: update readme"

# 重构代码
git commit -m "refactor: rename UserService"
```

---

## 📌 总结

* **前缀必填**，保持规范统一
* **描述简洁**，推荐极简 one-liner
* **英文书写**，国际化团队更易理解
* **可溯源**，方便快速浏览提交历史

---
