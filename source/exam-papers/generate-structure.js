// 动态文件结构生成脚本（GitHub 远程版）
// 该脚本可在 Node.js 环境运行，从 GitHub 仓库读取目录树并生成文件结构 JSON
//
// 默认目标仓库：@njdldkl666699/Another-NKUSE-Exams
// 可通过环境变量覆盖：GITHUB_OWNER, GITHUB_REPO, GITHUB_REF, GITHUB_TOKEN
// 可通过环境变量覆盖顶层目录列表：GITHUB_TOP_DIRS（逗号分隔，如：软件专业课,通识必修课）
// 可通过环境变量控制只读指定 ref 且不回退：GITHUB_STRICT_REF=1
// 可通过环境变量控制输出打印：GITHUB_PRINT_JSON=1
//
// 运行：node generate-structure.js
// 结果：控制台输出提示，同时保存到同目录 file-structure.json

"use strict";

import { writeFileSync } from "fs";
import { join, extname, dirname } from "path";
import { request } from "https";
import { fileURLToPath, pathToFileURL } from "url";

// ESM 等价的 __filename 和 __dirname
const __filename = fileURLToPath(import.meta.url);
const __dirname = dirname(__filename);

// 稳健判断是否直接运行（避免路径编码/平台差异导致比较失败）
let IS_MAIN = false;
try {
  const invoked = process.argv[1] ? pathToFileURL(process.argv[1]).href : "";
  IS_MAIN = import.meta.url === invoked;
} catch {
  IS_MAIN = false;
}

// ====================== GitHub 远程扫描实现 ======================

const DEFAULT_OWNER = process.env.GITHUB_OWNER || "njdldkl666699";
const DEFAULT_REPO = process.env.GITHUB_REPO || "Another-NKUSE-Exams";
const DEFAULT_REF = process.env.GITHUB_REF || "main";
const GITHUB_TOKEN = process.env.GITHUB_TOKEN || "";

// 顶层目录列表（可通过环境变量 GITHUB_TOP_DIRS 或 TOP_DIRS 覆盖，逗号分隔）
const DEFAULT_TOP_DIRS = (() => {
  const env = process.env.GITHUB_TOP_DIRS || process.env.TOP_DIRS;
  if (env && typeof env === "string") {
    const arr = env
      .split(",")
      .map((s) => s.trim())
      .filter(Boolean);
    if (arr.length > 0) return arr;
  }
  // 你当前项目的默认顶层目录
  return ["通识必修课", "专业必修课", "专业选修课"];
})();

// 乱码防治默认设置
const DEFAULT_FILTER_INVALID_NAMES = true; // 过滤包含 U+FFFD 的名称
const DEFAULT_UNICODE_NORMALIZATION = "NFC"; // 统一规范化名称：NFC/NFD/false
const DEFAULT_STRICT_REF = process.env.GITHUB_STRICT_REF === "1" || /^true$/i.test(process.env.GITHUB_STRICT_REF || "");

// GitHub API 基础请求
function fetchJSON(url, token = "") {
  return new Promise((resolve, reject) => {
    const u = new URL(url);
    const options = {
      protocol: u.protocol,
      hostname: u.hostname,
      path: u.pathname + (u.search || ""),
      headers: {
        "User-Agent": "generate-structure-script",
        Accept: "application/vnd.github+json",
      },
      method: "GET",
    };
    if (token) {
      options.headers.Authorization = `Bearer ${token}`;
    }

    const req = request(options, (res) => {
      // 处理重定向
      if (res.statusCode >= 300 && res.statusCode < 400 && res.headers.location) {
        res.resume(); // 丢弃当前响应数据
        fetchJSON(res.headers.location, token).then(resolve).catch(reject);
        return;
      }

      let data = "";
      res.on("data", (chunk) => (data += chunk));
      res.on("end", () => {
        if (res.statusCode >= 200 && res.statusCode < 300) {
          try {
            const json = JSON.parse(data || "{}");
            resolve(json);
          } catch (e) {
            reject(new Error(`Failed to parse JSON from ${url}: ${e.message}`));
          }
        } else {
          reject(new Error(`HTTP ${res.statusCode} for ${url}: ${data}`));
        }
      });
    });

    req.on("error", reject);
    req.end();
  });
}

async function getRepo(owner, repo) {
  const url = `https://api.github.com/repos/${owner}/${repo}`;
  return fetchJSON(url, GITHUB_TOKEN);
}

async function getCommit(owner, repo, ref) {
  // 显式指向分支的 heads/<branch> 形式，有助于避免歧义
  const refOrHeads = ref.startsWith("heads/") ? ref : `heads/${ref}`;
  const url = `https://api.github.com/repos/${owner}/${repo}/commits/${encodeURIComponent(refOrHeads)}`;
  return fetchJSON(url, GITHUB_TOKEN);
}

async function getTree(owner, repo, treeSha, recursive = false) {
  const url = `https://api.github.com/repos/${owner}/${repo}/git/trees/${treeSha}${recursive ? "?recursive=1" : ""}`;
  return fetchJSON(url, GITHUB_TOKEN);
}

function shouldIgnoreDir(name) {
  return name.startsWith(".") || name.startsWith("node_modules");
}

const excludedFileExts = new Set([".js", ".html", ".yml", ".yaml", ".json", ".md"]);

function shouldIncludeFile(fileName) {
  if (fileName.startsWith(".")) return false;
  const ext = extname(fileName).toLowerCase();
  if (excludedFileExts.has(ext)) return false;
  return true;
}

function sanitizeSegment(
  seg,
  { filterInvalidNames = DEFAULT_FILTER_INVALID_NAMES, unicodeNormalization = DEFAULT_UNICODE_NORMALIZATION } = {}
) {
  let s = seg;
  if (unicodeNormalization && typeof s.normalize === "function") {
    try {
      s = s.normalize(unicodeNormalization);
    } catch {
      // ignore normalization errors
    }
  }
  // 过滤包含 Unicode 替换字符的名称（多见于非 UTF-8 文件名被 GitHub 替换）
  if (filterInvalidNames && s.includes("\uFFFD")) {
    return null;
  }
  return s;
}

function safeAddFile(target, segments) {
  // segments: ["子目录", "文件名"]
  let obj = target;
  for (let i = 0; i < segments.length - 1; i++) {
    const seg = segments[i];
    if (!obj[seg]) obj[seg] = {};
    obj = obj[seg];
  }
  if (!obj.files) obj.files = [];
  obj.files.push(segments[segments.length - 1]);
}

function hasIgnoredSegment(segments) {
  return segments.some((seg) => shouldIgnoreDir(seg));
}

function buildStructureFromTreeEntries(targetRootObject, entries, opts = {}) {
  // entries 为 subtree 相对路径的列表
  for (const e of entries) {
    if (e.type !== "blob") continue; // 只在遇到文件时落盘，目录会在需要时自动创建
    const rawSegs = e.path.split("/").filter(Boolean);
    if (rawSegs.length === 0) continue;

    if (hasIgnoredSegment(rawSegs.slice(0, -1))) continue;

    // 规范化与过滤乱码片段
    const segs = [];
    let drop = false;
    for (const raw of rawSegs) {
      const s = sanitizeSegment(raw, opts);
      if (s == null) {
        drop = true;
        break;
      }
      segs.push(s);
    }
    if (drop) continue;

    // 如需按扩展名过滤，可启用下行
    // const fileName = segs[segs.length - 1];
    // if (!shouldIncludeFile(fileName)) continue;

    safeAddFile(targetRootObject, segs);
  }
}

/**
 * 解析 ref 对应的提交与根树；当 strictRef=true 时，不回退默认分支
 */
async function resolveRootTree(owner, repo, ref, { strictRef = DEFAULT_STRICT_REF } = {}) {
  try {
    const commit = await getCommit(owner, repo, ref);
    return {
      commitSha: commit.sha,
      treeSha: commit.commit?.tree?.sha,
      branchUsed: ref,
      fallbackUsed: false,
    };
  } catch (e) {
    if (strictRef) {
      throw new Error(`获取分支 ${ref} 失败且 strictRef=true：${e.message}`);
    }
    // 回退到默认分支
    const repoInfo = await getRepo(owner, repo);
    const fallback = repoInfo.default_branch || "main";
    const commit = await getCommit(owner, repo, fallback);
    return {
      commitSha: commit.sha,
      treeSha: commit.commit?.tree?.sha,
      branchUsed: fallback,
      fallbackUsed: true,
    };
  }
}

/**
 * 从 GitHub 仓库构建指定顶层目录的结构
 * @param {object} options
 * @param {string} options.owner
 * @param {string} options.repo
 * @param {string} options.ref
 * @param {string[]} options.topLevelDirs 顶层目录列表
 * @param {boolean} options.strictRef 严格使用 ref（失败不回退）
 * @param {boolean} options.filterInvalidNames 过滤包含 U+FFFD 的名称
 * @param {"NFC"|"NFD"|false} options.unicodeNormalization 名称规范化
 */
async function generateFileStructureFromGitHub({
  owner = DEFAULT_OWNER,
  repo = DEFAULT_REPO,
  ref = DEFAULT_REF,
  topLevelDirs = DEFAULT_TOP_DIRS,
  strictRef = DEFAULT_STRICT_REF,
  filterInvalidNames = DEFAULT_FILTER_INVALID_NAMES,
  unicodeNormalization = DEFAULT_UNICODE_NORMALIZATION,
} = {}) {
  const structure = {};

  const { treeSha, branchUsed, fallbackUsed } = await resolveRootTree(owner, repo, ref, { strictRef });
  if (!treeSha) {
    throw new Error(`无法获取根树 SHA：${owner}/${repo}@${branchUsed}`);
  }
  if (fallbackUsed) {
    console.warn(
      `[提示] 指定的 ref "${ref}" 不可用，已回退到默认分支 "${branchUsed}"。如需严格仅使用指定分支，请设置 strictRef=true 或 GITHUB_STRICT_REF=1。`
    );
  }

  // 拉取根树（非递归），定位顶层目录
  const rootTree = await getTree(owner, repo, treeSha, false);
  if (!rootTree || !Array.isArray(rootTree.tree)) {
    throw new Error(`无法获取根树内容：${owner}/${repo}@${branchUsed}`);
  }

  const rootDirShaByName = new Map();
  for (const item of rootTree.tree) {
    if (item.type === "tree" && topLevelDirs.includes(item.path)) {
      // 名称同样做一次规范化匹配（允许 topLevelDirs 传入未规范化字符串）
      const norm =
        typeof item.path.normalize === "function" && unicodeNormalization
          ? item.path.normalize(unicodeNormalization)
          : item.path;
      for (const userDir of topLevelDirs) {
        const userNorm =
          typeof userDir.normalize === "function" && unicodeNormalization
            ? userDir.normalize(unicodeNormalization)
            : userDir;
        if (norm === userNorm) {
          rootDirShaByName.set(userDir, item.sha);
          break;
        }
      }
    }
  }

  // 分别递归抓取各顶层目录，避免整仓库 recursive 截断
  for (const dirName of topLevelDirs) {
    const sha = rootDirShaByName.get(dirName);
    if (!sha) continue; // 仓库中不存在该目录则跳过

    const subtree = await getTree(owner, repo, sha, true);

    if (subtree.truncated) {
      console.warn(
        `[警告] 目录 ${dirName} 的树被 GitHub 截断（truncated=true）。生成结果可能不完整，可考虑拆分子目录单独抓取或使用 GraphQL API 分页。`
      );
    }

    const obj = {};
    buildStructureFromTreeEntries(obj, Array.isArray(subtree.tree) ? subtree.tree : [], {
      filterInvalidNames,
      unicodeNormalization,
    });

    // 即使没有匹配文件，也保留空对象表示目录存在
    structure[dirName] = obj;
  }

  return structure;
}

// 如果直接运行此脚本
if (IS_MAIN) {
  (async () => {
    try {
      const structure = await generateFileStructureFromGitHub({
        topLevelDirs: DEFAULT_TOP_DIRS,
        strictRef: DEFAULT_STRICT_REF,
        filterInvalidNames: DEFAULT_FILTER_INVALID_NAMES,
        unicodeNormalization: DEFAULT_UNICODE_NORMALIZATION,
      });

      const printJson = process.env.GITHUB_PRINT_JSON === "1" || /^true$/i.test(process.env.GITHUB_PRINT_JSON || "");

      if (printJson) {
        console.log(JSON.stringify(structure, null, 2));
      } else {
        console.log(
          `已从 GitHub 生成文件结构（分支：${DEFAULT_REF}${
            DEFAULT_STRICT_REF ? "，strict" : ""
          }；顶层目录：${DEFAULT_TOP_DIRS.join(", ")}）`
        );
      }

      // 保存到文件
      const outputPath = join(__dirname, "file-structure.json");
      writeFileSync(outputPath, JSON.stringify(structure, null, 2), "utf8");
      console.log(`\nFile structure saved to: ${outputPath}`);
    } catch (err) {
      console.error("生成文件结构失败：", err?.message || err);
      process.exitCode = 1;
    }
  })();
}

export default {
  // 远程 GitHub 结构生成（异步）
  generateFileStructureFromGitHub,
};
