---
title: 网站收藏夹
date: 2025-10-09 14:20:20
layout: page
---

<section class="bookmark-page">
	<div class="bookmark-page__inner">
		<header class="bookmark-page__header">
			<h1 class="bookmark-page__title">我的收藏</h1>
			<p class="bookmark-page__tip">数据源：<code>pintree.json</code>（支持折叠的层级目录，点击标题展开或收起）</p>
		</header>
		<div id="bookmark-root" class="bookmark-tree">
			<p class="bookmark-tree__status">正在加载收藏数据…</p>
		</div>
	</div>
</section>

<style>
	.bookmark-page {
		margin: 0;
		padding: 0;
	}

	.bookmark-page__inner {
		max-width: 960px;
		margin: 0 auto;
		padding: 1.5rem 1.25rem 3rem;
	}

	.bookmark-page__title {
		margin: 0 0 0.5rem;
		font-size: 2.25rem;
		line-height: 1.1;
		font-weight: 700;
	}

	.bookmark-page__tip {
		margin: 0;
		color: var(--text-color, #666);
		font-size: 0.95rem;
	}

	.bookmark-tree details {
		border-left: 3px solid rgba(120, 120, 150, 0.2);
		margin: 1rem 0 0;
		padding-left: 0.75rem;
	}

	.bookmark-tree details[open] {
		border-color: rgba(120, 120, 150, 0.35);
	}

	.bookmark-tree summary {
		list-style: none;
		cursor: pointer;
		position: relative;
		display: flex;
		align-items: center;
		padding: 0.35rem 0.25rem 0.35rem 1.4rem;
		font-weight: 600;
		color: var(--text-color, #333);
		border-radius: 10px;
		transition: color 0.2s ease, background 0.2s ease, box-shadow 0.2s ease;
	}

	.bookmark-tree summary:hover {
		background: rgba(91, 127, 255, 0.08);
		box-shadow: inset 0 0 0 1px rgba(91, 127, 255, 0.15);
		color: var(--primary-color, #5b7fff);
	}

	.bookmark-tree summary::marker {
		content: "";
	}

	.bookmark-tree summary::before {
		content: "";
		position: absolute;
		left: 0;
		top: 50%;
		width: 0.55rem;
		height: 0.55rem;
		border-radius: 2px;
		border: 2px solid currentColor;
		transform: translateY(-50%) rotate(45deg);
		transition: transform 0.2s ease;
	}

	.bookmark-tree details[open] > summary::before {
		transform: translateY(-50%) rotate(225deg);
	}

	.bookmark-folder__heading {
		margin: 0;
		line-height: 1.25;
		font-weight: 600;
	}

	.bookmark-folder.level-1 > summary .bookmark-folder__heading {
		font-size: 1.45rem;
	}

	.bookmark-folder.level-2 > summary .bookmark-folder__heading {
		font-size: 1.25rem;
	}

	.bookmark-folder.level-3 > summary .bookmark-folder__heading {
		font-size: 1.1rem;
	}

	.bookmark-folder.level-4 > summary .bookmark-folder__heading,
	.bookmark-folder.level-5 > summary .bookmark-folder__heading,
	.bookmark-folder.level-6 > summary .bookmark-folder__heading {
		font-size: 1rem;
	}

	.bookmark-folder__meta {
		margin-left: 1.4rem;
		color: var(--text-color-secondary, #888);
		font-size: 0.85rem;
	}

	.bookmark-children {
		margin: 0.75rem 0 0.5rem 0;
		display: flex;
		flex-direction: column;
		gap: 0.85rem;
	}

	.bookmark-link-grid {
		display: grid;
		gap: 0.85rem;
		grid-template-columns: repeat(3, minmax(0, 1fr));
	}

	@media (max-width: 1024px) {
		.bookmark-link-grid {
			grid-template-columns: repeat(2, minmax(0, 1fr));
		}
	}

	@media (max-width: 720px) {
		.bookmark-link-grid {
			grid-template-columns: 1fr;
		}
	}

	.bookmark-card {
		display: grid;
		grid-template-columns: 52px 1fr;
		align-items: center;
		gap: 0.75rem;
		padding: 0.85rem 1rem;
		border-radius: 14px;
		background: rgba(255, 255, 255, 0.75);
		border: 1px solid rgba(120, 120, 150, 0.16);
		box-shadow: 0 6px 18px rgba(15, 20, 30, 0.04);
		transition: transform 0.2s ease, box-shadow 0.2s ease;
		text-decoration: none;
	}

	.bookmark-card:link,
	.bookmark-card:visited,
	.bookmark-card:hover,
	.bookmark-card:focus,
	.bookmark-card:active {
		text-decoration: none !important;
	}

	.bookmark-card:hover {
		transform: translateY(-4px);
		box-shadow: 0 12px 28px rgba(15, 20, 30, 0.12);
	}

	.bookmark-card__icon {
		width: 52px;
		height: 52px;
		border-radius: 12px;
		background: rgba(120, 120, 150, 0.12);
		display: grid;
		place-items: center;
		overflow: hidden;
		font-size: 1.25rem;
		font-weight: 600;
		color: rgba(60, 60, 90, 0.85);
	}

	.bookmark-card__icon img {
		width: 100%;
		height: 100%;
		object-fit: cover;
	}

	.bookmark-card__body {
		display: flex;
		flex-direction: column;
		gap: 0.35rem;
		min-width: 0;
	}

	.bookmark-card__title {
		margin: 0;
		font-size: 1rem;
		font-weight: 600;
		color: var(--text-color, #222);
		white-space: nowrap;
		overflow: hidden;
		text-overflow: ellipsis;
	}

	.bookmark-card:hover .bookmark-card__title {
		color: var(--primary-color, #3a63f3);
	}

	.bookmark-card__link {
		display: block;
		font-size: 0.85rem;
		color: #a0a5af;
		text-decoration: none;
		white-space: nowrap;
		overflow: hidden;
		text-overflow: ellipsis;
		overflow-wrap: anywhere;
	}

	.bookmark-card:hover .bookmark-card__link {
		color: #a0a5af;
	}

	.bookmark-tree__status,
	.bookmark-tree__error {
		margin: 2rem 0;
		text-align: center;
		color: var(--text-color-secondary, #7a7a7a);
	}

	.bookmark-tree__error strong {
		display: block;
		margin-bottom: 0.5rem;
		color: #d9534f;
	}

	@media (max-width: 600px) {
		.bookmark-card {
			grid-template-columns: 44px 1fr;
			gap: 0.6rem;
			padding: 0.8rem;
		}

		.bookmark-card__icon {
			width: 44px;
			height: 44px;
			border-radius: 10px;
		}

		.bookmark-page__inner {
			padding: 1.2rem 1rem 2.5rem;
		}

		.bookmark-page__title {
			font-size: 1.8rem;
		}
	}
</style>

<script>
	window.addEventListener('DOMContentLoaded', function () {
		const container = document.getElementById('bookmark-root');
		const JSON_PATH = '_data/pintree.json?v=' + Date.now();

		fetch(JSON_PATH)
			.then(function (response) {
				if (!response.ok) {
					throw new Error('网络请求失败：' + response.status);
				}
				return response.json();
			})
			.then(function (data) {
				container.innerHTML = '';
				if (!Array.isArray(data) || data.length === 0) {
					container.innerHTML = '<p class="bookmark-tree__error"><strong>暂无数据</strong>请检查 pintree.json 文件。</p>';
					return;
				}
				data.forEach(function (node, index) {
					const rendered = renderNode(node, 1, index === 0);
					if (rendered) {
						container.appendChild(rendered);
					}
				});
			})
			.catch(function (err) {
				console.error(err);
				container.innerHTML = '<p class="bookmark-tree__error"><strong>加载失败</strong>' + err.message + '</p>';
			});

		function renderNode(node, level, expanded) {
			if (!node) return null;
			if (node.type === 'folder') {
				return renderFolder(node, level, expanded);
			}
			if (node.type === 'link') {
				return renderLink(node);
			}
			return null;
		}

		function renderFolder(folder, level, expanded) {
			const details = document.createElement('details');
			details.className = 'bookmark-folder level-' + level;
			if (expanded) {
				details.setAttribute('open', 'open');
			}

			const summary = document.createElement('summary');
			const headingLevel = Math.min(level, 6);
			const heading = document.createElement('h' + headingLevel);
			heading.className = 'bookmark-folder__heading';
			heading.textContent = folder.title || '未命名分组';
			summary.appendChild(heading);
			details.appendChild(summary);

			if (Array.isArray(folder.children) && folder.children.length > 0) {
				const meta = document.createElement('div');
				meta.className = 'bookmark-folder__meta';
				meta.textContent = '共 ' + folder.children.length + ' 项';
				details.appendChild(meta);

				const childrenWrap = document.createElement('div');
				childrenWrap.className = 'bookmark-children';
				let currentLinkGrid = null;
				folder.children.forEach(function (child) {
					const childEl = renderNode(child, level + 1, false);
					if (!childEl) return;
					if (childEl.tagName === 'A') {
						if (!currentLinkGrid) {
							currentLinkGrid = document.createElement('div');
							currentLinkGrid.className = 'bookmark-link-grid';
							childrenWrap.appendChild(currentLinkGrid);
						}
						currentLinkGrid.appendChild(childEl);
					} else {
						currentLinkGrid = null;
						childrenWrap.appendChild(childEl);
					}
				});
				details.appendChild(childrenWrap);
			} else {
				const emptyMeta = document.createElement('div');
				emptyMeta.className = 'bookmark-folder__meta';
				emptyMeta.textContent = '该分组暂无子项';
				details.appendChild(emptyMeta);
			}

			return details;
		}

		function renderLink(link) {
			const card = document.createElement('a');
			card.className = 'bookmark-card';
			card.href = link.url || '#';
			card.target = '_blank';
			card.rel = 'noopener noreferrer';

			const iconWrap = document.createElement('div');
			iconWrap.className = 'bookmark-card__icon';

			const titleText = link.title || (link.url || '未命名链接');
			if (link.icon) {
				const img = document.createElement('img');
				img.src = link.icon;
				img.alt = titleText;
				img.addEventListener('error', function () {
					iconWrap.innerHTML = '';
					iconWrap.textContent = getFallbackText(titleText);
				});
				iconWrap.appendChild(img);
			} else {
				iconWrap.textContent = getFallbackText(titleText);
			}

			const body = document.createElement('div');
			body.className = 'bookmark-card__body';

			const title = document.createElement('h3');
			title.className = 'bookmark-card__title';
			title.textContent = titleText;

			const linkEl = document.createElement('div');
			const anchor = document.createElement('span');
			anchor.className = 'bookmark-card__link';
			anchor.textContent = link.url || '';
			linkEl.appendChild(anchor);

			body.appendChild(title);
			body.appendChild(linkEl);

			card.appendChild(iconWrap);
			card.appendChild(body);

			return card;
		}

		function getFallbackText(text) {
			if (!text) return '∞';
			const matches = text.match(/\p{L}|\p{N}/u);
			return matches ? matches[0].toUpperCase() : '∞';
		}
	});
</script>
