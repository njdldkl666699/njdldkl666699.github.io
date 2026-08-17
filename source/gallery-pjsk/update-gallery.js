import { readFile, writeFile, readdir } from "node:fs/promises";
import path from "node:path";
import { fileURLToPath } from "node:url";

const __dirname = path.dirname(fileURLToPath(import.meta.url));

const galleryDir = __dirname;
const imagesDir = path.resolve(galleryDir, "../images");
const indexFile = path.join(galleryDir, "index.md");

// 匹配 card_数字_normal.webp / card_数字_trained.webp
const CARD_RE = /^card_(\d+)_(normal|trained)\.webp$/;

/** 格式化为 YYYY-MM-DD HH:mm:ss（本地时间） */
function formatTime(date) {
  const pad = (n) => String(n).padStart(2, "0");
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`;
}

async function main() {
  // 1. 扫描 images 目录，收集每张卡的 normal / trained 图片
  const files = await readdir(imagesDir);
  const cards = new Map(); // 数字 -> { normal: boolean, trained: boolean }

  for (const file of files) {
    const m = file.match(CARD_RE);
    if (m) {
      const num = Number(m[1]);
      if (!cards.has(num)) cards.set(num, { normal: false, trained: false });
      cards.get(num)[m[2]] = true;
    }
  }

  // 2. 按卡号升序生成图片列表（使用原始文件名）
  const nums = [...cards.keys()].sort((a, b) => a - b);
  const lines = nums.flatMap((num) => {
    const { normal, trained } = cards.get(num);
    const result = [];
    if (normal) result.push(`![](/images/card_${num}_normal.webp)`);
    if (trained) result.push(`![](/images/card_${num}_trained.webp)`);
    return result;
  });

  const galleryBlock = `{% gallery %}\n${lines.join("\n")}\n{% endgallery %}`;

  // 3. 读取 index.md 并更新
  let content = await readFile(indexFile, "utf8");

  // 更新 updated 字段为脚本运行时间
  const now = formatTime(new Date());
  if (/^updated:.*$/m.test(content)) {
    content = content.replace(/^updated:.*$/m, `updated: ${now}`);
  } else {
    content = content.replace(/^date:.*$/m, (match) => `${match}\nupdated: ${now}`);
  }

  // 替换 {% gallery %} 块内容
  if (!/{% gallery %}[\s\S]*?{% endgallery %}/.test(content)) {
    throw new Error("index.md 中未找到 {% gallery %} 块");
  }
  content = content.replace(/{% gallery %}[\s\S]*?{% endgallery %}/, galleryBlock);

  await writeFile(indexFile, content, "utf8");
  console.log(`已更新 ${indexFile}`);
  console.log(`共 ${nums.length} 张卡，${lines.length} 张图片`);
}

main().catch((err) => {
  console.error(err);
  process.exit(1);
});
