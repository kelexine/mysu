import { defineConfig, SiteConfig } from 'vitepress'
import locales from './locales'
import { mkdir, readdir, writeFile } from 'fs/promises'
import { resolve } from 'path'

export default defineConfig( {
    title: 'MySU',
    base: '/mysu/',
    locales: locales.locales,
    head: [
        ['script', {
            async: 'async',
            src: 'https://pagead2.googlesyndication.com/pagead/js/adsbygoogle.js?client=ca-pub-8356785667482909',
            crossorigin: 'anonymous',
        }],
    ],
    sitemap: {
        hostname: 'https://kelexine.github.io/mysu'
    },
    buildEnd: async (config: SiteConfig) => {
        const templateDir = resolve(config.outDir, 'templates');
        const templateList = resolve(templateDir, "index.json");
        let files: string[] = [];
        try {
            await mkdir(templateDir, { recursive: true });
            files = await readdir(templateDir);
            files = files.filter(file => !file.startsWith('.'));
            await writeFile(templateList, JSON.stringify(files));
        } catch(e) {
            // ignore
        }
    }
})
