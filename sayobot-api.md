# Sayobot (osu.sayobot.cn) API 文档

> 基于对 https://osu.sayobot.cn 网站的前端代码逆向和网络请求抓包整理
> 生成日期: 2026-05-17

---

## 基础域名

| 域名 | 用途 |
|------|------|
| `api.sayobot.cn` | API 主服务 |
| `txy1.sayobot.cn` | 下载重定向服务 |
| `b{N}.sayobot.cn:25225` | 实际下载 CDN 节点 (N=1,2,3,4,5,6...) |
| `a.sayobot.cn` | 谱面封面图片 CDN |
| `cdnx.sayobot.cn:25225` | 预览音频 CDN |
| `dl.sayobot.cn` | 谱面文件直接下载 CDN |
| `osugame.sayobot.cn` | 谱面在线预览 |
| `osu.sayobot.cn` | 前端页面 (Angular SPA) |

---

## API 端点

### 1. 谱面列表 (POST)

获取谱面列表，支持搜索、最新、热门等模式。

```
POST https://api.sayobot.cn/?post
Content-Type: text/plain
```

**请求体 (JSON):**

```json
{
  "cmd": "beatmaplist",
  "limit": 25,
  "offset": 0,
  "type": "new"
}
```

**参数:**

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `cmd` | string | 是 | 固定值 `"beatmaplist"` |
| `limit` | int | 是 | 每页数量，默认 `25` |
| `offset` | int | 是 | 偏移量，分页用。首页为 `0`，后续为上次响应的 `endid` |
| `type` | string | 是 | 搜索类型：`"new"` (最新)、`"hot"` (热门)、`"search"` (关键词搜索) |
| `keyword` | string | 否 | 搜索关键词，仅 `type="search"` 时有效 |

**响应:**

```json
{
  "status": 0,
  "endid": 25,
  "data": [
    {
      "approved": 1,
      "artist": "DJ SHARPNEL",
      "artistU": "",
      "creator": "Ratarok",
      "favourite_count": 473,
      "lastupdate": 1768210710,
      "modes": 1,
      "order": 9.764,
      "play_count": 120279,
      "sid": 2403321,
      "title": "Marunouchi surviver",
      "titleU": ""
    }
  ]
}
```

**响应字段说明:**

| 字段 | 说明 |
|------|------|
| `status` | 状态码，`0` 为成功 |
| `endid` | 当前页末尾 ID，用于下一次请求的 `offset` |
| `data[].sid` | 谱面集 ID (Set ID) |
| `data[].approved` | 审核状态: `1`=Ranked, `2`=Approved, `3`=Qualified, `4`=Loved, `5`=Pending, `0`=WIP/Graveyard |
| `data[].modes` | 游戏模式位掩码: `1`=std, `2`=taiko, `4`=ctb, `8`=mania |
| `data[].title` | 谱面标题 |
| `data[].titleU` | 标题 Unicode (日文/中文原名) |
| `data[].artist` | 艺术家 |
| `data[].artistU` | 艺术家 Unicode |
| `data[].creator` | 谱面创建者 |
| `data[].play_count` | 游玩次数 |
| `data[].favourite_count` | 收藏数 |
| `data[].lastupdate` | 最后更新时间 (Unix 时间戳) |

---

### 2. 谱面详情 (GET)

通过谱面集 ID (Set ID) 获取详细信息，包括所有难度数据。

```
GET https://api.sayobot.cn/v2/beatmapinfo?0={sid}
```

**参数:**

| 参数 | 位置 | 说明 |
|------|------|------|
| `0` (Query) | 谱面集 ID | 如 `767294` |

> 注意: 参数名为 `0`，不是 `sid`。当输入为纯数字时，前端调用此 API。

**响应:**

```json
{
  "status": 0,
  "data": {
    "sid": 767294,
    "title": "far in the blue sky...",
    "titleU": "",
    "artist": "saikoro",
    "artistU": "",
    "creator": "1nar",
    "creator_id": 11724727,
    "approved": 4,
    "approved_date": 1613580020,
    "bpm": 133.2,
    "genre": 10,
    "language": 5,
    "tags": "trance bms electronic instrumental stepmania",
    "source": "",
    "favourite_count": 1013,
    "last_update": 1613577230,
    "local_update": 1778998576,
    "preview": 1,
    "video": 0,
    "storyboard": 0,
    "bids_amount": 9,
    "bid_data": [
      {
        "bid": 1612787,
        "version": "42 [0.9]",
        "mode": 3,
        "star": 6.847,
        "AR": 5.0,
        "CS": 4.0,
        "OD": 8.0,
        "HP": 8.0,
        "circles": 3127,
        "sliders": 40,
        "spinners": 0,
        "length": 206,
        "maxcombo": 0,
        "passcount": 15607,
        "playcount": 88336,
        "audio": "audio90.mp3",
        "bg": "bg.png"
      }
    ]
  }
}
```

**响应字段说明:**

顶层字段:

| 字段 | 说明 |
|------|------|
| `preview` | 是否有音频预览 (`1`=有) |
| `video` | 是否有视频 (`1`=有) |
| `storyboard` | 是否有故事板 (`1`=有) |
| `bids_amount` | 难度数量 |
| `genre` | 颗材类型 |
| `language` | 语言分类 |

难度字段 (`bid_data[]`):

| 字段 | 说明 |
|------|------|
| `bid` | 单个难度 ID (Beatmap ID) |
| `version` | 难度名称 |
| `mode` | 游戏模式 (0=std, 1=taiko, 2=ctb, 3=mania) |
| `star` | 星级 |
| `AR` | Approach Rate |
| `CS` | Circle Size |
| `OD` | Overall Difficulty |
| `HP` | HP Drain |
| `length` | 时长 (秒) |
| `audio` | 音频文件名 (用于直接下载) |
| `bg` | 背景图片文件名 |

---

### 3. 公告列表 (GET)

```
GET https://api.sayobot.cn/notice
```

**响应:**

```json
{
  "data": [
    {
      "title": "公告标题",
      "user_id": "_BlackC",
      "date": "2020-03-07",
      "content": "公告内容",
      "importance": 0,
      "img": []
    }
  ]
}
```

---

### 4. 广告数据 (GET)

```
GET https://api.sayobot.cn/static/ad
```

**响应:**

```json
{
  "data": [
    {
      "title": "广告标题",
      "img1000x150": "图片URL",
      "uri": "跳转链接"
    }
  ]
}
```

---

### 5. 下载服务器列表 (GET)

```
GET https://api.sayobot.cn/static/servers
```

返回可用的下载 CDN 服务器列表。

---

### 6. 赞助/支持列表 (GET)

```
GET https://api.sayobot.cn/v2/support
```

返回赞助者信息。

---

## 下载 API (谱面文件)

下载 API 不直接返回文件，而是通过重定向服务分发到最近的 CDN 节点。

### 下载流程

1. 请求重定向 URL (302)
2. 重定向到实际 CDN 节点下载

### 下载类型

| 类型 | 重定向 URL 模式 | 说明 |
|------|-----------------|------|
| 完整版 | `https://txy1.sayobot.cn/beatmaps/download/full/{sid}?server={server}` | 含视频的完整 .osz 文件 |
| 无视频 | `https://txy1.sayobot.cn/beatmaps/download/novideo/{sid}?server={server}` | 不含视频的 .osz 文件 |
| Mini | `https://txy1.sayobot.cn/beatmaps/download/mini/{sid}?server={server}` | 仅含谱面和音频的精简版 |

### 参数

| 参数 | 说明 |
|------|------|
| `{sid}` | 谱面集 ID (Set ID) |
| `server` | 下载服务器选择: `"auto"` (自动选择) 或具体服务器名 |

### 重定向示例

```
请求: GET https://txy1.sayobot.cn/beatmaps/download/full/767294?server=auto
响应: 302 → https://b6.sayobot.cn:25225/beatmaps/76/7294/full?filename=767294%20saikoro%20-%20far%20in%20the%20blue%20sky
```

### 重定向后 URL 结构

```
https://{server}/{sid的前两位}/{sid的后几位}/{type}?filename={sid} {artist} - {title}
```

---

## CDN 静态资源 URL

### 谱面封面图片

```
https://a.sayobot.cn/beatmaps/{sid}/covers/cover.webp?0
```

### 预览音频 (mp3)

```
https://cdnx.sayobot.cn:25225/preview/{sid}.mp3
```

### 谱面内音频文件 (通过 dl CDN)

```
https://dl.sayobot.cn/beatmaps/files/{sid}/{audio_filename}
```

其中 `{audio_filename}` 来自 `/v2/beatmapinfo` 响应中 `bid_data[].audio` 字段。

### 在线预览页面

```
https://osugame.sayobot.cn/preview.html?sid={sid}&bid={bid}
```

---

## 前端路由

| URL | 说明 |
|-----|------|
| `/home` | 首页 |
| `/home?search={keyword}` | 搜索结果页 |
| `/home/downloadcenter` | 下载中心 |

---

## 游戏模式 (modes) 位掩码

| 值 | 模式 |
|----|------|
| `1` | osu!standard |
| `2` | osu!taiko |
| `4` | osu!catch |
| `8` | osu!mania |

组合值: `3` = std+taiko, `9` = std+mania, `11` = std+taiko+mania, `15` = 全模式

---

## 审核状态 (approved)

| 值 | 状态 |
|----|------|
| `-2` | Graveyard |
| `-1` | WIP |
| `0` | Pending |
| `1` | Ranked |
| `2` | Approved |
| `3` | Qualified |
| `4` | Loved |

---

## 示例: 用 ID 767294 搜索并下载

### 步骤 1: 获取谱面详情

```bash
curl "https://api.sayobot.cn/v2/beatmapinfo?0=767294"
```

返回谱面 "saikoro - far in the blue sky..." 的详细信息 (sid=767294, 9 个难度, mania 模式)。

### 步骤 2: 下载完整版

```bash
curl -L -o "767294 saikoro - far in the blue sky.osz" \
  "https://txy1.sayobot.cn/beatmaps/download/full/767294?server=auto"
```

文件大小约 19.2MB。

### 步骤 3: 下载无视频版

```bash
curl -L -o "767294 saikoro - far in the blue sky.osz" \
  "https://txy1.sayobot.cn/beatmaps/download/novideo/767294?server=auto"
```

### 步骤 4: 下载 mini 版

```bash
curl -L -o "767294 saikoro - far in the blue sky.osz" \
  "https://txy1.sayobot.cn/beatmaps/download/mini/767294?server=auto"
```

---

## 注意事项

1. **无需认证**: 所有 API 均无需登录或 Token
2. **无请求频率限制**: 据网站说明，没有冷却时间
3. **CORS**: API 响应头包含 `Access-Control-Allow-Origin: *`，支持跨域调用
4. **搜索逻辑**: 输入纯数字时当作 Set ID 直接查详情 (`/v2/beatmapinfo`)；非数字时按关键词搜索 (`beatmaplist` + `type=search`)
5. **分页**: 使用栈式分页，`offset` 从 `0` 开始，后续使用响应中的 `endid`
6. **默认每页**: 25 条
